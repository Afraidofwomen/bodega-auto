# Eléctrica — mini AS/RS

Diagrama de bloques, pinout ESP32-S3, cableado y BOM. Estados y coordenadas en [plano.md](plano.md).

## Diagrama de bloques

```
                    +------------------+
                    |  E-stop NC       |
                    |  (lazo ENABLE)   |
                    +--------+---------+
                             |
                             v
  24 V PSU ----+----> TMC2209 X / Y / Z  (VMOT)
               |            ^
               +--> buck 5V | ENABLE conjunto (cortado por E-stop)
                      |     |
                      v     |
                 ESP32-S3   |  STEP / DIR / UART
                      |     v
                      +--> sensores (3V3): home X/Y/Z, fork, I/O
                      |
                      +--> WiFi MQTT (WCS)
```

Un solo **ESP32-S3-DevKit**. No hay segundo MCU. Los tres TMC2209 comparten UART (direcciones 0, 1, 2) para corriente, microstep y diagnóstico.

**Regla de alimentación:** los steppers se alimentan a 24 V desde la PSU. El ESP32 se alimenta a 5 V por el buck (pin 5V del DevKit). Nunca VMOT desde 3V3.

## Seguridad (maqueta, no industrial)

- El E-stop **corta ENABLE** de los tres drivers en hardware (serie con el pin EN, activo-bajo típico en TMC2209). El ESP32 también lee el E-stop para pasar a `fault`.
- Homes **NC** a 3V3 con pull-up; abierto = home o cable cortado.
- Soft-limits solo en firmware (documentados; no hay endstop de máximo).
- GND común entre PSU 24 V (0 V), buck y ESP32.
- TVS / flyback no hace falta en steppers con TMC; sí un fusible o PSU con límite en el 24 V.

Polaridad ENABLE: TMC2209 ENN es **enable-not**. E-stop abierto debe dejar ENN = HIGH (drivers off). El ESP32 pone ENN = LOW solo si E-stop está cerrado y no hay `fault`.

En `idle` los drivers **siguen en enable** (hold). El T8 de avance 8 mm no es autobloqueante: si se corta ENABLE, el eje Y puede bajar.

## Pinout ESP32-S3-DevKit

Referencia: DevKitC-1 estilo. Evitar GPIO 26–32 si se usa PSRAM en el módulo; la tabla usa pines GPIO seguros para input/output de propósito general. Ajustar si la placa concreta remapea el USB o el UART0.

| Función | GPIO | Dirección | Notas |
| --- | --- | --- | --- |
| STEP X | 4 | out | pulso al TMC X |
| DIR X | 5 | out | |
| STEP Y | 6 | out | |
| DIR Y | 7 | out | |
| STEP Z | 15 | out | |
| DIR Z | 16 | out | |
| ENN (drivers) | 17 | out | común a los 3 TMC, **después** del E-stop |
| TMC UART TX | 8 | out | UART1 → los 3 drivers (PDN_UART) |
| TMC UART RX | 9 | in | con resistencia 1 kΩ en serie al nodo TX (one-wire TMC) |
| HOME_X | 10 | in, pull-up | NC a GND |
| HOME_Y | 11 | in, pull-up | NC a GND |
| HOME_Z | 12 | in, pull-up | NC a GND |
| FORK_OCC | 13 | in | IR o microswitch; HIGH = bin en horquilla (definir en firmware y mantener) |
| IO_OCC | 14 | in | IR en estación I/O; HIGH = bin presente |
| ESTOP_SENSE | 21 | in, pull-up | lee el lazo E-stop; LOW = seta OK (cerrado) |
| LED_STATUS | 48 | out | LED RGB onboard en muchos S3; si no, GPIO 38 + LED |

Pines reservados y no usar para motion:

- 0, 3, 45, 46: strapping
- 19, 20: USB D−/D+ en S3 nativo
- 43, 44: UART0 USB-serial (logs)

Alimentación DevKit: 5 V en el pin `5V` (o USB solo para debug). GND común.

## Cableado

### Drivers TMC2209

Cada módulo (BigTreeTech / FYSETC típico):

| TMC | ESP32 / alimentación |
| --- | --- |
| VMOT / GND | 24 V PSU |
| VIO | 3V3 del DevKit |
| STEP / DIR | según tabla |
| ENN | nodo común: E-stop → ESP32 GPIO 17 |
| PDN_UART | bus UART (dir 0, 1, 2 con MS1/MS2) |
| DIAG | no cableado en v1 (opcional StallGuard después) |

Direcciones UART:

