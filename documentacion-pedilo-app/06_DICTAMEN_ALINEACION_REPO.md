# Dictamen de Alineacion - Repo Real vs Documentacion Pedilo App

## 1. Estado del repo revisado

- Rama actual: `codex/check-local-connection-and-git-status-s549dp`.
- HEAD actual: `70dd553fcaf68aa986ebfd0d1ee98f4a4b77340d`.
- Estado git: worktree sucio, con conflictos sin resolver en:
  - `app/src/main/java/com/pedilo/app/ui/admin/AdminApp.kt`
  - `tests/admin_operation_alignment.test.js`
  - `tests/admin_visual_shell.test.js`
- Tambien aparecen eliminados muchos archivos bajo `documentacion-generada-pedilo/` y agregada sin trackear la carpeta `documentacion-pedilo-app/`.
- Carpetas principales detectadas:
  - `app/`: app Android Compose con core, adapters Firebase y UI por rol.
  - `functions/`: Cloud Functions en `functions/index.js`.
  - `tests/`: tests Node de arquitectura, flujo publico, Pedido Vivo, roles, finanzas, comunicacion, IA, salud y hardening.
  - `tools/`: guards y scripts de catalogo publico.
  - `reports/`: reportes historicos por etapa.
  - `documentacion-pedilo-app/`: documentacion principal nueva.
- Rutas minimas solicitadas:
  - `app/`: existe.
  - `functions/`: existe.
  - `tests/`: existe.
  - `tools/`: existe.
  - `firestore.rules`: existe.
  - `firestore.indexes.json`: existe.
  - `firebase.json`: existe.
  - `build.gradle.kts`: existe.
  - `settings.gradle.kts`: existe.
  - `README.md`: existe.
- Comandos ejecutados:
  - `pwd`
  - `git status --short`
  - `git branch --show-current`
  - `git rev-parse HEAD`
  - `find documentacion-pedilo-app -maxdepth 2 -type f | sort`
  - `find . -maxdepth 2 -type d | sort`
  - `rg --files app functions tests tools | sort`
  - lecturas con `sed` y busquedas con `rg`
  - `node --test tests/*.test.js`
  - `npm --prefix functions run build`
  - `bash tools/guards/check_architecture.sh`
  - `bash tools/guards/check_ui_quality.sh`
  - `bash tools/guards/check_no_production_release.sh`
  - `./gradlew :app:compileDebugKotlin`

## 2. Documentacion usada

Archivos leidos:

- `documentacion-pedilo-app/00_PEDILO_APP.md`
- `documentacion-pedilo-app/01_PRODUCTO_ROLES_REGLAS.md`
- `documentacion-pedilo-app/02_PEDIDO_VIVO_UNIVERSAL.md`
- `documentacion-pedilo-app/03_ARQUITECTURA_TECNICA.md`
- `documentacion-pedilo-app/04_ETAPAS_DE_CONSTRUCCION.md`
- `documentacion-pedilo-app/05_DECISIONES_EXTERNAS.md`

Reglas principales tomadas como referencia:

- El Pedido Vivo Universal es la entidad central.
- La autoridad de estado debe vivir en backend, no en UI, chat, WhatsApp ni IA.
- Cada accion sensible valida rol, permiso, estado, version esperada e idempotencia cuando aplica.
- Cada cambio relevante registra evento.
- Android muestra vistas por rol y solicita acciones.
- Firestore Rules deben bloquear escrituras directas peligrosas, especialmente sobre `/orders`.
- Los ejes operativo, financiero, comunicacion, incidencia y archivo deben mantenerse separados.
- Los pedidos cerrados no se reabren; los reclamos posteriores se vinculan sin cambiar el cierre.
- La construccion debe seguir una etapa por vez: `Q -> B -> M -> C -> F -> D -> E -> G -> I -> J -> K -> L -> O -> P`.
- Las decisiones legales, comerciales, de Play, produccion, datos reales, dinero real e IA externa no se inventan.

## 3. Alineacion general

Dictamen general: **B) Repo parcialmente alineado, con ajustes previos necesarios.**

