# Pedilo App - Admin Operaciones cerrado

Fecha: 2026-06-13

Resultado: APTA

## Que estaba mal

- La configuracion Admin podia quedar en error humano visible si el documento remoto no existia o el listener fallaba.
- El backend escribia `updatedBy`, pero Android esperaba `lastUpdatedBy`, dejando trazabilidad incompleta en UI.
- Pedidos legacy vivos sin `nextAllowedActions` quedaban sin acciones operativas aunque la funcion soportara intervencion Admin.
- La funcion critica `operateLiveOrder` no estaba desplegada en Firebase `pediloapp-e2758`, por lo que la UI podia mostrar el flujo pero no ejecutar una accion real.
- El cierre anterior dependia de pantallas y pruebas tecnicas, sin mutacion real verificada de un pedido vivo.

## Que se hizo

- Se agrego `getAdminConfig` callable con inicializacion segura, auditoria y retorno consistente para Admin activo.
- Se corrigio `adminUpdateConfig` para persistir tambien `lastUpdatedBy`.
- Android Admin ahora lee configuracion por listener y, ante error/documento faltante, cae al callable seguro.
- Android Admin mantiene compatibilidad de trazabilidad leyendo `lastUpdatedBy` o `updatedBy`.
- Android Admin expone acciones operativas seguras en pedidos legacy vivos cuando falta `nextAllowedActions`, sin saltarse la validacion backend.
- Se desplegaron reglas Firestore y funciones `getAdminConfig`, `adminUpdateConfig` y `operateLiveOrder` en `southamerica-east1`.
- Se instalo y valido el APK debug en el dispositivo real conectado.

## Que se elimino

- El callejon sin salida de configuracion remota no disponible.
- La pantalla de pedido vivo sin acciones para pedidos legacy operables.
- La dependencia de una funcion backend ausente para operar pedidos desde Admin.
- La validacion meramente visual sin accion real.

## Que se reconstruyo

- El ciclo Admin de configuracion: abrir, leer estado remoto, cambiar modo, guardar, auditar y verificar resultado.
- El ciclo Admin de pedido vivo: detectar bandeja, abrir pedido, intervenir, persistir estado, ver reubicacion del pedido y consultar historial.
- La continuidad operativa de pedidos legacy dentro de la mesa viva.

## Como se valido

- Cuenta Admin usada: `javib18@gmail.com`.
- Dispositivo real: `EH423L012409`, modelo `24044RN32L`.
- Pedido real intervenido: `PDL-4BQDUP`, documento `4BqDUpqPbles9iY0m0lE`.
- Accion real ejecutada: `admin_intervene`, motivo `toma_admin_real`.
- Resultado backend: `operationalStatus=admin_intervention`, `currentResponsibleRole=admin`, `version=2`, `lastOperationEvent=true`.
- Resultado UI: la bandeja principal bajo de `10 sin responsable` a `9 sin responsable`, aparecio `Requieren accion: 1`, la sub-bandeja `Revision operativa` mostro `Pedido #PDL-4BQDUP`, y el historial del pedido mostro `Admin intervino el pedido: toma_admin_real`.

## Validaciones tecnicas

- `node --test tests/*.test.js`: 33/33 OK.
- `npm --prefix functions test`: 33/33 OK.
- `npm --prefix functions run build`: OK.
- `bash tools/guards/check_architecture.sh`: OK.
- `bash tools/guards/check_ui_quality.sh`: OK.
- `bash tools/guards/check_no_production_release.sh`: OK.
- `./gradlew :app:compileDebugKotlin`: OK.
- `./gradlew :app:assembleDebug`: OK.
- `adb install -r app/build/outputs/apk/debug/app-debug.apk`: OK.
- `adb shell logcat -d -t 400 AndroidRuntime:E '*:S'`: sin errores.

## Evidencia

- `reports/admin-config-final.png`
- `reports/admin-config-rain-saved.png`
- `reports/admin-without-responsible-list.png`
- `reports/admin-order-detail-final.png`
- `reports/admin-after-restart.png`
- `reports/admin-requires-action-after-intervention.png`
- `reports/admin-order-after-intervention-open.png`
- `reports/admin-order-after-intervention-actions.png`

## Pendientes

- No quedan pendientes operativos dentro de Admin Operaciones.
- Mantenimiento tecnico no bloqueante: Firebase informo que no hay cleanup policy de artifacts en `southamerica-east1`; esto no afecta el flujo Admin.
