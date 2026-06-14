# 10 - Certificacion de uso humano real

Fecha: 2026-06-12

## Resultado

**NO APTA para uso humano real controlado.**

La app compila, genera APK debug, pasa la suite automatizada y arranca en un telefono fisico. Tambien se corrigieron fallas reales encontradas durante la auditoria.

No se certifica como APTA porque no se pudo ejecutar el ciclo humano completo con escrituras reales en un entorno Firebase de prueba autorizado, con catalogo no demo, usuarios operativos reales, Rules evaluadas en emulator y datos controlados. Sin esa prueba, no corresponde afirmar que los flujos principales funcionan de punta a punta para personas reales.

## Que se audito

- Documentacion vigente en `documentacion-pedilo-app/`.
- Backend en `functions/index.js`.
- Reglas Firestore en `firestore.rules`.
- Flujos Android Publico, Store, Driver y Admin.
- Adapters Firebase y fronteras UI/use case/port.
- Tests existentes y guards.
- Build Kotlin y APK debug.
- Instalacion y arranque en telefono fisico Android `24044RN32L`, pantalla `720x1650`.

## Flujos humanos revisados

- Usuario Publico: home, catalogo publico, tracking, cancelacion publica, reclamos, pedido local y pedido Plus por codigo/fuentes.
- Store: lectura de pedidos propios, acciones por `operateLiveOrder`, catalogo propio read-only, solicitud de repartidor.
- Driver: pedidos disponibles/asignados, toma, retiro, entrega, capacidad y cierre de caja operativo.
- Admin: operacion, salud, configuracion, equipo, intervencion y resolucion asistida.

En dispositivo fisico se probo instalacion, arranque, cierre de sesion desde Admin persistido y home publica. No se enviaron pedidos reales ni se escribieron datos remotos por falta de autorizacion explicita de entorno/datos.

## Edge cases y estres

- Version vieja contra `expectedVersion`.
- Doble accion/idempotencia.
- Reutilizacion de `actionId` por otro actor.
- Doble toma y toma por driver ajeno.
- Entrega sin retiro previo.
- Pedido cerrado sin acciones posteriores.
- Cancelacion publica fuera de estados permitidos.
- Reclamo publico sin reabrir pedido cerrado.
- Driver con capacidad llena.
- Store intentando operar pedido ajeno.
- Lecturas de Rules por rol.
- Catalogo seed/demo presentado como real.
- Totales de carrito local desalineados con backend.
- Timeouts/fallbacks y salud operacional por pruebas de codigo.

## Errores encontrados

- `requireOperationalActor` no cargaba `driverCapacity`, por lo que el control de capacidad caia al default y no respetaba capacidad real del repartidor.
- `operateLiveOrder` devolvia resultados idempotentes de eventos existentes antes de validar que el evento perteneciera al mismo actor y accion.
- `firestore.rules` permitia lectura de pedidos por coincidencia de `storeId` o `driverId` sin atar esa condicion al rol correspondiente.
- `store_driver_request` podia ofrecerse desde `accepted`, adelantando disponibilidad de reparto antes de preparacion.
- La UI publica/local tenia restos de fixture `pizzeria-roma` como selector operativo.
- El carrito local permitia tamanos/extras y sumaba envio, pero el backend recalcula desde productos Firestore sin esas variantes, generando riesgo de total mostrado distinto al persistido.
- El Firebase conectado al APK devolvia el seed `Pizzeria Roma / Promo del dia`; se veia como catalogo real en el telefono.

## Que se corrigio

- Actor backend ahora incluye `driverCapacity` / `maxActiveOrders` para repartidores.
- Replay idempotente de `operateLiveOrder` ahora exige mismo actor, mismo rol y misma accion.
- Firestore Rules separan lectura de pedidos por rol Store/Driver/Admin.
- `store_driver_request` ya no se habilita desde `accepted`; requiere pedido en preparacion.
- Home, busqueda, subcategoria y Local navegan por `storeId` real seleccionado.
- Pedido local arma draft con el local seleccionado o el `storeId` del carrito.
- Se quitaron extras/tamanos/envio no respaldados por backend; total local = precio real del producto x cantidad.
- Mapper de catalogo publico filtra el seed conocido `pizzeria-roma` para no presentarlo como local operativo.
- Se agregaron tests para los cambios anteriores.

## Validaciones ejecutadas

| Validacion | Resultado |
| --- | --- |
| `node --test tests/*.test.js` | OK: 33/33 tests pasan. |
| `npm --prefix functions test` | OK: 33/33 tests pasan. |
| `npm --prefix functions run build` | OK: `node --check index.js`. |
| `bash tools/guards/check_architecture.sh` | OK. |
| `bash tools/guards/check_ui_quality.sh` | OK. |
| `bash tools/guards/check_no_production_release.sh` | OK. |
| `./gradlew :app:compileDebugKotlin` | OK fuera del sandbox. |
| `./gradlew :app:assembleDebug` | OK fuera del sandbox; APK debug generado. |
| `adb install -r app/build/outputs/apk/debug/app-debug.apk` | OK en telefono fisico. |
| `adb shell am start -n com.pedilo.app/.MainActivity` | OK; app arranca. |
| `adb logcat -d -t 1000 AndroidRuntime:E '*:S'` | OK sin crashes AndroidRuntime en la ventana revisada. |

## No probado o pendiente

- No se ejecuto flujo completo real de pedido Publico -> Store -> Driver -> entrega contra Firebase de prueba autorizado.
- No se escribieron pedidos remotos ni datos reales.
- No se probo Firestore Rules con emulator real; las pruebas actuales inspeccionan reglas/codigo y cubren contratos por unidad.
- No se probo carga real multiusuario con dos celulares operando la misma orden.
- No se valido un catalogo real autorizado distinto del seed bloqueado.
- No se hizo deploy productivo ni publicacion.
- No se integro pasarela, banco, proveedor WhatsApp/push ni IA externa.
- No se resolvieron decisiones legales/comerciales externas.

## Bloqueo de certificacion

Para declarar **APTA** falta una prueba controlada con:

- entorno Firebase de prueba autorizado;
- catalogo real de prueba, no seed/demo;
- usuarios activos Admin, Store y Driver;
- ejecucion real de pedidos y acciones desde celulares;
- evaluacion de Firestore Rules con emulator o entorno aislado;
- evidencia de concurrencia real para doble toma/doble accion;
- datos de prueba autorizados y limpiables.

Hasta entonces, el dictamen correcto es **NO APTA para uso humano real controlado**.
