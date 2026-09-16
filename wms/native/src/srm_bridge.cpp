#include "srm_bridge.h"

#include <cstdio>

/*
 * Stub del controlador C++. Sustituye el cuerpo por:
 *  - cliente MQTT hacia el ESP32, o
 *  - motion planner local (STEP/DIR, TMC UART) si el C++ corre en el host.
 *
 * El firmware del ESP32-S3 debe publicar status / event / inventory
 * con el JSON de docs/protocolo.md. JNI (jni_srm_bridge.cpp) reenvía
 * esos JSON a Java llamando SrmNative.onStatus/onEvent/onInventory.
 */

int srm_bridge_start(const char *config_json) {
    std::fprintf(stderr, "srm_bridge_start %s\n", config_json ? config_json : "");
    return 0;
}

int srm_bridge_store(const char *cmd_id, const char *slot_id, const char *sku) {
    std::fprintf(stderr, "store %s %s %s\n", cmd_id, slot_id, sku ? sku : "");
    return 0;
}

int srm_bridge_retrieve(const char *cmd_id, const char *slot_id) {
    std::fprintf(stderr, "retrieve %s %s\n", cmd_id, slot_id);
    return 0;
}

int srm_bridge_abort(const char *cmd_id, const char *action) {
    std::fprintf(stderr, "abort %s %s\n", cmd_id, action);
    return 0;
}

void srm_bridge_stop(void) {
    std::fprintf(stderr, "srm_bridge_stop\n");
}
