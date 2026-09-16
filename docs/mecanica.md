# Mecánica — mini AS/RS

Estructura, ejes, homing y croquis. Coordenadas lógicas y ciclos en [plano.md](plano.md).

## Estructura

Conjunto de escritorio / taller: dos racks de bin pequeño y un pasillo central por el que corre la SRM.

```
          lado A (Z+)                    lado B (Z-)
     +------------------+           +------------------+
     |  niveles 1..4    |  pasillo  |  niveles 1..4    |
     |  bahias 1..4     |  < SRM >  |  bahias 1..4     |
     +------------------+           +------------------+
     I/O aquí (X=0, Y=nivel 1)
```

Materiales de maqueta (no estructurales de almacén):

- Perfil de aluminio 20×20 (o 2020) para racks, rieles X y mástil Y.
- Estantes: lámina o PLA con cejas para que el bin no se caiga; holgura ~2 mm por lado.
- Base rígida (MDF / aluminio) para no perder paralelismo entre racks.

El pasillo debe ser lo bastante ancho para el mástil más el extractor retraído, y lo bastante estrecho para que el stroke de Z alcance el fondo del bin (~80–100 mm por cara).

### Bin y slot

| Pieza | Dimensión de referencia |
| --- | --- |
| Bin | 100 × 80 × 50 mm (caja de partes) |
| Hueco de slot | 104 × 84 × 55 mm (holgura) |
| Cejas / rieles del estante | retienen el bin 8–12 mm en Z hasta que la horquilla lo saca |
| Masa objetivo | < 300 g por bin (NEMA 17 holgado) |

La horquilla entra **por debajo** del bin (tipo mini-load: pestañas o cuna). No hay pinza: el bin se desliza.

## Ejes

Tres ejes cartesianos. Un motor por eje. Z no interpola con X/Y: se mueve solo con X e Y quietos y en posición.

```
        Y (mástil, arriba)
        ^
        |
        |     Z (extractor)
        |    --->  cara A
        |    <---  cara B
        |
        +--------------> X (pasillo, hacia bahía 4)
       home
       I/O
```

Los tres ejes usan el **mismo tipo de transmisión: tornillo infinito T8** (tornillo de potencia / sin fin), uno por eje. No hay correa GT2 ni cremallera. El tornillo solo empuja/tira; la carga radial va a un riel MGN12 (o V-slot) paralelo.

| Eje | Función | Transmisión | Motor | Stroke útil | Longitud T8 |
| --- | --- | --- | --- | --- | --- |
| X | traslación a lo largo del pasillo | T8 + tuerca POM + acople 5×8 + riel MGN12 | NEMA 17 | ~480 mm + margen home | ≥ 550 mm |
| Y | elevación de la horquilla | T8 + tuerca POM + acople 5×8 + riel MGN12 | NEMA 17 | ~280 mm + margen home | ≥ 350 mm |
| Z | extractor hacia A o B | T8 + tuerca POM + acople 5×8 + guía anti-rotación | NEMA 17 | ±80–100 mm desde centro | ≥ 250 mm |

SKU unificado: **T8 Ø8 mm, avance 8 mm/rev** (4 entradas). Misma tuerca y acople en X, Y y Z. El avance de 8 mm no es autobloqueante: el TMC debe dejar hold en Y (y en Z con bin) al idle. Si se quiere que Y no caiga sin corriente, sustituir solo ese tornillo por T8 de avance 2 mm.

Montaje de cada eje:

- Motor fijo → acople flexible 5 mm (eje NEMA) a 8 mm (T8).
- Apoyo del T8 en **ambos** extremos: KP08 junto al motor y KP08 en la punta libre (6 chumaceras en total).
- Tuerca anti-backlash en el carro; el carro corre en el riel, no “cuelga” del tornillo.
- En Z el tornillo cruza el pasillo: home (Z=0) es el centro, A y B son signos opuestos sobre el mismo husillo. La cuna lleva MGN9 (o dos varillas 8 mm) para que la tuerca no gire con el husillo.

Por qué stepper en Z (no servo 180°): el extractor necesita posición repetible a ambos lados del pasillo y hold al llevar el bin.

Velocidades de maqueta (más bajas que con correa; el T8-8 a ~400–600 rpm del NEMA 17):

| Eje | Velocidad | Aceleración |
| --- | --- | --- |
| X | 40–80 mm/s | 200 mm/s² |
| Y | 25–50 mm/s | 150 mm/s² |
| Z | 20–40 mm/s | 120 mm/s² |

