# Limpieza profunda - archivos obsoletos Pédilo

Fecha: `2026-06-08`
HEAD de partida auditado: `462b56f` - `Validate end-to-end live order flow V1`

## 1. Resumen

Se realizó una limpieza segura del repo y del workspace local sin tocar la lógica funcional viva.

Resultado principal:

- se eliminaron `254` archivos versionados obsoletos;
- se eliminó por completo `design/`;
- se eliminaron `3` drawables no referenciados;
- se eliminaron `5` logs rastreados en `.kotlin/errors`;
- se limpiaron además outputs y artefactos locales ignorados del workspace;
- se corrigió `.gitignore` para no ocultar los reports `.md` útiles.

Dictamen preliminar:

- limpieza amplia lograda sin afectar la app viva;
- quedaron algunos archivos conservados por prudencia porque siguen referenciados por tests, runtime o bitácora técnica.

## 2. Estado inicial Git

- `git status --short`: limpio antes de empezar.
- `git log --oneline -10`: confirmado con HEAD `462b56f`.

Commits recientes observados:

- `462b56f` - `Validate end-to-end live order flow V1`
- `9e65de6` - `Build Driver operational flow V1`
- `ded4e71` - `Build Store operational flow V1`
- `26a1872` - `Connect Admin UI to operational order actions`
- `0992789` - `Build operational order action backend V1`

## 3. Candidatos detectados

### Candidatos eliminados con riesgo bajo

1. `design/**`
Tipo:
documentación, mockups, capturas, video, blueprint y material de construcción visual.
Motivo:
no participa del runtime, tests, Gradle, manifest ni Functions.
Evidencia:
- única referencia real encontrada en código/documentación viva: `README.md` mencionaba `design/00-master/`;
- no hubo referencias desde `app/`, `functions/`, `tests/`, `tools/`, `firebase.json`, `firestore.rules`.
Riesgo:
bajo.

2. `.kotlin/errors/errors-*.log`
Tipo:
logs locales del compilador Kotlin.
Motivo:
artefactos de error locales rastreados por accidente.
Evidencia:
- no referenciados por runtime, tests, guards ni Gradle.
Riesgo:
bajo.

3. `app/src/main/res/drawable/icon_512.png`
4. `app/src/main/res/drawable/icon_512_source.png`
5. `app/src/main/res/drawable/logopedilo.png`
Tipo:
recursos Android.
Motivo:
archivos no usados.
Evidencia:
- búsqueda global sin coincidencias;
- los launchers reales usan `ic_launcher_background.xml` + `ic_launcher_foreground.png`;
- splash y branding real usan `drawable-nodpi/pedilo_logo_mark.png`.
Riesgo:
bajo.

### Candidatos eliminados del workspace local ignorado

1. `.gradle/`
2. `.kotlin/` restante
3. `app/build/`
4. `functions/node_modules/`
5. `tools/node_modules/`
6. `reports/admin-visual-audit-logcat-filtered.txt`
7. `reports/*/` de capturas, visual certification y auditorías gráficas

Tipo:
caches, builds, dependencias instaladas localmente y evidencia visual no versionada.
Motivo:
ruido local no necesario para la app viva.
Evidencia:
- estaban ignorados por `.gitignore` o no participaban del runtime;
- no eran fuente de verdad funcional;
- eran artefactos de validación visual local.
Riesgo:
bajo.

### Candidatos conservados por duda o decisión prudente

1. `reports/*.md`
Motivo:
siguen sirviendo como bitácora técnica reciente y algunos están alineados con commits reales.
Riesgo de borrado:
medio.

2. `tools/seed_public_catalog.js`
3. `tools/verify_public_catalog.js`
Motivo:
no participan del runtime, pero siguen referenciados por `tests/catalog_readonly.test.js`.
Riesgo de borrado:
medio.

3. `app/src/main/java/com/pedilo/app/ui/admin/components/AdminComponents.kt`
4. `app/src/main/java/com/pedilo/app/ui/admin/operation/AdminOperationNavigation.kt`
5. `app/src/main/java/com/pedilo/app/ui/admin/operation/OperationData.kt`
6. `app/src/main/java/com/pedilo/app/ui/admin/roles/RoleAccessData.kt`
Motivo:
parecían candidatos por estar fuera del archivo principal, pero sí siguen referenciados por `AdminApp.kt` y tests.
Riesgo de borrado:
alto.

## 4. Archivos eliminados

### Eliminados versionados

- `design/` completo: `246` archivos versionados.
- `.kotlin/errors/errors-*.log`: `5` archivos.
- `app/src/main/res/drawable/icon_512.png`
- `app/src/main/res/drawable/icon_512_source.png`
- `app/src/main/res/drawable/logopedilo.png`