| Eje | MS1 | MS2 | Dirección |
| --- | --- | --- | --- |
| X | LOW | LOW | 0 |
| Y | HIGH | LOW | 1 |
| Z | LOW | HIGH | 2 |

Corriente de referencia (maqueta, NEMA 17 ~1.5 A): empezar en **0.8 A RMS** y subir si hay pérdida de pasos. Microstep: 8 u 16.

### E-stop (lazo ENABLE)

```
3V3 --[10k]--+---- GPIO 21 (ESTOP_SENSE)
             |
            E-stop NC
             |
            GND

24V logic / 3V3 ---- E-stop NC ---- ENN de los 3 TMC
                                      ^
                                      |
                               GPIO 17 también puede
                               bajar ENN solo si el lazo está cerrado
```

Implementación simple recomendada: E-stop en serie entre GPIO 17 y los ENN. Si la seta está abierta, ENN flota a HIGH con pull-up en los módulos → motores off, da igual lo que haga el firmware.

### Sensores

- Homes: switch mecánico NC, un lado GND, otro GPIO con pull-up interno.
- Horquilla e I/O: sensor IR de proximidad (3V3) o palanca. Montar el de I/O mirando el hueco del bin; el de horquilla bajo la cuna.
- Cables de sensores lejos de los looms de motor; torsión en el mástil con cadena portacables o espiral.

### Motores

NEMA 17 bipolar, 4 hilos, a 2A máx del driver. Cable apantallado si hay WiFi inestable (poco habitual en maqueta).

## BOM

Cantidades para **una** maqueta según este plano.

| Qty | Parte | Uso | Notas de compra |
| --- | --- | --- | --- |
| 1 | ESP32-S3-DevKitC-1 (N8R8 o N16R8) | MCU + WiFi | USB-C nativo; evitar ESP32 clásico por GPIO |
| 3 | TMC2209 stepper driver (módulo) | X, Y, Z | UART, 24 V |
| 3 | NEMA 17 1.5–2 A, 40–48 mm | ejes | eje 5 mm; acople a T8 8 mm |
| 1 | Fuente 24 V 5 A (o 4 A mínimo) | VMOT | DIN o Mean Well-alike |
| 1 | Buck 24 V → 5 V 2 A | alimentación ESP32 | no usar el LDO del DevKit para el resto |
| 3 | Endstop mecánico NC | home X/Y/Z | tipo impresora 3D |
| 2 | Sensor IR o microswitch | horquilla, I/O | 3V3 |
| 1 | Seta E-stop NC | lazo ENABLE | 22 mm panel |
| 1 | Perfil 2020 + rieles (kit) | rack y SRM | ~2–3 m de perfil |
| 3 | Tornillo infinito T8 Ø8 mm, avance 8 mm | X, Y, Z | X ≥ 550 mm, Y ≥ 350 mm, Z ≥ 250 mm |
| 3 | Tuerca T8 POM anti-backlash | carros | una por eje |
| 3 | Acople flexible 5 mm a 8 mm | motor → T8 | tipo jaw o helicoidal |
| 6 | Soporte KP08 (chumacera 8 mm) | apoyos de cada T8 | 2 por eje (lado motor + punta libre) |
| 1 | Riel MGN12 o V-slot | guía X y Y | el tornillo no lleva carga radial |
| 1 | Riel corto MGN9 (o 2 varillas 8 mm) | anti-rotación de la cuna Z | ~200 mm |
| 32 | Cajas 100×80×50 mm | bins | mismo modelo todas |
| 1 | Cadena portacables 10×10 | loom al mástil | opcional pero recomendable |
| — | Tornillería M3/M5, cables 22 AWG señal, 18 AWG VMOT | | |

### Opcional (no requisito)

| Qty | Parte | Por qué no está en v1 |
| --- | --- | --- |
| 1 | Lector RFID / cámara | identificación de bin; el plano usa `sku` en el comando MQTT |
| 3 | Endstop de máximo | sustituibles por soft-limits |
| 1 | PCB backplane | el cableado en protoboard / DIN alcanza para maqueta |

### Consumo estimado

| Carga | Pico |
| --- | --- |
| 3 × TMC @ 0.8 A, 24 V (no los 3 a pico a la vez) | ~2–3 A |
| ESP32 + sensores + buck | < 0.5 A @ 5 V |
| PSU 24 V 5 A | margen suficiente |

## Checklist de primer encendido (cuando exista firmware)

1. E-stop pulsado: ningún motor debe tener hold.
2. Solo 5 V al ESP32: UART TMC responde a `driver_version`.
3. 24 V con E-stop OK: jog de un eje a 10 mm, homes, luego misión vacía (sin bin).