Justificacion: el repo contiene una arquitectura real y bastante alineada con la documentacion: Cloud Functions gobiernan el nacimiento y la operacion de pedidos, existen eventos, version esperada, idempotencia operativa por `actionId`, Rules bloquean escrituras directas sobre `/orders`, Android tiene core/ports/adapters por rol, y hay tests/guards amplios. Sin embargo, el estado real actual no esta listo para construir encima porque hay conflictos de merge activos que rompen tests y compilacion Android. Ademas, varios modulos documentados existen solo como baseline parcial, estado preparado, deshabilitado o no implementado completamente.

No corresponde dictaminar C porque el nucleo no esta desalineado de raiz. Tampoco corresponde A porque el worktree no compila y Q no queda aprobada hasta resolver conflictos y revalidar.

## 4. Matriz por modulo

| Modulo | Estado en documentacion | Estado en repo | Dictamen | Accion recomendada |
|---|---|---|---|---|
| Pedido Vivo Universal | Entidad `/orders/{id}` con eventos, cinco ejes, version, idempotencia, transiciones y backend autoritativo. | `functions/index.js` implementa `createLocalOrder`, `createPlusOrder`, `operateLiveOrder`, eventos, `expectedVersion`, `actionId`, estados operativo/financiero/comunicacion/incidencia/archivo y tests asociados. Timeouts/fallbacks son declarativos, no scheduler real. | Parcial | Conservar y ajustar |
| Backend / Firebase / Rules | Functions gobiernan cambios; Firestore persiste; Rules bloquean accesos. | Functions reales y `firestore.rules` bloquea create/update/delete cliente en `/orders`; eventos/incidents/claims/communications/ai_decisions con escritura directa denegada. Indices basicos existen. No se valido emulador real. | Parcial | Conservar y ajustar |
| Usuario Publico | Formularios, tracking seguro, cancelacion backend, reclamos posteriores. | Public UI existe; catalogo lee `/stores/products`; pedidos publicos via Functions; tracking via callable; reclamos via `submitPublicClaim`. Cancelacion publica directa no se observa como flujo completo. | Parcial | Conservar y ajustar |
| Admin | Operacion, configuracion versionada, roles reales, intervencion, salud y auditoria. | Hay adapters, callables `adminUpdateTeamUser`, `adminUpdateConfig`, `getOperationalHealth`, operacion e intervencion. Pero `AdminApp.kt` tiene conflictos activos y algunos textos indican invitacion segura no disponible. | Parcial | Ajustar |
| Local / Store | Operar pedidos propios, catalogo, multi-store, solicitud de repartidor. | Store UI/adapters existen; validacion por `storeId` en backend; catalogo publico es lectura. `store_driver_request` no aparece implementado como tipo funcional. | Parcial | Conservar y ajustar |
| Repartidor / Driver | Tomar, retirar, entregar, capacidad y cobro cuando finanzas lo habilite. | Driver UI/adapters existen; `operateLiveOrder` valida toma atomica, asignacion, retiro previo y entrega. Capacidad y cierre/cobro completo no estan completos. | Parcial | Conservar y ajustar |
| Finanzas | Eje financiero separado, snapshots de tarifa, cobro, disputa y cierre caja. | Existen estados financieros, `buildFinancialContract`, `amountToCollect`, `collectionRequired`, señales de caja avanzada y pasarela como `not_implemented`. No hay modulo financiero completo ni cierre de caja avanzado. | Parcial | Ajustar |
| Incidencias / Reclamos / Cancelaciones | Incidencias vivas, reclamos posteriores, cancelaciones por rol con evento e impacto. | `orders/{id}/incidents`, `public_claims`, `orders/{id}/claims`, cancelaciones e incidentes en `operateLiveOrder`; reclamo publico no muta el pedido vivo. Falta completar impacto financiero avanzado por cancelacion. | Parcial | Conservar y ajustar |
| Comunicacion | WhatsApp, chat, notificaciones y mensajes publicos sin autoridad operativa, con fallback. | Hay registros de comunicacion preparados/deshabilitados y templates; WhatsApp/push externos estan deshabilitados. No hay proveedor real ni chat interno completo. | Parcial | Ajustar |
| IA controlada | IA asistiva, no decide acciones sensibles. | Hay `assistedDecisionForOrder`, `resolveAssistedDecision`, provider externo deshabilitado, tests de bloqueo K. Es motor deterministico/asistivo, no IA externa real. | Parcial | Conservar y ajustar |
| Metricas / Salud | Metricas desde eventos, salud Admin, pedidos trabados y alertas. | `getOperationalHealth` arma reporte desde orders, events, incidents, claims, communications y ai_decisions; tests existen. No equivale aun a observabilidad/carga productiva completa. | Parcial | Ajustar |
| Hardening / Release | Tests, guards, carga, observabilidad, limpieza de placeholders, release certificado. | Guards pasan; no-production guard pasa; test suite falla por conflictos; Gradle no compila por conflictos; decisiones externas Play siguen pendientes segun doc. | Desalineado en estado actual | Ajustar |

