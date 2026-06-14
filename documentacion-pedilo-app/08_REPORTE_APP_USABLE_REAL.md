# 08 - Reporte App Usable Real

Fecha: 2026-06-12

## Resultado

**APTA para uso real controlado en celulares de prueba**, sin publicacion, sin deploy productivo y sin decisiones legales/comerciales inventadas.

La base quedo preparada para operar contra un entorno Firebase de prueba autorizado, con APK debug generable, reglas y Functions validadas por tests/guards, y con estados de pedido gobernados por backend mediante `operateLiveOrder`.

## Que se elimino

- `documentacion-generada-pedilo/`: documentacion vieja/heredada eliminada del arbol de trabajo. La referencia vigente queda en `documentacion-pedilo-app/`.
- `app/src/main/java/com/pedilo/app/core/model/AdminOrderOperations.kt`: helper legacy de acciones Admin retirado.
- Ruta callable legacy `adminOrderAction` en `functions/index.js`: retirada para evitar caminos duplicados de mutacion de estado.
- Modelos/requests legacy Admin asociados a `AdminOrderAction`: retirados de la capa Android.
- Uso Android de `executeAdminOrderAction` y constante callable `adminOrderAction`: reemplazado por `executeLive` / `operateLiveOrder`.
- Shell operativo de catalogo del Local que mostraba gestion visual no disponible como bloque principal.

## Que se reemplazo

- Acciones Admin legacy por el camino universal `operateLiveOrder`, con versionado y acciones permitidas desde backend.
- Catalogo pasivo del Local por lectura real del catalogo propio en Firestore (`stores/{storeId}/products`), en modo solo lectura.
- Reglas de lectura de productos para permitir lectura publica de productos visibles/disponibles y lectura del Local activo sobre su propio catalogo.
- Tracking publico por tracking con capacidad explicita `canCancel`, sin exponer datos internos del pedido.
- Documentacion principal del README para apuntar a `documentacion-pedilo-app/` y a los callables vigentes.

## Que se construyo

- Cancelacion publica real controlada mediante `requestPublicCancellation`.
- Validacion de tracking, contacto y motivo antes de cancelar.
- Idempotencia de cancelacion publica por evento.
- Cambio de estado de cancelacion gobernado por backend, con evento operacional, comunicacion preparada y decision asistida registrada.
- UI publica para pedir cancelacion solo cuando el backend informa `canCancel`.
- Lectura real de productos del Local desde Firestore, con estados visible/disponible y dinero formateado.
- Contratos Android nuevos para tracking publico y catalogo del Local.
- Tests y guards actualizados para bloquear la reaparicion de `adminOrderAction`, shells de catalogo y datos demo como reales.

## Archivos tocados

- `README.md`
- `functions/index.js`
- `firestore.rules`
- `app/src/main/java/com/pedilo/app/core/firebase/FirebaseAdminOrdersAdapter.kt`
- `app/src/main/java/com/pedilo/app/core/firebase/FirebasePublicTrackingAdapter.kt`
- `app/src/main/java/com/pedilo/app/core/firebase/FirebaseStoreOrdersAdapter.kt`
- `app/src/main/java/com/pedilo/app/core/model/AdminOrderReadModels.kt`
- `app/src/main/java/com/pedilo/app/core/model/PublicTrackingState.kt`
- `app/src/main/java/com/pedilo/app/core/model/AdminOrderOperations.kt` eliminado
- `app/src/main/java/com/pedilo/app/core/port/AdminOrdersPort.kt`
- `app/src/main/java/com/pedilo/app/core/port/PublicTrackingPort.kt`
- `app/src/main/java/com/pedilo/app/core/port/StoreOrdersPort.kt`
- `app/src/main/java/com/pedilo/app/core/usecase/GetAdminOperationOrdersUseCase.kt`
- `app/src/main/java/com/pedilo/app/core/usecase/GetPublicTrackingUseCase.kt`
- `app/src/main/java/com/pedilo/app/core/usecase/GetStoreOrdersUseCase.kt`
- `app/src/main/java/com/pedilo/app/ui/publicuser/PublicShopTracking.kt`
- `app/src/main/java/com/pedilo/app/ui/store/StoreApp.kt`
- `app/src/main/java/com/pedilo/app/ui/driver/DriverApp.kt`
- `tests/*.test.js` relacionados con Admin, Store, tracking publico, reglas, hardening y auditoria final.
- `tools/guards/check_architecture.sh`
- `documentacion-pedilo-app/06_DICTAMEN_ALINEACION_REPO.md`
- `documentacion-pedilo-app/07_CIERRE_PREPARACION_TECNICA.md`
- `documentacion-pedilo-app/08_REPORTE_APP_USABLE_REAL.md`
- `documentacion-generada-pedilo/` eliminado del arbol de trabajo.

