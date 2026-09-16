# Protocolo WCS — MQTT / JSON

Contrato entre un cliente (dashboard, script, WMS) y el ESP32. El firmware aún no existe; cualquier implementación debe respetar estos tópicos y payloads.

Broker: Mosquitto u otro MQTT 3.1.1 en la LAN. El ESP32 es **cliente**. QoS 1 en comandos y en `status` de fin de misión. Retain en `status` (último estado). No retain en comandos.

Prefijo de tópicos: `asrs/{device_id}/` con `device_id` fijo de fábrica, p. ej. `srm1`.

## Tópicos

| Tópico | Dirección | Retain | Payload |
| --- | --- | --- | --- |
| `asrs/{id}/cmd/store` | cliente → ESP32 | no | comando store |
| `asrs/{id}/cmd/retrieve` | cliente → ESP32 | no | comando retrieve |
| `asrs/{id}/cmd/abort` | cliente → ESP32 | no | abort / reset |
| `asrs/{id}/status` | ESP32 → clientes | sí | estado actual |
| `asrs/{id}/event` | ESP32 → clientes | no | evento puntual (fin de misión, error) |
| `asrs/{id}/inventory` | ESP32 → clientes | sí | mapa de 32 slots (publicar tras cambio) |

El ESP32 no se suscribe a `status` ni `event`. Un cliente de UI se suscribe a `status`, `event` e `inventory`.

## Identificadores

- `cmd_id`: UUID o string único por comando (el cliente lo genera). Se ecoa en `status` y `event`.
- `slot_id`: `A11`…`B44` según [plano.md](plano.md). `"IO"` no es destino de store/retrieve de inventario.
- `sku`: string; puede ser `""` si el bin no está etiquetado.

## Comandos

Todos los JSON usan UTF-8, claves en `snake_case`. Campos desconocidos se ignoran. Campos extraídos abajo son los únicos requeridos u opcionales de v1.

### `store`

Publicar en `cmd/store`. Toma el bin que está en I/O y lo deja en `slot_id`.

```json
{
  "cmd_id": "c1",
  "slot_id": "A23",
  "sku": "TORN-M3"
}
```

| Campo | Requerido | Regla |
| --- | --- | --- |
| `cmd_id` | sí | único |
| `slot_id` | sí | slot libre `A\|B` + bahía + nivel |
| `sku` | no | default `""`; se guarda en el inventario del slot |

Rechazo inmediato (event `rejected`, SRM sigue `idle`): `slot_occupied`, `invalid_slot`, `not_idle`, `not_homed`, `io_empty`, `fork_occupied`.

### `retrieve`

Publicar en `cmd/retrieve`. Saca el bin de `slot_id` y lo deja en I/O.

```json
{
  "cmd_id": "c2",
  "slot_id": "B41"
}
```

| Campo | Requerido | Regla |
| --- | --- | --- |
| `cmd_id` | sí | único |
| `slot_id` | sí | slot ocupado |

Rechazo inmediato: `slot_empty`, `invalid_slot`, `not_idle`, `not_homed`, `io_occupied`, `fork_occupied`.

### `abort`

Publicar en `cmd/abort`.

```json
{
  "cmd_id": "c3",
  "action": "abort"
}
```

| `action` | Efecto |
| --- | --- |
| `abort` | parada controlada de la misión en curso; Z a 0 si es seguro; pasa a `fault` si el inventario quedó ambiguo, si no a `idle` |
| `reset` | solo válido en `fault`: limpia el fallo y arranca homing |
| `home` | solo válido en `idle`: fuerza homing de nuevo |

```json
{
  "cmd_id": "c4",
  "action": "reset"
}
```

Si no hay misión y `action=abort`, respuesta `rejected` / `not_busy`.

## `status` (retain)

El ESP32 publica al cambiar de estado y al menos cada 2 s durante `moving` / `picking` / `placing` / `homing`.

```json
{
  "device_id": "srm1",
  "state": "idle",
  "homed": true,
  "cmd_id": null,
  "mission": null,
  "position": { "x_mm": 0, "y_mm": 0, "z_mm": 0 },
  "fork_occupied": false,
  "io_occupied": false,
  "estop": false,
  "error": null
}
```