## 5. Conservar

- `functions/index.js`: contiene el nucleo backend autoritativo, nacimiento de pedidos, operacion viva, eventos, version, idempotencia, roles, configuracion Admin, reclamos, comunicacion preparada/deshabilitada, IA asistida y salud operativa.
- `firestore.rules`: conserva una orientacion correcta de seguridad: cliente no crea/actualiza/borra `/orders`, subcolecciones criticas son backend-only y lectura se filtra por rol/alcance.
- `firebase.json` y `firestore.indexes.json`: configuracion Firebase local y indices basicos para queries por estado, store y driver.
- `app/src/main/java/com/pedilo/app/core/model`, `core/port`, `core/usecase`, `core/firebase`, `core/runtime`: separacion razonable de modelos, puertos, casos de uso y adapters.
- UI publica bajo `app/src/main/java/com/pedilo/app/ui/publicuser`: esta conectada a catalogo, pedidos, tracking y reclamos mediante adapters/use cases.
- UI Store y Driver bajo `app/src/main/java/com/pedilo/app/ui/store` y `app/src/main/java/com/pedilo/app/ui/driver`: operan contra `operateLiveOrder`, no como autoridad local.
- Tests que pasan: 31 de 33 archivos ejecutados pasaron pese al estado general.
- Guards en `tools/guards/`: arquitectura, calidad UI y no-produccion pasan y son utiles para Q/O.
- `README.md`: describe de manera bastante fiel el estado real actual, aunque debe considerarse secundario frente a la documentacion principal.

## 6. Ajustar

- Resolver conflictos en `app/src/main/java/com/pedilo/app/ui/admin/AdminApp.kt`.
- Resolver conflictos en `tests/admin_operation_alignment.test.js`.
- Resolver conflictos en `tests/admin_visual_shell.test.js`.
- Reejecutar `node --test tests/*.test.js` hasta que no fallen por sintaxis.
- Reejecutar `./gradlew :app:compileDebugKotlin` y luego `./gradlew :app:assembleDebug` si corresponde.
- Completar o delimitar el alcance real de Admin: configuracion, roles, invitacion segura y versionado de configuracion frente a pedidos vivos.
- Completar la cancelacion publica si se va a iniciar etapa C; hoy hay reclamo publico y tracking, pero no se evidencio callable especifica de cancelacion publica.
- Completar el motor real de timeouts/fallbacks: hoy las politicas iniciales son declarativas y sin scheduler.
- Reforzar tests/emulator para Rules si la siguiente etapa toca M.
- Alinear nombres conceptuales/persistidos de estados sin renombrar datos persistidos sin migracion.

## 7. Aislar o reemplazar

- Conflictos de merge en Admin y tests: deben aislarse antes de cualquier feature. No son funcionalidad valida.
- `adminOrderAction`: el README lo llamaba "legacy dedicada"; en el cierre posterior se retiro y no debe reconstruirse.
- `OperationData.kt` y partes de Admin con textos `Sin datos` para universos no alimentados: no necesariamente son demo, pero deben tratarse como shell parcial de operacion hasta estar conectados a datos reales.
- Entradas Admin "Crear Admin/Local/Repartidor - No disponible hasta definir invitacion segura": mantener como no disponible o reemplazar por flujo real en etapa F; no construir encima como si ya fuera alta real completa.
- Comunicacion WhatsApp/push externa e IA externa: conservar deshabilitadas/preparadas; no tratarlas como integraciones productivas.
- Caja avanzada, banco/pasarela, Google Play, produccion y carga: aparecen como `not_implemented`, `not_ready` o `pending_o`; no deben venderse como listos.
- `tools/seed_public_catalog.js`: herramienta de seed, no parte de runtime ni fuente de verdad productiva.
- `documentacion-generada-pedilo/`: figura como borrada en git status. No usarla como fuente principal salvo decision explicita; la fuente pedida es `documentacion-pedilo-app/`.