Orden de movimiento en misión:

1. Z a 0 (retraído) si no lo está.
2. Interpolación lineal X+Y (o primero Y si hay riesgo de rozar estantes; en esta grilla, con Z=0 el pasillo está libre: X e Y juntos).
3. Z al stroke de la cara.
4. Z a 0.
5. X+Y al siguiente punto.

### Calibración a pasos

Constantes que el firmware futuro tomará de config (T8 avance 8 mm, 1.8°, 8 microsteps). **Igual en X, Y y Z** si los tres tornillos son el mismo SKU:

```
STEPS_PER_REV     = 200 * 8 = 1600
MM_PER_REV        = 8 mm
STEPS_PER_MM      = 1600 / 8 = 200
STEPS_PER_BAY_X   = 120 mm * 200 = 24000
STEPS_PER_LEVEL_Y = 70 mm * 200 = 14000
```

Si Y usa T8 de avance 2 mm: `STEPS_PER_MM_Y = 800`. El plano no sustituye a una medición en banco: jog 10 mm y corregir con regla.

## Homing

Cada eje tiene **un endstop NC en el origen** (X min, Y min, Z centro/retraído). No hay endstops de máximo: el máximo es soft-limit.

Secuencia (siempre en este orden):

1. **Z home.** Retráe el extractor hasta el switch de Z=0. Si Z no está en 0, X/Y no se mueven.
2. **Y home.** Baja hasta Y min (nivel I/O). Dirección −Y, lenta.
3. **X home.** Hacia I/O (−X) hasta X min.

Perfil de homing: acercamiento rápido hasta el flanco, retract 2–3 mm, segundo acercamiento lento para repetibilidad. Los switches son NC: cable roto se lee como “presionado” y aborta a `fault`.

| Eje | Switch | Dirección de home | Soft-limit max |
| --- | --- | --- | --- |
| X | NC en tope I/O | −X | bahía 4 + margen (~500 mm) |
| Y | NC en base del mástil | −Y | nivel 4 + margen (~300 mm) |
| Z | NC en posición retraída | hacia 0 desde ambos signos | ±Z_STROKE |

Tras homing válido, el flag `homed = true` se pierde con E-stop, reset, pérdida de ENABLE o reboot.

### Colisiones a evitar

- Nunca mover X o Y con Z ≠ 0 (horquilla dentro de un slot).
- Nunca ir a un slot sin retractar después de I/O.
- Dejar ~5 mm de clearance vertical al entrar al slot (Y un poco más bajo al insertar, subir para “colgar” el bin en las cejas, o al revés según el diseño de cuna). El plano asume **cuna deslizante a la misma Y**: las cejas son laterales, no hace falta micro-ciclo Y. Si el CAD posterior usa pestañas inferiores, documentar un subciclo `Y_pick_offset` (±3–5 mm).

## Croquis — racks y grúa

Planta, cotas aproximadas en mm:

```
     0                         ~600
     |---------------------------|
         I/O
        [====]
     A  | slot | slot | slot | slot |     profundidad rack ~80
        |------|------|------|------|
 pasillo|        SRM  X-->          |     ancho pasillo ~120
        |------|------|------|------|
     B  | slot | slot | slot | slot |
        |---------------------------|
```

Alzado del mástil (SRM):

```
        +--------+  Y max (nivel 4)
        | riel Y |
        | T8 Y   |
        |  [Z]   |  carro: motor Z + T8 Z + horquilla
        |        |
        |        |
        +--------+  Y=0 home + motor Y
    ==== riel X + T8 X =================
       motor X
```

Horquilla (vista desde I/O, Z=0):

```
   rack A              pasillo                 rack B
   ========   <----Z+  [cuna]  Z---->          ========
                    motor Z -- T8 -- tuerca
```

La cuna es una U invertida o dos púas de ~90 mm de largo, más estrecha que el hueco del slot, más ancha que el fondo del bin.

## Tolerancias de maqueta

| Concepto | Objetivo |
| --- | --- |
| Repetibilidad X/Y | ±1 mm (suficiente para holgura de 2 mm) |
| Paralelismo entre racks | < 2 mm en todo el pasillo |
| Verticalidad del mástil | < 1 mm de deriva en Y=max |
| Backlash Z | tuerca anti-backlash; si queda juego al cambiar de cara, compensar en firmware |

El T8 en Y mantiene el carro si hay hold current. Sin hold y con avance 8 mm, el mástil puede bajar solo: no cortar ENABLE en idle salvo E-stop.