| Campo | Valores |
| --- | --- |
| `state` | `homing` \| `idle` \| `moving` \| `picking` \| `placing` \| `fault` |
| `homed` | bool |
| `cmd_id` | comando en curso o `null` |
| `mission` | `store` \| `retrieve` \| `home` \| `null` |
| `position` | estimación en mm desde home (telemetría; no es setpoint) |
| `fork_occupied` | sensor horquilla |
| `io_occupied` | sensor I/O |
| `estop` | `true` si el lazo está abierto |
| `error` | código o `null` |

Durante una misión, `cmd_id` y `mission` están rellenos. En `idle`, ambos `null`.

## `event` (no retain)

Un mensaje por rechazo, fin OK o fallo.

Éxito:

```json
{
  "cmd_id": "c1",
  "type": "completed",
  "mission": "store",
  "slot_id": "A23"
}
```

Rechazo (ni siquiera arrancó):

```json
{
  "cmd_id": "c1",
  "type": "rejected",
  "error": "slot_occupied"
}
```

Fallo a mitad:

```json
{
  "cmd_id": "c2",
  "type": "failed",
  "error": "no_bin",
  "state": "fault"
}
```

| `type` | Cuándo |
| --- | --- |
| `accepted` | comando válido, misión arranca (opcional; `status` ya lo muestra) |
| `completed` | misión OK, de vuelta a `idle` |
| `rejected` | no se aceptó |
| `failed` | se abortó o sensor inesperado |

### Códigos `error`

| Código | Significado |
| --- | --- |
| `invalid_slot` | `slot_id` mal formado o fuera de 1–4 |
| `not_idle` | llegó store/retrieve fuera de `idle` |
| `not_homed` | no hay homing válido |
| `not_busy` | abort sin misión |
| `slot_occupied` | store sobre slot lleno |
| `slot_empty` | retrieve sobre slot vacío |
| `io_empty` | store y I/O sin bin |
| `io_occupied` | retrieve y I/O ocupado |
| `io_blocked` | al depositar, I/O ocupado de forma inesperada |
| `fork_occupied` | horquilla ocupada al inicio |
| `no_bin` | retrieve: horquilla vacía tras extraer |
| `place_failed` | store: horquilla sigue ocupada tras depositar |
| `timeout` | movimiento o homing excedió el plazo |
| `estop` | seta pulsada |
| `collision` | reservado (soft-limit o DIAG futuro) |
| `bad_json` | payload no parseable |
| `unknown_action` | `abort.action` inválida |

## `inventory` (retain)

Publicar el mapa completo tras cada `completed` de store/retrieve y tras boot (lo que haya en NVS).

```json
{
  "slots": [
    { "slot_id": "A11", "occupied": false, "sku": null },
    { "slot_id": "A23", "occupied": true, "sku": "TORN-M3" }
  ]
}
```

Los 32 `slot_id` deben aparecer siempre. `sku` es `null` si `occupied` es false.

No hay comando MQTT de recuento en v1: un desajuste sensor/mapa se resuelve a mano y con un store/retrieve coherente, o en firmware futuro con `cmd/set_slot` fuera de este contrato.

## Semántica de concurrencia

- Un solo comando de misión a la vez. El segundo store/retrieve se `rejected` / `not_idle`.
- `abort` se puede enviar en cualquier momento; `reset` solo en `fault`.
- El cliente debe esperar `completed` o `failed` del mismo `cmd_id` (o `status.state == idle|fault`) antes de mandar el siguiente.
- Orden en broker no está garantizado entre tópicos distintos; el ESP32 serializa por llegada en sus colas internas.

## Timeouts sugeridos (cliente)

| Fase | Timeout sugerido |
| --- | --- |
| homing | 30 s |
| store / retrieve completo | 60 s |
| abort hasta `fault` o `idle` | 10 s |

Si el cliente vence el timeout, no reenviar el mismo `cmd_id` como misión nueva: enviar `abort` y esperar `status`.

## Ejemplo de sesión retrieve

1. Cliente lee `status` retain: `state=idle`, `homed=true`, `io_occupied=false`.
2. Publica `cmd/retrieve` `{ "cmd_id": "c2", "slot_id": "B41" }`.
3. `status` pasa a `moving` / `picking` / `moving` / `placing`.
4. `event` `{ "type": "completed", "cmd_id": "c2", "mission": "retrieve", "slot_id": "B41" }`.
5. `inventory` actualizado: `B41.occupied = false`.
6. `status` `idle`, `io_occupied = true`.
