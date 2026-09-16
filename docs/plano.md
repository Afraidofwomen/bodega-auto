# Plano maestro — mini AS/RS

Visión, layout, coordenadas, ciclos de operación y máquina de estados de la SRM. Detalle mecánico en [mecanica.md](mecanica.md), eléctrico en [electrica.md](electrica.md), contrato WCS en [protocolo.md](protocolo.md).

## Visión

Sistema de almacenamiento y recuperación automático de escritorio. Dos estanterías enfrentadas, un pasillo y una máquina SRM cartesiana de tres ejes. Cada eje (X, Y, Z) se mueve con un **tornillo infinito T8** (husillo trapezoidal), un NEMA 17 y un TMC2209. El ESP32-S3 es a la vez controlador de movimiento y WCS: recibe órdenes `store` / `retrieve` por MQTT y publica estado.

Capacidad de referencia:

- 32 ubicaciones de almacenamiento
- 1 estación I/O (único punto de intercambio con el operador)
- 1 bin en tránsito sobre la horquilla (nunca más de uno)

No hay buffer intermedio. Store y retrieve siempre pasan por I/O.

## Layout

Vista en planta (operador de pie frente a I/O, mirando el pasillo):

```
        Y+ (arriba, fuera del papel)
        ^
        |
   X=0  |              X+ (pasillo → fondo)
        +---------------------------------->
        |
  I/O   |  bahia 1    bahia 2    bahia 3    bahia 4
 +----+ | +--------+ +--------+ +--------+ +--------+
 | IO | | |  A1    | |  A2    | |  A3    | |  A4    |   Rack A (izquierda, Z+)
 +----+ | +--------+ +--------+ +--------+ +--------+
        | =============================================  pasillo + SRM
        | +--------+ +--------+ +--------+ +--------+
        | |  B1    | |  B2    | |  B3    | |  B4    |   Rack B (derecha, Z-)
        | +--------+ +--------+ +--------+ +--------+
```

Vista de un rack (cara A o B), 4 bahías × 4 niveles:

```
nivel 4   A14  A24  A34  A44
nivel 3   A13  A23  A33  A43
nivel 2   A12  A22  A32  A42
nivel 1   A11  A21  A31  A41     ← I/O a la altura de nivel 1
          bay1 bay2 bay3 bay4
```

La estación I/O está en `X = 0`, alineada en Y con el **nivel 1**. No ocupa un slot de almacenamiento.

Dimensiones de referencia (ajustables; el firmware usará pasos, no milímetros en runtime):

| Magnitud | Valor |
| --- | --- |
| Bin (L × An × Al) | ~100 × 80 × 50 mm |
| Paso X (bahía a bahía) | 120 mm |
| Paso Y (nivel a nivel) | 70 mm |
| Recorrido X útil | 4 × 120 = 480 mm |
| Recorrido Y útil | 4 × 70 = 280 mm |
| Recorrido Z (por cara) | ~80–100 mm desde centro de pasillo |
| Volumen estimado del conjunto | ~600 × 400 × 350 mm |

## Coordenadas

Hay dos sistemas. El WCS habla en **ubicaciones lógicas**. El motion planner habla en **pasos de motor**.

### Ubicación lógica

Formato: `cara / bahia / nivel`

| Campo | Valores | Notas |
| --- | --- | --- |
| `cara` | `A` \| `B` | A = izquierda (Z+), B = derecha (Z−) |
| `bahia` | `1`–`4` | 1 es la más cercana a I/O |
| `nivel` | `1`–`4` | 1 es el más bajo |

`slot_id` canónico (string): `{cara}{bahia}{nivel}` — ejemplos: `A11`, `B34`.

Estación I/O: `slot_id = "IO"`. No es un slot de inventario.

### Origen mecánico

Tras homing, `(X, Y, Z) = (0, 0, 0)`:

- **X = 0:** SRM frente a I/O (antes de bahía 1).
- **Y = 0:** horquilla a altura de I/O / nivel 1.
- **Z = 0:** extractor retraído, centrado en el pasillo (no entra a ningún rack).

Traducción a pasos (T8 avance 8 mm en X/Y/Z; `STEPS_PER_MM = 200` con 8 microsteps; constantes de calibración, no runtime):

```
X_steps = (bahia - 1) * STEPS_PER_BAY_X      // 120 mm × 200 = 24000
Y_steps = (nivel - 1) * STEPS_PER_LEVEL_Y    // 70 mm × 200 = 14000
Z_steps = cara == A ? +Z_STROKE : -Z_STROKE  // ~90 mm × 200 = ±18000
```

