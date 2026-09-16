#ifndef SRM_BRIDGE_H
#define SRM_BRIDGE_H

#ifdef __cplusplus
extern "C" {
#endif

/*
 * ABI estable entre el WMS Java (JNI) y el C++ que mueve la SRM.
 * Los JSON son el mismo contrato que docs/protocolo.md (MQTT/ESP32).
 *
 * Códigos de retorno: 0 ok, <0 error de puente, >0 reservado al firmware.
 */

int srm_bridge_start(const char *config_json);
int srm_bridge_store(const char *cmd_id, const char *slot_id, const char *sku);
int srm_bridge_retrieve(const char *cmd_id, const char *slot_id);
int srm_bridge_abort(const char *cmd_id, const char *action);
void srm_bridge_stop(void);

#ifdef __cplusplus
}
#endif

#endif
