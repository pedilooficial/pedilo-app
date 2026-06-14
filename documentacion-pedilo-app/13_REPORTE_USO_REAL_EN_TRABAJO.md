# 13 - Reporte uso real en trabajo

Fecha: 2026-06-12

## Resultado

Pédilo App queda operable tecnicamente para uso real en trabajo controlado: las pantallas por rol tienen flujo humano, acciones condicionadas por backend, estados explicitos, mensajes de error entendibles y build debug generado.

No se certifica recorrido visual multirol en celular en esta sesion porque `adb devices` no mostro dispositivos conectados. Por lo tanto, no se declara instalacion/apertura ni validacion visual de Admin, Local o Repartidor con credenciales en esta pasada.

## Que impedia el uso real en trabajo

- Local y Repartidor ya tenian acciones reales, pero ante doble toque o mala conexion el operador podia volver a confirmar sin una senal clara de accion en curso.
- Los errores de carga por sesion/conexion eran demasiado genericos para una persona trabajando.
- La operacion intensa requiere que el operador entienda que debe esperar respuesta del backend para la version del pedido antes de tocar otra accion.
- Sin dispositivo conectado no se puede cerrar la validacion visual real de roles, aunque los contratos, guards y APK pasen.

## UI redisenada o reforzada

- Local/Store: se agrego estado visible de "Procesando accion" mientras `operateLiveOrder` responde.
- Local/Store: el dialogo de confirmacion bloquea confirmar/cancelar y editar motivo mientras una accion esta en curso.
- Repartidor/Driver: se agrego estado visible de "Procesando accion" para toma, retiro, entrega, incidencia y cancelacion.
- Repartidor/Driver: el cierre de caja bloquea doble confirmacion y muestra "Cerrando caja" hasta recibir respuesta.
- Local y Repartidor: los errores de carga ahora indican revisar conexion o volver a iniciar sesion si la cuenta vencio.

## Flujo operativo usable

- Publico puede iniciar pedido local o Plus, completar datos, confirmar, obtener ticket, consultar tracking y reclamar por el camino publico sin reabrir pedidos cerrados.
- Local ve pedidos propios, catalogo propio, estado financiero/comunicacion y acciones disponibles segun `nextAllowedActions`.
- Repartidor ve disponibles/asignados, pedido actual, cobro/caja y acciones disponibles segun `nextAllowedActions`.
- Admin mantiene mesa de control con prioridades, problemas, salud operativa, configuracion, equipo e intervenciones auditadas.
- Las pantallas no inventan estados: usan version del pedido, acciones habilitadas y resultado humano devuelto por backend.

## Publico

- Conserva validacion de formularios contra datos vacios o placeholders.
- Tracking publico usa formato controlado y mensajes seguros.
- Reclamos/cancelaciones quedan separados del flujo normal de pedido cerrado.
- No se agregaron datos demo como reales.

## Admin

- Mantiene `operateLiveOrder` como camino de intervencion y evita rutas legacy.
- La UI de mesa de control sigue cubierta por tests de alineacion y shell visual.
- Las acciones Admin se sostienen por clasificacion, versionado, auditoria y errores operativos humanos.

## Local / Store

- Acciones disponibles solo desde `current.nextAllowedActions`.
- Cada accion se envia con `expectedVersion`.
- Rechazo, cancelacion e incidencia exigen motivo operativo.
- Nueva proteccion contra doble confirmacion mientras el backend procesa.
- Mensajes de sesion/conexion quedaron entendibles para trabajo real.

## Repartidor / Driver

- Pedidos disponibles y asignados se separan por visibilidad/autorizacion.
- Tomar, retirar, entregar, cancelar e incidencia usan `operateLiveOrder` con version esperada.
- Caja operativa queda protegida contra doble cierre.
- La UI explica cuando no hay accion disponible y cuando se esta esperando backend.

## Escenarios de uso intenso validados

- Muchos pedidos seguidos: listas por rol y contadores operativos cubiertos por tests.
- Acciones repetidas/doble toque: Local, Driver y caja bloquean confirmaciones mientras hay accion en curso.
- Mala conexion o sesion vencida: mensajes indican revisar conexion o reiniciar sesion.
- Pedido sin repartidor: Local solo puede solicitarlo cuando backend habilita la accion.
- Driver con capacidad llena: la toma sigue delegada a backend y la UI no fuerza asignacion.
- Cancelacion/incidencia: requieren motivo cuando corresponden.
- Estados cambiando mientras otro rol mira: version esperada se envia en cada accion.
- Permisos incorrectos: adapters filtran por usuario/rol y rules/tests verifican visibilidad.

## Validaciones ejecutadas

| Validacion | Resultado |
| --- | --- |
| `node --test tests/*.test.js` | OK, 33/33. |
| `npm --prefix functions test` | OK, 33/33. |
| `npm --prefix functions run build` | OK. |
| `bash tools/guards/check_architecture.sh` | OK. |
| `bash tools/guards/check_ui_quality.sh` | OK. |
| `bash tools/guards/check_no_production_release.sh` | OK. |
| `./gradlew :app:compileDebugKotlin` | OK. Requirio ejecucion fuera del sandbox por lock de `~/.gradle`. |
| `./gradlew :app:assembleDebug` | OK. APK generado en `app/build/outputs/apk/debug/app-debug.apk`. |
| `adb devices` | OK, sin dispositivos conectados. |

## No certificado visualmente en esta sesion

- Instalacion del APK en celular.
- Apertura de la app en celular.
- Recorrido visual con credenciales Admin, Local y Repartidor.
- Operacion multirol real contra Firebase con pedidos vivos.

## Dictamen

El producto queda mas apto para uso real en trabajo controlado porque la UI guia la operacion y evita acciones repetidas mientras el backend decide. La autoridad de estado sigue en backend. La certificacion visual multirol queda pendiente por falta de dispositivo conectado en esta sesion.