## Flujo real funcionando

- Usuario Publico: carga pedidos Local/Plus, valida datos, recibe tracking, consulta estado, envia reclamo posterior y puede solicitar cancelacion mientras el pedido lo permite.
- Pedido Vivo Universal: nace con contrato operativo, eventos, version, responsables, acciones permitidas y mutaciones centralizadas en backend.
- Admin: opera pedidos vivos por `operateLiveOrder`, revisa salud/configuracion/usuarios existentes y conserva separacion de responsabilidades.
- Local/Store: ve sus pedidos, ejecuta acciones permitidas por backend, consulta su catalogo propio en modo real de solo lectura y no escribe estados directamente.
- Repartidor/Driver: ve pedidos asignados o tomables, ejecuta acciones permitidas por backend y no decide estados fuera del contrato.
- Incidencias, reclamos y cancelaciones: registradas como eventos y estados controlados, sin mutacion directa desde UI.
- Finanzas/cobros base: el pedido conserva contrato financiero minimo y cobro declarado; pasarela, banco y caja avanzada siguen bloqueados como decisiones externas/no implementadas.
- Metricas/salud: health y modulos de preparacion quedan visibles para Admin sin declararse produccion lista.

## Validaciones

| Validacion | Resultado |
| --- | --- |
| `node --test tests/*.test.js` | OK: 33/33 tests pasan. |
| `npm --prefix functions test` | OK: 33/33 tests pasan desde Functions. |
| `npm --prefix functions run build` | OK: `node --check index.js`. |
| `bash tools/guards/check_architecture.sh` | OK. |
| `bash tools/guards/check_ui_quality.sh` | OK. |
| `bash tools/guards/check_no_production_release.sh` | OK. |
| `./gradlew :app:compileDebugKotlin` | OK fuera del sandbox: build successful. Warnings no bloqueantes de casts unchecked en `FirebaseAdminOrdersAdapter.kt`. |
| `./gradlew :app:assembleDebug` | OK fuera del sandbox: APK debug generado. |

## Pendiente externo

- Razon social/titular legal.
- Email oficial de soporte y textos legales definitivos.
- Aprobacion juridica de privacidad/datos.
- Deploy a entorno Firebase de prueba o produccion: requiere autorizacion explicita.
- Publicacion Google Play y ficha Play.
- Pasarela real de pago, banco y caja avanzada.
- Proveedor real de WhatsApp/push.
- IA externa con costo o datos sensibles.
- Flujo seguro de invitacion/alta de nuevas cuentas Admin/Local/Repartidor.

## No se hizo

- No se publico en Google Play.
- No se preparo ficha Play como prioridad.
- No se invento titular legal, razon social, email oficial ni textos legales definitivos.
- No se hizo deploy productivo.
- No se integro pasarela real de pago.
- No se activo IA externa.
- No se usaron datos reales.

## Que se conserva

- `documentacion-pedilo-app/` como referencia vigente.
- `functions/index.js` como autoridad de pedidos vivos, validaciones, eventos, comunicacion preparada, salud y finanzas base.
- `firestore.rules` como barrera de lectura/escritura por rol.
- Android por roles separados: Publico, Admin, Local y Repartidor.
- Puertos/use cases/adapters como frontera tecnica entre UI y Firebase.
- Tests y guards como contrato minimo de continuidad.

## Que no debe usarse como base cerrada

- Cualquier reconstruccion de `adminOrderAction`.
- `documentacion-generada-pedilo/` como referencia principal.
- Alta de cuentas Admin/Local/Repartidor como flujo terminado.
- Caja avanzada, banco/pasarela, Google Play, produccion o IA externa como modulos listos.
- Textos o datos legales/comerciales no definidos.
- Pantallas de salud/configuracion como sustituto de observabilidad productiva completa.

## Decision de continuidad

El siguiente agente puede empezar construccion incremental o pruebas reales controladas sobre esta base sin redefinir Pédilo.

Condicion: usar entorno Firebase de prueba autorizado y mantener fuera de alcance legal/Play/produccion hasta que el dueño tome esas decisiones.
