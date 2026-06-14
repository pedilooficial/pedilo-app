# 19 - Admin auditado con pedido nuevo

Fecha: 2026-06-13

## Resultado

**NO APTA como cierre definitivo total.**

El flujo operativo Admin con pedido nuevo fue validado y corregido: Admin vio el pedido, lo abrio, ejecuto una accion permitida por backend, vio resultado, vio historial/evento y la mesa se actualizo.

No se declara APTA total porque el entorno conectado todavia muestra pedidos residuales asociados a `Pizzeria Roma`, identificado en documentacion previa como seed/demo riesgoso. Ese dato no fue inventado ni ocultado desde UI. Requiere limpieza o entorno controlado autorizado para cerrar Admin como modulo definitivo sin mezclar operacion real y datos de prueba.

## Archivo maestro usado

- `documentacion-pedilo-app/PLANO_MAESTRO_PEDILO_APP_PARA_AGENTES.md`

## Pedido auditado

- Pedido: `PDL-4BQDUP`
- Cola inicial: `Problemas > Revision operativa`
- Estado anterior visible: `Retiro solicitado`, `Con problema`
- Accion ejecutada: `Resolver incidencia`
- Motivo: `cierre_admin_real`
- Resultado visible: `Incidencia resuelta.`
- Estado posterior de mesa: `Requieren accion` bajo de `1` a `0`; `Problemas` bajo de `11` a `10`; `Activos` subio a `1`.
- Historial visible: `Admin resolvio incidencia: cierre_admin_real`

## Hallazgos corregidos

- Resultado post-accion quedaba debajo del proximo paso y podia confundirse con nuevas acciones.
- Acciones posteriores seguian visibles inmediatamente despues del resultado.
- `Volver a mesa` aparecia con chip generico `Lectura`.
- Eventos/motivos historicos podian verse con texto URL-encoded, por ejemplo `%20`.
- Configuracion mostraba chips inferidos como `Bloqueo` para controles inactivos.

## Cambios aplicados

- En detalle de pedido, despues de una accion se muestra primero `Resultado`, luego `Historial reciente`, luego `Volver a mesa`.
- Mientras hay resultado de accion, se oculta la lista de acciones posteriores para evitar encadenar operaciones sin volver a la mesa.
- Se decodifican textos de evento/resumen/motivo antes de mostrarlos.
- `Volver a mesa` se clasifica como accion lista, no como lectura.
- Configuracion usa tarjetas especificas con chip `activo` / `inactivo`.

## Reglas protegidas

- Android no infiere acciones.
- Las acciones visibles siguen viniendo de `nextAllowedActions`.
- La accion se ejecuto con `expectedVersion`.
- La mutacion paso por `operateLiveOrder`.
- No se escribio directo en `/orders`.
- El evento quedo visible en historial.

## Evidencia visual versionable

- `documentacion-pedilo-app/evidencias-admin-auditado-2026-06-13/01_mesa_inicial_pedido_nuevo.png`
- `documentacion-pedilo-app/evidencias-admin-auditado-2026-06-13/02_lista_revision_operativa_pedido.png`
- `documentacion-pedilo-app/evidencias-admin-auditado-2026-06-13/03_detalle_antes_accion.png`
- `documentacion-pedilo-app/evidencias-admin-auditado-2026-06-13/04_confirmar_resolver_incidencia.png`
- `documentacion-pedilo-app/evidencias-admin-auditado-2026-06-13/05_resultado_incidencia_resuelta.png`
- `documentacion-pedilo-app/evidencias-admin-auditado-2026-06-13/06_historial_evento_generado.png`
- `documentacion-pedilo-app/evidencias-admin-auditado-2026-06-13/07_mesa_post_accion.png`
- `documentacion-pedilo-app/evidencias-admin-auditado-2026-06-13/08_configuracion_final.png`
- `documentacion-pedilo-app/evidencias-admin-auditado-2026-06-13/09_equipo_final.png`

## Validacion ejecutada

- `node --test tests/admin_operation_alignment.test.js tests/admin_operational_actions.test.js tests/admin_visual_shell.test.js` - PASS.
- `node --test tests/operational_order_actions_backend.test.js tests/firestore_rules.test.js tests/live_order_end_to_end_flow.test.js` - PASS.
- `./gradlew :app:assembleDebug` - BUILD SUCCESSFUL.
- `adb install -r app/build/outputs/apk/debug/app-debug.apk` - Success.

## Archivos tocados

- `app/src/main/java/com/pedilo/app/ui/admin/AdminApp.kt`
- `tests/admin_visual_shell.test.js`
- `tests/admin_operation_alignment.test.js`
- `documentacion-pedilo-app/19_ADMIN_AUDITADO_PEDIDO_NUEVO_2026_06_13.md`
- `documentacion-pedilo-app/evidencias-admin-auditado-2026-06-13/*`

## Riesgo residual

- El entorno conectado muestra pedidos con `Pizzeria Roma`, dato marcado previamente como seed/demo. No se puede declarar Admin APTA total mientras el entorno de validacion mezcle esos pedidos con operacion real controlada.
- Quedan 10 pedidos `Sin responsable` en la mesa. Admin los muestra y permite abrirlos, pero no fueron cerrados uno por uno en esta pasada.

## Siguiente paso para APTA

- Limpiar o aislar los pedidos residuales seed/demo del entorno usado por el APK.
- Repetir la validacion sobre un entorno controlado sin datos demo tratados como reales.
- Cerrar los pedidos sin responsable o confirmar que son parte del escenario controlado.
