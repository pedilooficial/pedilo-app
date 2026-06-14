# Cierre de Preparacion Tecnica

## Estado inicial

- Rama: `codex/check-local-connection-and-git-status-s549dp`.
- HEAD: `70dd553fcaf68aa986ebfd0d1ee98f4a4b77340d`.
- El repo tenia conflictos activos (`UU`) en:
  - `app/src/main/java/com/pedilo/app/ui/admin/AdminApp.kt`
  - `tests/admin_operation_alignment.test.js`
  - `tests/admin_visual_shell.test.js`
- La carpeta vigente `documentacion-pedilo-app/` existia como referencia nueva y contenia los documentos `00` a `06`.
- Persisten cambios previos no resueltos por esta tarea: muchos archivos de `documentacion-generada-pedilo/` figuran como eliminados en `git status`. No se restauraron ni se tomaron como fuente principal.

## Que hice

- Lei la documentacion vigente en `documentacion-pedilo-app/`.
- Tome como referencia el dictamen `06_DICTAMEN_ALINEACION_REPO.md`.
- Resolvi los conflictos activos en Admin y tests tomando la variante local que conserva superficies reales de configuracion/equipo y evita rutas heredadas como base cerrada.
- Verifique que no queden marcadores `<<<<<<<`, `=======` ni `>>>>>>>`.
- Actualice `README.md` para apuntar a `documentacion-pedilo-app/` como documentacion de trabajo y para reflejar que configuracion y accesos existentes son persistidos/auditados, mientras la creacion de cuentas nuevas sigue bloqueada hasta definir invitacion segura.
- Ejecute validaciones de backend, tests, guards y Android.

## Archivos tocados

- `README.md`
- `app/src/main/java/com/pedilo/app/ui/admin/AdminApp.kt`
- `tests/admin_operation_alignment.test.js`
- `tests/admin_visual_shell.test.js`
- `documentacion-pedilo-app/07_CIERRE_PREPARACION_TECNICA.md`

Nota: `documentacion-pedilo-app/06_DICTAMEN_ALINEACION_REPO.md` ya existia como resultado del dictamen previo y queda como antecedente de este cierre.

## Validaciones ejecutadas

| Comando | Resultado |
|---|---|
| `rg -n "^<<<<<<<|^=======|^>>>>>>>" .` | OK: sin marcadores de conflicto. |
| `node --test tests/*.test.js` | OK: 33 tests, 33 pass, 0 fail. |
| `npm --prefix functions test` | OK: 33 tests, 33 pass, 0 fail. |
| `npm --prefix functions run build` | OK: `node --check index.js` sin errores. |
| `bash tools/guards/check_architecture.sh` | OK: `architecture guard passed`. |
| `bash tools/guards/check_ui_quality.sh` | OK: `ui quality guard passed`. |
| `bash tools/guards/check_no_production_release.sh` | OK: `no-production-release guard passed`. |
| `./gradlew :app:compileDebugKotlin` | OK fuera del sandbox: build successful. Quedaron warnings no bloqueantes de casts unchecked en `FirebaseAdminOrdersAdapter.kt`. |
| `./gradlew :app:assembleDebug` | OK fuera del sandbox: build successful. |

Observacion de entorno: `./gradlew :app:compileDebugKotlin` falla dentro del sandbox porque no puede crear lock en `~/.gradle` (`Read-only file system`). Fuera del sandbox compila correctamente.

## Que quedo pendiente

- Resolver la decision de repositorio sobre los archivos eliminados bajo `documentacion-generada-pedilo/`. No bloquean la preparacion tecnica si la fuente vigente es `documentacion-pedilo-app/`, pero siguen apareciendo como cambios pendientes en `git status`.
- Completar modulos que la documentacion marca como posteriores: timeouts reales, `store_driver_request`, cancelacion publica completa, finanzas/caja, capacidad de repartidor, comunicacion real, IA externa si alguna vez se autoriza, metricas/observabilidad avanzada, hardening y release.
- Validar emulator/entorno Firebase real cuando corresponda a la etapa M. No se hizo deploy ni se tocaron datos reales.
- Resolver decisiones externas de publicacion: razon social/titular legal, email oficial de soporte y aprobacion juridica.

## Que se conserva como base valida

- `functions/index.js` como nucleo backend de Pedido Vivo: nacimiento de pedidos, operacion por rol, version esperada, eventos, idempotencia por `actionId`, reclamos, comunicacion preparada, IA asistida controlada, salud operativa, configuracion y accesos.
- `firestore.rules`, `firebase.json` y `firestore.indexes.json` como base Firebase local.
- Core Android por modelos, puertos, use cases, adapters Firebase y runtimes.
- UI publica, Store y Driver conectadas a backend.
- Admin operativo, configuracion persistida y roles/accesos para usuarios existentes.
- Tests Node y guards como red de seguridad inicial.
- `documentacion-pedilo-app/` como referencia vigente de producto, arquitectura y etapas.

## Que no debe usarse como base cerrada

- `documentacion-generada-pedilo/` hasta que se decida si se restaura, archiva o reemplaza definitivamente.
- El camino legado `adminOrderAction` quedo registrado como no vigente y fue retirado luego en la limpieza real documentada en `08_REPORTE_APP_USABLE_REAL.md`; la base vigente es `operateLiveOrder`.
- Creacion de cuentas Admin/Local/Repartidor: sigue bloqueada hasta definir invitacion segura.
- Timeouts/fallbacks declarativos: no son scheduler real.
- Comunicacion WhatsApp/push: preparada o deshabilitada, no integracion productiva.
- IA asistida actual: no es IA externa ni autoridad para acciones sensibles.
- Finanzas/caja/pasarela: baseline parcial, no modulo terminado.
- Release/Google Play/produccion: no estan habilitados ni certificados.

## Resultado

**APTA para continuar construccion real por etapas**, con alcance limitado: el repo queda sin conflictos activos, compila, pasa tests y tiene documentacion vigente integrada como referencia.

Esto no significa que Pédilo este listo para produccion ni que los modulos parciales esten terminados. Significa que la preparacion tecnica inicial queda cerrada y el siguiente agente puede empezar por la etapa que corresponda segun `04_ETAPAS_DE_CONSTRUCCION.md`, sin redefinir producto ni mezclar partes viejas.
