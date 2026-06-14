# Build Driver operational flow V1

## Que se implemento

- Se conecto el rol `driver` a una pantalla operativa real en lugar del placeholder.
- Se agrego `DriverApp` con listado de pedidos disponibles y asignados, apertura de detalle, visualizacion de estado operativo real, version y `nextAllowedActions`.
- Se incorporo el flujo de acciones del repartidor usando exclusivamente `operateLiveOrder`: tomar pedido, marcar retirado, marcar entregado, reportar incidencia y cancelar cuando el backend lo habilita.
- Se agrego refresh posterior a cada accion y mensajes humanos de resultado o error.
- Se encapsulo la lectura operativa del repartidor en `DriverOrdersPort` + adapter/use case/runtime sin escrituras directas en `/orders`.
- Se ajustaron `firestore.rules` para permitir al driver leer pedidos disponibles para tomar y pedidos propios ya asignados.

## Archivos tocados

- `app/src/main/java/com/pedilo/app/ui/publicuser/PublicApp.kt`
- `app/src/main/java/com/pedilo/app/ui/driver/DriverApp.kt`
- `app/src/main/java/com/pedilo/app/core/model/DriverOrderModels.kt`
- `app/src/main/java/com/pedilo/app/core/port/DriverOrdersPort.kt`
- `app/src/main/java/com/pedilo/app/core/usecase/GetDriverOrdersUseCase.kt`
- `app/src/main/java/com/pedilo/app/core/runtime/DriverRuntime.kt`
- `app/src/main/java/com/pedilo/app/core/firebase/FirebaseDriverOrdersAdapter.kt`
- `firestore.rules`
- `tests/driver_operational_flow.test.js`
- `tests/store_operational_flow.test.js`
- `tests/team_access_flow.test.js`
- `tests/firestore_rules.test.js`

## Acciones Driver conectadas

- `DriverTake`
- `DriverMarkPickedUp`
- `DriverMarkDelivered`
- `OpenIncident`
- `CancelOrder` cuando aparece dentro de `nextAllowedActions`

## Como usa operateLiveOrder

- La UI del driver nunca escribe directo en `/orders`.
- Cada accion construye `AdminLiveOrderActionRequest` con `orderId`, `action`, `expectedVersion` y `reason` cuando corresponde.
- `FirebaseDriverOrdersAdapter` ejecuta `getHttpsCallable("operateLiveOrder")`.
- La vista se refresca despues de cada resultado para volver a leer estado, version y acciones permitidas por backend.

## Validaciones ejecutadas

- `node --test tests/*.test.js`
- `bash tools/guards/check_architecture.sh`
- `cd functions && npm run build`
- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:assembleDebug`
- `git diff --check`

## Commit final

- `Build Driver operational flow V1`

## Riesgos restantes

- Los pedidos disponibles para tomar dependen de que los documentos operativos V1 tengan `responsibleRole = "driver"` y `assignedActorId = ""` al quedar listos para retiro.
- La visibilidad de direccion y telefono del driver asume la forma actual de `customer` y `delivery` en los pedidos vivos.