I/O: `(0, 0, ±Z_STROKE)` según si se deposita o se toma; Z=0 en tránsito.

### Inventario

Mapa estático de 32 slots. Modelo de datos (persistirá en NVS cuando exista firmware):

| Campo | Tipo | Descripción |
| --- | --- | --- |
| `slot_id` | string | `A11` … `B44` |
| `occupied` | bool | hay bin en el slot |
| `sku` | string \| null | identificador de contenido; null si vacío o bin sin etiquetar |
| `bin_id` | string \| null | opcional; no hay RFID en esta maqueta |

Invariantes:

- A lo sumo un bin en la horquilla.
- `occupied` de un slot y presencia en horquilla no pueden ambos representar el mismo bin.
- Store a un slot con `occupied = true` es error `slot_occupied`.
- Retrieve de un slot con `occupied = false` es error `slot_empty`.

## Ciclos de operación

Toda misión es atómica: un comando, un bin, un origen, un destino. Si falla a mitad, la SRM pasa a `fault` y no asume el inventario hasta un `reset` + homing.

### Retrieve (slot → I/O)

Precondiciones: SRM en `idle`, homing válido, horquilla vacía, slot ocupado, I/O libre.

1. **Home-check.** Si el último homing no es válido (power-on, fault previo), homing completo antes de mover.
2. **Ir al slot.** X y Y a la bahía/nivel; Z permanece en 0.
3. **Extraer.** Z hacia la cara, enganche/deslizamiento del bin, Z de vuelta a 0. Sensor de horquilla debe pasar a ocupado.
4. **Ir a I/O.** X=0, Y=0, Z=0.
5. **Depositar.** Z hacia el lado de I/O (mismo lado físico que se defina en mecánica; por defecto cara A / Z+), soltar bin, Z a 0. Sensor I/O ocupado, horquilla vacía.
6. **Idle.** Inventario: slot `occupied = false`; el operador retira el bin de I/O.

### Store (I/O → slot)

Precondiciones: SRM en `idle`, homing válido, horquilla vacía, I/O ocupado, slot libre.

1. **Home-check.**
2. **Tomar de I/O.** Ir a I/O si no está, extraer bin a la horquilla.
3. **Ir al slot.** X/Y destino, Z=0.
4. **Depositar.** Z hacia la cara, soltar, Z a 0. Sensor de horquilla vacío.
5. **Idle.** Inventario: slot `occupied = true` con el `sku` del comando.

### Abort

Un `abort` en movimiento pide parada controlada (desacelerar, Z a 0 si es seguro, quedarse en `fault` o `idle` según [protocolo.md](protocolo.md)). No deja un bin a medio meter en un slot: si Z no está retraído, retraer primero.

## Estados de la SRM

```
                    powerOn / fault
                          |
                          v
                       homing
                          |
                     homesOk
                          v
        +-------------- idle <------------------+
        |                |                      |
        |         store / retrieve              |
        |                v                      |
        |             moving                    |
        |            /      \                   |
        |      atSlot        atIO               |
        |         v            v                |
        |      picking      placing             |
        |         |            |                |
        |    binOnFork       done               |
        |         v            |                |
        |      moving ---------+                |
        |                                       |
        |   estop / timeout / noBin / collision |
        |                v                      |
        +------------> fault ---reset--> homing +
```

| Estado | Significado | Motores |
| --- | --- | --- |
| `homing` | busca origen X, Y, Z | activos, rutina de home |
| `idle` | listo; inventario coherente | ENABLE según política (ahorro o hold) |
| `moving` | interpolando X/Y (Z=0) | activos |
| `picking` | Z extendiéndose/retrayéndose para tomar | Z activo |
| `placing` | Z extendiéndose/retrayéndose para dejar | Z activo |
| `fault` | E-stop, timeout, sensor inesperado | ENABLE cortado o hold; no acepta store/retrieve |

Transiciones de error:

- E-stop hardware → `fault` inmediato (los drivers ya están en disable por el lazo).
- Timeout de movimiento o homing → `fault`.
- Retrieve y horquilla sigue vacía tras extraer → `fault` (`no_bin`).
- Store y horquilla sigue ocupada tras depositar → `fault` (`place_failed`).
- I/O ocupado al depositar un retrieve → `fault` (`io_blocked`).

Tras `fault` hace falta `reset` + homing. El inventario en NVS no se “adivina”: slots dudosos se marcan para recuento manual (campo lógico `unknown` en una implementación futura; el plano no lo exige en v1).