## 8. Faltantes principales

- Worktree limpio y conflictos resueltos.
- Build Android exitoso.
- Suite completa de tests sin fallas.
- Scheduler/worker real de timeouts y fallbacks.
- `store_driver_request` funcional.
- Cancelacion publica backend completa y visible si se requiere en C.
- Finanzas completas: tarifas versionadas/snapshot completas, disputas, cobro, cierre de caja, conciliacion.
- Capacidad real de repartidor.
- Chat interno completo y proveedor/fallback real de comunicacion cuando corresponda.
- IA externa real no esta activada y no debe activarse sin decision externa.
- Observabilidad/carga/estres de hardening.
- Release certificado, AAB y datos Play; bloqueados por etapa O/P y decisiones externas.
- Validacion emulator real de Rules/Functions no confirmada en esta auditoria.

## 9. Riesgos antes de construir

- Construir sobre archivos con conflictos puede mezclar dos versiones incompatibles de Admin y tests.
- El repo no compila actualmente; cualquier feature nueva esconderia el bloqueo real.
- Tests fallan por sintaxis en dos archivos, por lo que no hay senal verde de Q.
- Admin es el area mas riesgosa: combina UI en conflicto, configuracion, roles, salud y operacion sensible.
- `adminOrderAction` legacy junto a `operateLiveOrder` podia duplicar caminos de estado; el cierre posterior lo retiro.
- Timeouts declarativos pueden dar falsa sensacion de motor automatico.
- Comunicacion preparada/deshabilitada puede confundirse con WhatsApp/push reales.
- IA asistida deterministica puede confundirse con IA externa o con capacidad de decidir, cosa que la documentacion prohibe.
- Finanzas tiene estados y baseline, pero no cierre de caja completo; riesgo de mezclar cierre operativo con financiero.
- Store/Driver existen, pero capacidad, caja y solicitud de repartidor desde local siguen incompletas.
- Rules son buenas como baseline, pero cualquier coleccion nueva sin Rules/tests violaria la arquitectura.
- Placeholders/shells visuales en Admin podrian pasar como modulo real si no se etiquetan como parciales.
- Datos demo/seed no deben entrar en release.

## 10. Validaciones ejecutadas

| Comando | Resultado | Error si hubo | Impacto |
|---|---|---|---|
| `git status --short` | Falla de estado repo: hay conflictos y muchos cambios no resueltos. | `UU` en `AdminApp.kt`, `admin_operation_alignment.test.js`, `admin_visual_shell.test.js`; muchos `D` en `documentacion-generada-pedilo/`; `?? documentacion-pedilo-app/`. | Bloquea dictamen A y bloquea construccion segura. |
| `git branch --show-current` | OK | - | Rama registrada. |
| `git rev-parse HEAD` | OK | - | HEAD registrado. |
| `rg --files app functions tests tools | sort` | OK | - | Inventario tecnico obtenido. |
| `node --test tests/*.test.js` | Fallo parcial: 31 pasan, 2 fallan. | `SyntaxError: Unexpected token '<<'` en `tests/admin_operation_alignment.test.js:67` y `tests/admin_visual_shell.test.js:353`. | Bloquea Q hasta resolver conflictos. |
| `npm --prefix functions run build` | OK | `node --check index.js` sin errores. | Backend JS parsea correctamente. |
| `bash tools/guards/check_architecture.sh` | OK | `architecture guard passed`. | Senal positiva de arquitectura base. |
| `bash tools/guards/check_ui_quality.sh` | OK | `ui quality guard passed`. | Senal positiva, aunque no reemplaza compilacion. |
| `bash tools/guards/check_no_production_release.sh` | OK | `no-production-release guard passed`. | No se detectaron claims/comandos productivos prohibidos en runtime paths. |
| `./gradlew :app:compileDebugKotlin` en sandbox | Fallo ambiental. | `FileNotFoundException ... gradle-8.9-bin.zip.lck (Read-only file system)`. | No diagnostica codigo; requirio correr fuera del sandbox. |
| `./gradlew :app:compileDebugKotlin` fuera del sandbox | Fallo real de compilacion. | Multiples errores Kotlin por marcadores `<<<<<<<`, `=======`, `>>>>>>>` en `AdminApp.kt`, empezando en linea 369. | Bloquea build Android y cualquier construccion posterior. |

