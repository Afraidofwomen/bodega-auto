# Bodega automática

Mini AS/RS (Automated Storage and Retrieval System) de maqueta, controlado por un **ESP32-S3**. Un pasillo, dos racks enfrentados, 32 ubicaciones y una estación I/O. Un solo MCU mueve la grúa cartesiana XYZ y expone el WCS por MQTT.

Esta entrega es el **plano técnico**. No hay firmware, PCB ni CAD todavía.

Vista de conjunto y kit de piezas: [docs/img/mini-asrs-conjunto-y-componentes.png](docs/img/mini-asrs-conjunto-y-componentes.png)

## Alcance

- Topología cartesiana de un pasillo (no shuttle ni carrusel).
- 4 bahías × 4 niveles × 2 caras = 32 bins + 1 estación I/O.
- 3 ejes STEP/DIR (NEMA 17 + TMC2209 UART), cada uno con tornillo infinito T8.
- Seguridad de maqueta: E-stop que corta ENABLE, homes NC, soft-limits documentados.

## Documentación

| Documento | Contenido |
| --- | --- |
| [docs/justificacion.md](docs/justificacion.md) | Contexto, problema, objetivos y valor del mini AS/RS |
| [docs/plano.md](docs/plano.md) | Visión, layout, coordenadas, ciclo store/retrieve, estados de la SRM |
| [docs/mecanica.md](docs/mecanica.md) | Estructura, ejes, homing, croquis de rack y grúa |
| [docs/electrica.md](docs/electrica.md) | Diagrama de bloques, pinout ESP32-S3, cableado, BOM |
| [docs/protocolo.md](docs/protocolo.md) | Contrato MQTT/JSON: `store`, `retrieve`, `status`, `abort` |

## Interfaz WMS (Java)

App JavaFX en [`wms/`](wms/): dashboard, mapa de 32 slots, módulo operario y enlace al SRM.

```bash
chmod +x wms/run.sh
./wms/run.sh
```

Tres pasarelas, mismo contrato JSON:

| Modo | Uso |
| --- | --- |
| Simulador | Opera sin hardware (arranque por defecto) |
| MQTT → ESP32 | Broker 3.1.1, tópicos `asrs/{device_id}/…` |
| Nativo C++ (JNI) | `wms/native` (`srm_bridge.h`) cuando el C++ controle el movimiento |

## Fuera de alcance (por ahora)

Firmware del ESP32, KiCad y CAD 3D. El contrato de [docs/protocolo.md](docs/protocolo.md) queda listo para implementarlos.
