# Justificación del proyecto — Mini AS/RS

Documento de contexto, problema, objetivos y valor del sistema. El plano técnico está en [plano.md](plano.md); mecánica, eléctrica y protocolo WCS en los documentos enlazados desde el [README](../README.md).

## 1. Contexto

El almacenamiento y la recuperación de material son operaciones repetitivas, sensibles a error humano y difíciles de enseñar sin planta industrial. Un **AS/RS** (Automated Storage and Retrieval System) resuelve esa operación con una máquina que lleva un bin a una ubicación lógica y lo recupera bajo demanda. Encima de la máquina suele haber un **WCS** (Warehouse Control System) que traduce órdenes de negocio en movimiento, y un **WMS** (Warehouse Management System) que muestra inventario, estado y excepciones al operador.

En planta real esos sistemas son caros, ocupan naves enteras y no se pueden desmontar en un laboratorio. El proyecto construye una **maqueta de escritorio** de un AS/RS cartesiano de un pasillo: dos racks enfrentados, 32 ubicaciones, una estación I/O y una grúa XYZ. El objetivo no es competir con un almacén comercial; es tener un sistema completo —mecánica, electrónica, contrato de control e interfaz— que se pueda construir, instrumentar y explicar.

## 2. Problema

Sin una maqueta, el ciclo store/retrieve se queda en diagrama. No hay forma de:

- Verificar que una ubicación lógica (`A23`) se traduce en un movimiento X/Y/Z repetible.
- Comprobar precondiciones reales: I/O ocupado o vacío, horquilla con o sin bin, homing válido, E-stop abierto.
- Enseñar la separación entre **inventario lógico** (mapa de 32 slots) y **telemetría** (posición en mm, sensores).
- Probar una interfaz de operador con reglas industriales (confirmación doble, indicador online/offline, semántica de estados) contra un dispositivo que puede fallar.

Un robot de tres ejes genérico no basta: hace falta un **contrato de almacén** (store, retrieve, abort, status, inventario) y una UI que hable ese contrato, no pulsos STEP/DIR sueltos.

## 3. Justificación

El proyecto se justifica en tres planos que se refuerzan.

### 3.1 Didáctico

Permite recorrer de punta a punta la pila de un almacén automático en un volumen de ~600 × 400 × 350 mm:

1. Ubicación lógica → coordenadas de bahía/nivel/cara.
2. Coordenadas → pasos de tornillo T8 (calibración, no magia).
3. Pasos → drivers TMC2209 y sensores de home / horquilla / I/O.
4. Firmware (C++ en el ESP32, pendiente) → tópicos MQTT.
5. WMS Java → operador y administrador.

Esa cadena es el objeto de estudio. Un tutorial de MQTT o un eje único no la cubre.

### 3.2 Técnico

La topología elegida es la más simple que sigue siendo un AS/RS de verdad:

| Decisión | Por qué |
| --- | --- |
| Un pasillo, dos caras, 4×4 | 32 slots caben en mesa; el mapa de calor y el inventario son legibles |
| SRM cartesiana XYZ | Un solo mecanismo; no shuttle ni carrusel |
| Tres tornillos T8 (mismo SKU) | Transmisión unificada, hold en Y, stroke de Z a ambos lados |
| Un ESP32-S3 | MCU + WiFi; motion y WCS en el mismo dispositivo |
| MQTT/JSON | Contrato estable entre Java, simulador y C++ futuro |
| E-stop en hardware sobre ENABLE | La seta corta motores aunque el firmware falle |

El WMS en Java no es un adorno: es el cliente del contrato. Arranca en simulador (sin hardware), habla MQTT cuando exista firmware, y deja un ABI JNI para el C++ de movimiento. Así la interfaz y el control se pueden desarrollar en paralelo.

### 3.3 Operativo (maqueta)

Aunque la escala es de taller, las reglas son las de un WCS pequeño:

- Una misión, un bin, un origen y un destino.
- Store y retrieve siempre pasan por I/O; no hay buffer intermedio.
- Tras un fallo el inventario no se “adivina”: hace falta `reset` + homing.
- El operador ve ocupación, estado de la SRM, mapa de slots y confirmación de picking en menos de dos segundos.

Eso convierte la maqueta en banco de prueba de procedimientos, no solo de motores.

## 4. Objetivos

### General

Diseñar e integrar un mini AS/RS de un pasillo, con WCS por MQTT y WMS de escritorio, de modo que un operador pueda almacenar y recuperar bins por `slot_id` con visibilidad de estado, inventario y fallos.

### Específicos

1. Fijar layout, coordenadas lógicas (`A11`…`B44` + `IO`) y ciclos store/retrieve.
2. Definir mecánica de tres ejes T8, homing NC y límites blandos.
3. Definir eléctrica (ESP32-S3, TMC2209, E-stop, sensores) y BOM de maqueta.
4. Publicar el contrato WCS (`store`, `retrieve`, `abort`, `status`, `event`, `inventory`).
5. Implementar el WMS Java (dashboard, mapa, operario, enlace MQTT / nativo / simulador).
6. Dejar el puente C++ (`srm_bridge`) para el firmware o el motion planner que mueva los ejes.

## 5. Alcance y no alcance

**Dentro:** plano, mecánica, eléctrica, protocolo, interfaz WMS y ABI nativo. Seguridad de maqueta (E-stop, homes NC).

**Fuera (v1):** firmware de motion en el ESP32, PCB, CAD 3D, RFID, endstops de máximo, recuento automático de inventario, throughput industrial y certificación de máquina.

El no alcance es deliberado: primero el contrato y la UI contra un simulador; después el C++ sobre hardware ya acotado.

## 6. Beneficios esperados

- Un demostrador que se puede mostrar en mesa: I/O → slot → I/O, con mapa e indicadores.
- Material de diseño reutilizable (pinout, tópicos, estados) para el firmware C++.
- Separación clara WMS (Java) / WCS-motion (ESP32 o JNI), útil como arquitectura de referencia.
- Reducción de errores de operador en la maqueta: slot visible, SKU en monoespaciado, retrieve con doble clic, flash verde/rojo.

## 7. Criterio de éxito

El proyecto cumple su justificación cuando:

1. Un slot se identifica de forma unívoca y coincide en plano, protocolo y UI.
2. Store y retrieve se rechazan si I/O, horquilla, home o estado no lo permiten.
3. El WMS opera en simulador y queda listo para MQTT (`asrs/{device_id}/…`) sin cambiar de contrato.
4. Un abort/E-stop deja la máquina en un estado explícito (`fault` o `idle`), no en inventario ambiguo sin avisar.

Mientras el firmware no exista, el simulador del WMS es el banco de esas reglas.