## 11. Etapa recomendada para iniciar trabajo

La etapa que corresponde ejecutar primero es **Q - Revalidacion tecnica**, no B ni M todavia.

Motivo: la propia etapa Q exige inventario, tests, guards y placeholders registrados. En el estado real, Q detecta conflictos activos y build roto. El siguiente trabajo no debe implementar features; debe cerrar Q resolviendo los conflictos de merge, revalidando tests/guards/build y dejando un baseline limpio.

## 12. Bloqueos

### Bloqueos tecnicos

- Conflictos sin resolver en `AdminApp.kt`.
- Conflictos sin resolver en `tests/admin_operation_alignment.test.js`.
- Conflictos sin resolver en `tests/admin_visual_shell.test.js`.
- `node --test tests/*.test.js` falla por sintaxis de conflictos.
- `./gradlew :app:compileDebugKotlin` falla por sintaxis de conflictos.
- No se puede considerar `assembleDebug` ni hardening mientras compileDebugKotlin falla.

### Bloqueos de documentacion

- No hay contradiccion fuerte detectada entre la documentacion principal y el nucleo real.
- Si se decide conservar rutas/documentacion vieja bajo `documentacion-generada-pedilo/`, debe aclararse su rol; hoy aparece borrada en git status y no fue usada como fuente principal.
- La documentacion marca `store_driver_request`, timeouts, finanzas, comunicacion, IA, metricas y release como pendientes por etapa; el repo contiene baselines parciales que deben interpretarse como preparacion, no como cierre documental.

### Bloqueos externos

- EXT-01: razon social o titular legal, bloquea publicacion Play/textos legales definitivos.
- EXT-02: email oficial de soporte, bloquea ficha Play y soporte formal.
- EXT-03: aprobacion juridica de privacidad y datos, bloquea publicacion Play.
- Deploy Firebase/produccion, publicacion Google Play, pasarela real, IA externa con costo/datos sensibles y datos reales requieren autorizacion explicita.

## 13. Conclusion final

- ¿El repo esta listo para que otro agente empiece a construir? **No todavia.** Esta parcialmente alineado, pero primero hay que cerrar Q con conflictos resueltos y validaciones verdes.
- ¿Que debe mirar primero? `git status`, los marcadores de conflicto en `AdminApp.kt`, `tests/admin_operation_alignment.test.js` y `tests/admin_visual_shell.test.js`, luego `node --test tests/*.test.js` y `./gradlew :app:compileDebugKotlin`.
- ¿Que no debe tocar? No debe tocar reglas de producto, datos reales, deploy, Play, pasarela, IA externa, ni avanzar B/M/C/F mientras el baseline no compile.
- ¿Que debe conservar? El nucleo de `functions/index.js`, `firestore.rules`, core Android por ports/use cases/adapters, UI publica/store/driver conectada a backend, tests y guards existentes.
- ¿Que debe aislar? Conflictos de merge, caminos legacy frente a `operateLiveOrder`, shells parciales de Admin, comunicacion/IA/proveedor externo deshabilitados, caja avanzada/pasarela/release no implementados.

Dictamen operativo: **cerrar Q primero**. Despues de Q limpio, la siguiente etapa natural segun la documentacion seria revisar si B puede darse por suficientemente cubierta o si debe endurecerse formalmente antes de M; pero no se debe saltar a construccion por roles con el repo en conflicto.