Total versionado eliminado:

- `254` archivos.

### Eliminados locales ignorados

Se eliminaron además estos grupos del workspace:

- `.gradle/`
- `.kotlin/` restante
- `app/build/`
- `functions/node_modules/`
- `tools/node_modules/`
- `32` carpetas de capturas/evidencia visual dentro de `reports/`
- `1` log de auditoría visual en `reports/admin-visual-audit-logcat-filtered.txt`

## 5. Archivos conservados por duda

- `reports/build-driver-operational-flow-v1.md`
- `reports/cierre-admin-ui-operational-actions.md`
- `reports/cierre-backend-functions-operativo-v1.md`
- `reports/cierre-pedido-vivo-universal-core-v1.md`
- `reports/cierre-store-operational-flow-v1.md`
- `reports/validate-end-to-end-live-order-flow-v1.md`
- `reports/auditoria-profunda-estado-real-app.md`

Razón:

- son `.md` de contexto reciente y alineados con la evolución real del repo.

## 6. Archivos conservados porque siguen referenciados

- `app/src/main/res/drawable/ic_launcher_background.xml`
- `app/src/main/res/drawable/ic_launcher_foreground.png`
- `app/src/main/res/drawable-nodpi/pedilo_logo_mark.png`
- `tools/seed_public_catalog.js`
- `tools/verify_public_catalog.js`
- `tools/guards/check_ui_quality.sh`
- `app/src/main/java/com/pedilo/app/ui/admin/components/AdminComponents.kt`
- `app/src/main/java/com/pedilo/app/ui/admin/operation/AdminOperationNavigation.kt`
- `app/src/main/java/com/pedilo/app/ui/admin/operation/OperationData.kt`
- `app/src/main/java/com/pedilo/app/ui/admin/roles/RoleAccessData.kt`

## 7. Cambios en .gitignore

Se cambió:

- se agregó `.kotlin/`;
- se reemplazó `reports/` por:
  - `reports/*`
  - `!reports/*.md`

Motivo:

- seguir ignorando artefactos visuales y logs dentro de `reports/`;
- permitir que los reports `.md` queden visibles para git y no se pierdan.

## 8. Cambios colaterales necesarios

1. `README.md`
Se eliminó la referencia a `design/00-master/`, porque esa documentación fue removida.

2. No hubo cambios funcionales en:

- `functions/index.js`
- `firestore.rules`
- puertos, adapters, use cases activos;
- pantallas activas Público/Admin/Store/Driver.

## 9. Validaciones ejecutadas

- `node --test tests/*.test.js` -> OK, `20/20`
- `bash tools/guards/check_architecture.sh` -> OK
- `npm --prefix functions run build` -> OK
- `git diff --check` -> OK
- `grep -R "plan_" -n app/src/main 2>/dev/null || true` -> sin resultados
- `grep -R "mockup" -n app/src/main 2>/dev/null || true` -> sin resultados
- `grep -R "TODO LIMPIEZA" -n . 2>/dev/null || true` -> sin resultados

## 10. Validaciones fallidas o no ejecutadas

- `./gradlew :app:compileDebugKotlin` -> falló por entorno
- `./gradlew :app:assembleDebug` -> falló por entorno

Error observado:

`java.io.FileNotFoundException: /home/oem/.gradle/wrapper/dists/gradle-8.9-bin/.../gradle-8.9-bin.zip.lck (Read-only file system)`

Lectura de la falla:

- parece restricción del entorno de esta sesión sobre `~/.gradle`;
- no hay evidencia de que la limpieza haya roto el código Android;
- la compilación queda pendiente de reconfirmación en un entorno con escritura sobre `~/.gradle`.

## 11. Riesgos restantes

1. Los `.md` de `reports/` siguen acumulando historia técnica; no ensucian runtime, pero sí el repo si se dejan crecer sin criterio.
2. `tools/seed_public_catalog.js` y `tools/verify_public_catalog.js` siguen atados a `pizzeria-roma`; hoy no son basura, pero tampoco pertenecen al runtime.
3. La compilación Android no pudo reconfirmarse en esta sesión por restricción del entorno.
4. Persisten riesgos funcionales ya detectados antes de esta limpieza, especialmente los no vinculados a archivos muertos:
   - acople del flujo local a `pizzeria-roma`;
   - desajuste probable de dirección para Driver en pedidos locales.

## 12. Dictamen final

`A) Repo limpiado sin afectar app viva`

La limpieza fue segura y efectiva:

- se retiró material de diseño obsoleto y artefactos rastreados por accidente;
- se limpiaron caches y evidencia local;
- no se tocaron contratos vivos ni flujos operativos;
- las validaciones relevantes para lógica y arquitectura siguieron pasando.
