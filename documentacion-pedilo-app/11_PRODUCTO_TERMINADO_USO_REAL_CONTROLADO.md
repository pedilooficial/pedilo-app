# 11 - Producto terminado para uso real controlado

Fecha: 2026-06-12

## Resultado

**NO APTA todavia para uso real controlado completo.**

El producto quedo tecnicamente cerrado y preparado para una prueba real controlada: compila, genera APK debug, pasa tests y guards, instala y arranca en un celular fisico, no muestra catalogo demo como real y tiene scripts seguros para cargar/verificar catalogo y usuarios de prueba.

No corresponde certificar **APTA** porque no se ejecuto el flujo humano completo Publico -> Store -> Driver -> Admin contra un Firebase de prueba autorizado. El intento local con Firestore Emulator quedo bloqueado por el entorno: Firebase Tools 15 exige JDK 21+ y la maquina disponible tiene Java 17. Tampoco se recibieron UIDs Auth/credenciales/deploy autorizados para escribir un proyecto Firebase real de prueba.

## Que impedia uso real

- Catalogo seed `pizzeria-roma` podia aparecer como local real en el APK conectado.
- Home/busqueda/subcategoria/local arrastraban seleccion por nombre o fixture historico, con riesgo de pedir al local equivocado.
- Pedido local mostraba tamanos, extras y envio que el backend no respaldaba en el contrato real.
- `store_driver_request` podia ofrecerse antes de que el pedido estuviera en preparacion.
- Idempotencia de `operateLiveOrder` podia devolver un evento existente sin validar mismo actor/rol/accion.
- Capacidad Driver no se cargaba desde `/users`, por lo que el backend no respetaba capacidad real.
- Firestore Rules leian pedidos por coincidencias de campos sin atar cada permiso al rol correspondiente.
- No habia seed controlado para usuarios Admin/Store/Driver y catalogo no demo.

## Que se corrigio

- Backend carga `driverCapacity` / `maxActiveOrders` para Driver activo.
- Replay idempotente exige mismo actor, mismo rol y misma accion.
- `store_driver_request` requiere pedido en preparacion y ya no se ofrece desde `accepted`.
- Firestore Rules separan lectura por rol: Admin, Store propio y Driver asignado/disponible.
- UI publica navega por `storeId` real seleccionado.
- Local arma el pedido con el local seleccionado y productos reales del catalogo.
- Carrito local quedo alineado con backend: `priceCents * quantity`, sin extras/tamanos/envio no persistidos.
- Mapper publico filtra el seed conocido `pizzeria-roma` para no presentarlo como real.
- Se reemplazaron scripts de seed/verificacion por entorno controlado:
  - `tools/seed_public_catalog.js`
  - `tools/verify_public_catalog.js`

## Que se redisenio o elimino

- Se elimino el seed operativo `Pizzeria Roma` como fuente visible para usuarios.
- Se elimino la dependencia de nombres hardcodeados en el flujo publico/local.
- Se elimino UI de variantes no soportadas por backend para evitar totales falsos.
- Se convirtio el seed en una herramienta de prueba controlada con confirmacion explicita.

## Datos y usuarios preparados

Los scripts preparados requieren UIDs Auth reales o de emulator:

```bash
PEDILO_CONFIRM_CONTROLLED_SEED=YES \
PEDILO_ADMIN_UID=<uid-admin> \
PEDILO_STORE_UID=<uid-store> \
PEDILO_DRIVER_UID=<uid-driver> \
node tools/seed_public_catalog.js
```

Verificacion read-only:

```bash
PEDILO_ADMIN_UID=<uid-admin> \
PEDILO_STORE_UID=<uid-store> \
PEDILO_DRIVER_UID=<uid-driver> \
node tools/verify_public_catalog.js
```

El seed crea:

- `/users/<uid-admin>` con `role=admin`, `active=true`.
- `/users/<uid-store>` con `role=store`, `active=true`, `storeId=<uid-store>`.
- `/users/<uid-driver>` con `role=driver`, `active=true`, `driverCapacity=2`.
- `/stores/<uid-store>` visible, operativo y apto para pedidos.
- 4 productos reales de prueba: `mila-completa`, `ensalada-fresca`, `bebida-controlada`, `promo-almuerzo`.

Los scripts soportan Firestore Emulator mediante `FIRESTORE_EMULATOR_HOST` sin requerir Application Default Credentials.

## Flujo validado

Validado por codigo/tests:

1. Usuario Publico crea pedido local desde catalogo real.
2. Tracking publico lee por callable y permite cancelacion controlada.
3. Store opera pedidos propios por `operateLiveOrder`.
4. Store solicita repartidor solamente cuando corresponde.
5. Driver toma pedidos disponibles respetando capacidad.
6. Driver retira y entrega segun transiciones.
7. Admin supervisa, configura, interviene y ve salud operativa.
8. Cancelaciones, reclamos, incidencias, comunicaciones fallback, caja y metricas quedan cubiertos sin pasarela real.
9. Estados imposibles, doble toma, replay cruzado y cierre/reapertura quedan bloqueados por backend.

Validado en celular fisico:

- `adb install -r app/build/outputs/apk/debug/app-debug.apk`: OK.
- `adb shell am start -n com.pedilo.app/.MainActivity`: OK.
- Sin errores `AndroidRuntime` en la ventana revisada.
- La app abre en pantalla publica, sin locales/ofertas demo visibles cuando no hay catalogo real autorizado.

No validado en flujo humano real:

- No se ejecuto pedido completo multirol con celulares reales contra Firebase autorizado.
- No se ejecuto emulator completo porque Firebase Tools 15 exige JDK 21+ y el entorno tiene Java 17.

## Validaciones ejecutadas

| Validacion | Resultado |
| --- | --- |
| `node --test tests/*.test.js` | OK, 33/33. |
| `npm --prefix functions test` | OK, 33/33. |
| `npm --prefix functions run build` | OK. |
| `bash tools/guards/check_architecture.sh` | OK. |
| `bash tools/guards/check_ui_quality.sh` | OK. |
| `bash tools/guards/check_no_production_release.sh` | OK. |
| `./gradlew :app:compileDebugKotlin` | OK. |
| `./gradlew :app:assembleDebug` | OK, APK debug generado. |
| `npm --prefix tools install` | OK, dependencias de seed/verificacion instaladas. |
| `node tools/seed_public_catalog.js` sin confirmacion | OK, modo seguro sin escrituras. |
| `node tools/verify_public_catalog.js` sin UIDs | OK, falla antes de Firebase pidiendo UIDs. |
| `firebase emulators:exec --only firestore ...` | Bloqueado por JDK 21 requerido; entorno local tiene Java 17. |
| `adb install -r app/build/outputs/apk/debug/app-debug.apk` | OK. |
| `adb shell am start -n com.pedilo.app/.MainActivity` | OK. |
| `adb logcat -d -t 1000 AndroidRuntime:E '*:S'` | OK, sin crashes registrados. |

## Pendiente externo

- Proveer o autorizar proyecto Firebase de prueba.
- Crear usuarios Firebase Auth Admin/Store/Driver y entregar sus UIDs, o ejecutar el seed con esos UIDs.
- Instalar JDK 21+ para ejecutar Firestore Emulator con Firebase Tools 15, o bajar/usar una version de tooling compatible.
- Ejecutar prueba humana real con al menos Publico, Store, Driver y Admin.
- Definir legales, razon social, politicas, Play Store, pasarela bancaria, proveedor WhatsApp/push o IA paga si se avanza a produccion.

## Dictamen

El repo quedo preparado para prueba real controlada y el APK debug es usable para arrancar en telefono real. La certificacion final sigue **NO APTA** hasta ejecutar el flujo completo con datos/usuarios Firebase autorizados o emulator disponible con JDK 21+.
