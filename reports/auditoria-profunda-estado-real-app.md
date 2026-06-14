# PÉDILO - Auditoría profunda del estado real de la app

Fecha de auditoría: `2026-06-08`
Workspace auditado: repo local en `main`
HEAD auditado: `462b56f` - `Validate end-to-end live order flow V1`

## 1. Resumen ejecutivo

El estado real del repo es más avanzado que lo que declara el `README.md`.

Hoy sí existe una app Android conectada a Firebase para:

- catálogo público en lectura;
- creación de pedidos públicos por Cloud Functions;
- tracking público por callable;
- login interno por Firebase Auth + `/users/{uid}`;
- operación real de `Admin`, `Store` y `Driver` sobre pedidos vivos V1.

También existe una separación arquitectónica reconocible entre:

- `core/model`
- `core/port`
- `core/usecase`
- `core/firebase`
- `ui/...`

Pero antes de pensar en producción siguen presentes riesgos importantes:

- el `README.md` quedó desactualizado y contradice el código;
- el flujo público de `Local` sigue acoplado a una tienda fija `pizzeria-roma`;
- `Admin` mezcla una mesa operativa real con grandes bloques de configuración/roles que siguen siendo sólo visuales;
- hay validaciones y contratos muy cubiertos por tests de lectura de código, pero casi nada de integración real contra Firebase;
- `functions/package.json` tiene un script `test` roto;
- la vista de `Driver` lee dirección desde `delivery`, mientras los pedidos locales nacen con dirección en `customer.address`.

## 2. Fuentes de verdad usadas

Se auditó contra:

1. workspace local actual;
2. `git status --short`;
3. `git log --oneline --decorate -n 30`;
4. código en `app/`, `functions/`, `tests/`, `tools/guards/`;
5. `README.md`;
6. reports `.md` y artefactos de `reports/`;
7. validaciones ejecutadas en esta sesión.

No se tomó ningún report previo como verdad si el código no lo confirmaba.

## 3. Validaciones ejecutadas en esta auditoría

### Ejecutadas y confirmadas

- `node --test tests/*.test.js` -> `20/20` OK
- `bash tools/guards/check_architecture.sh` -> OK
- `npm --prefix functions run build` -> OK (`node --check index.js`)

### Ejecutadas y fallidas

- `npm --prefix functions test` -> falla porque el script apunta a `node --test test` y no existe `functions/test`

### No verificables en esta sesión por restricción del entorno

- `./gradlew :app:compileDebugKotlin`

Resultado observado:

- Gradle falla antes de compilar por intentar escribir un lock en `~/.gradle/.../gradle-8.9-bin.zip.lck` sobre filesystem de solo lectura.
- Esto impide confirmar compilación Android en esta sesión.
- No es prueba concluyente de error del código; sí es una verificación pendiente.

### Cobertura real de tests

- No hay `app/src/test/` ni `app/src/androidTest/`.
- La suite activa está toda en `tests/*.test.js`.
- Gran parte de esa suite valida contrato y arquitectura leyendo archivos fuente o ejecutando helpers del backend en memoria.
- No hay evidencia en este repo de tests instrumentados Android ni de tests de integración con emuladores Firebase.

## 4. Estado git y consistencia temporal

### Estado del workspace

- `git status --short` limpio al momento de auditar.

### Commits recientes relevantes

Secuencia real reciente:

- `7a4b747` - `Build Pedido Vivo Universal core V1`
- `0992789` - `Build operational order action backend V1`
- `26a1872` - `Connect Admin UI to operational order actions`
- `ded4e71` - `Build Store operational flow V1`
- `9e65de6` - `Build Driver operational flow V1`
- `462b56f` - `Validate end-to-end live order flow V1`

### Consistencia commits <-> reports

Los reports principales de cierre sí están alineados con esa secuencia:

- `reports/cierre-pedido-vivo-universal-core-v1.md`
- `reports/cierre-backend-functions-operativo-v1.md`
- `reports/cierre-admin-ui-operational-actions.md`
- `reports/cierre-store-operational-flow-v1.md`
- `reports/build-driver-operational-flow-v1.md`
- `reports/validate-end-to-end-live-order-flow-v1.md`

Conclusión:

- los reports de cierre recientes son consistentes entre sí y con los commits;
- el documento que quedó realmente atrasado es `README.md`.

## 5. Arquitectura real del repo

## Android

- app principal Compose en `app/src/main/java/com/pedilo/app/...`
- actividad única en `app/src/main/AndroidManifest.xml`
- `MainActivity` carga `PublicApp()`
- el acceso interno se monta dentro de la misma app pública por ruteo

## Backend

- Cloud Functions en `functions/index.js`
- despliegue configurado en `firebase.json`

## Seguridad y datos

- reglas en `firestore.rules`
- índices en `firestore.indexes.json`

## Tests

- suite JS en `tests/`
- guard de arquitectura en `tools/guards/check_architecture.sh`

## 6. Estado funcional por superficie

### 6.1 Usuario público

Estado: implementado, con un recorte importante en `Local`.

Confirmado por:

- `app/src/main/java/com/pedilo/app/ui/publicuser/PublicApp.kt`
- `PublicHome.kt`
- `PublicShop.kt`
- `PublicShopSearch.kt`
- `PublicShopSubcategory.kt`
- `PublicPlus.kt`
- `PublicLocal.kt`
- `PublicConventions.kt`
- `PublicShopTracking.kt`

Lo real hoy:

- Home, Tienda, búsqueda, subcategorías, convenciones y Botón + existen en Compose.
- El catálogo se carga desde Firestore mediante `FirebasePublicCatalogAdapter`.
- No hay escritura pública directa a `/orders`.
- El tracking público usa callable y no lecturas directas de pedidos.

Riesgo importante:

- el mundo `Local` no es genérico.
- `PublicLocal.kt` toma productos de `productsByStore["pizzeria-roma"]`.
- `PublicApp.kt` confirma pedidos usando `romaStoreForOrder()`.
- Esto significa que la UI pública puede mostrar catálogo amplio, pero el flujo operativo de pedido local sigue acoplado a una tienda fija.

Impacto:

- si existen varios locales visibles en Firestore, la experiencia de compra local no está resuelta de forma general;
- el pedido local real queda orientado a una sola tienda.

### 6.2 Creación de pedidos públicos

Estado: implementado.

Confirmado por:

- `functions/index.js`
- `app/src/main/java/com/pedilo/app/core/firebase/FirebasePublicOrderAdapter.kt`
- `app/src/main/java/com/pedilo/app/core/firebase/FirebasePublicPlusOrderAdapter.kt`
- `CreatePublicOrderUseCase.kt`
- `CreatePublicPlusOrderUseCase.kt`

Functions exportadas y reales:

- `createLocalOrder`
- `createPlusOrder`
- `getPublicOrderTracking`
- `adminOrderAction`
- `operateLiveOrder`

Flujo real:

1. Android arma draft local o plus.
2. El use case valida.
3. El adapter llama callable.
4. La Function valida y escribe `/orders/{id}` con evento inicial.
5. Devuelve ticket público.

Punto fuerte:

- no hay escritura Android directa sobre `/orders`;
- el nacimiento del pedido queda centralizado en backend.

Observación:

- el repo muestra idempotencia por hash determinístico de payload;
- eso ayuda a evitar duplicados, pero puede colisionar funcionalmente si el usuario reintenta con exactamente el mismo payload esperando un pedido nuevo.

### 6.3 Tracking público

Estado: implementado.

Confirmado por:

- `FirebasePublicTrackingAdapter.kt`
- `GetPublicTrackingUseCase.kt`
- `functions/index.js`

Lo real hoy:

- consulta por `trackingNumber` o `publicOrderNumber`;
- devuelve sólo campos públicos;
- reconoce estados del ciclo vivo;
- en estado terminal devuelve cierre sin exponer detalle interno.

Observación:

- el tracking depende completamente de la consistencia del documento `/orders`.
- no existe una colección pública separada de tracking.

### 6.4 Backend / Cloud Functions

Estado: implementado para V1 operativo.

Confirmado por `functions/index.js`.

Hoy existen dos capas de mutación:

1. Pública:
   - `createLocalOrder`
   - `createPlusOrder`
2. Operativa:
   - `adminOrderAction`
   - `operateLiveOrder`

`operateLiveOrder` hoy sí es la autoridad operativa real para:

- `local_accept`
- `local_reject`
- `local_mark_preparing`
- `local_mark_ready`
- `driver_take`
- `driver_mark_picked_up`
- `driver_mark_delivered`
- `cancel_order`
- `open_incident`
- `resolve_incident`
- `admin_intervene`

Fortalezas reales:

- validación de actor por rol;
- validación de `expectedVersion`;
- idempotencia por `actionId`;
- escritura transaccional;
- auditoría en `/orders/{id}/events/{actionId}`;
- incidencias en `/orders/{id}/incidents/{actionId}`;
- cálculo de `nextAllowedActions`.

Puntos parciales:

- `timeoutPolicy` y `fallbackPolicy` están declarados al nacer el pedido, pero hoy son declarativos; no hay motor visible que ejecute timeouts reales.
- `adminOrderAction` sigue existiendo y tiene adapter/usecase, pero no hay evidencia de wiring activo desde la UI actual para ese camino legacy de acciones Admin específicas.

Conclusión:

- `operateLiveOrder` está conectado;
- `adminOrderAction` está declarado y testeado, pero parece quedar como carril paralelo no prioritario en la UI actual.

### 6.5 Firestore rules

Estado: implementadas y cerradas para cliente.

Confirmado por `firestore.rules`.

Lo real hoy:

- `/orders` rechaza `create/update/delete` desde cliente;
- `/events` e `/incidents` también rechazan escrituras cliente;
- `/stores` y `/products` sólo exponen catálogo visible;
- `/users/{uid}` puede ser leído por el propio usuario autenticado o Admin;
- Store lee pedidos donde `order.storeId == request.auth.uid`;
- Driver lee pedidos asignados a su uid o pedidos listos para driver sin actor asignado;
- Admin lee todo pedido operativo.

Fortaleza:

- las rules están alineadas con la idea “cliente no muta pedidos”.

Riesgo de modelo:

- la relación `storeId == auth.uid` y parte de la visibilidad driver dependen de que el modelo operativo use IDs exactos de usuario como clave de negocio.
- Si más adelante se separa “tienda comercial” de “usuario operador”, hay deuda de modelo.

### 6.6 Core Android

Estado: implementado.

Confirmado por:

- `core/model`
- `core/port`
- `core/usecase`
- `core/result`
- `core/runtime`
- `core/firebase`

La arquitectura está viva, no sólo declarada:

- hay puertos reales;
- use cases reales;
- adapters Firebase reales;
- runtimes que instancian adapters concretos;
- guards que controlan acoplamientos básicos.

Fortaleza:

- el core puro no depende de Compose ni Firebase.

Límite:

- no hay inyección de dependencias más allá de factories/runtimes simples;
- está bien para V1, pero el crecimiento puede volver pesado el mantenimiento.

### 6.7 Admin

Estado: mixto.

#### Parte operativa de pedidos

Estado: implementada.

Confirmado por:

- `app/src/main/java/com/pedilo/app/ui/admin/AdminApp.kt`
- `FirebaseAdminOrdersAdapter.kt`
- `GetAdminOperationOrdersUseCase.kt`

Lo real hoy:

- Admin observa pedidos;
- abre detalle;
- lee historial/eventos;
- ejecuta acciones vivas usando `executeLive(...)` -> `operateLiveOrder`;
- muestra `nextAllowedActions` del backend;
- refresca detalle post acción.

Esto contradice directamente al README.

#### Parte configuración / alta de roles

Estado: visual solamente.

Confirmado por textos explícitos dentro de `AdminApp.kt`:

- “No se aplicaron cambios reales.”
- “solo visual”
- “sin aplicarlo”
- “El botón de confirmar es solo visual en este bloque.”

Conclusión Admin:

- operación de pedidos: real;
- configuración y alta de roles: shell visual extensa, no funcional como panel de administración persistente.

Riesgo adicional:

- `AdminApp.kt` supera `3000` líneas.
- Es un archivo muy grande y mezcla operación, navegación, visuales y mundos aún no conectados.

### 6.8 Store / Local interno

Estado: implementado para operación básica V1.

Confirmado por:

- `StoreApp.kt`
- `FirebaseStoreOrdersAdapter.kt`
- `StoreOrdersPort.kt`
- `GetStoreOrdersUseCase.kt`

Lo real hoy:

- login con rol `store`;
- lectura de pedidos propios por `storeId == auth.uid`;
- apertura de detalle;
- ejecución de acciones permitidas por backend;
- sin escritura directa a Firestore desde cliente.

Acciones reales visibles cuando backend las habilita:

- aceptar;
- rechazar;
- marcar en preparación;
- marcar listo;
- reportar incidencia.

Limitación:

- el modelo sigue atado a `storeId == uid`.

### 6.9 Driver / Repartidor

Estado: implementado para operación básica V1.

Confirmado por:

- `DriverApp.kt`
- `FirebaseDriverOrdersAdapter.kt`
- `DriverOrdersPort.kt`
- `GetDriverOrdersUseCase.kt`

Lo real hoy:

- login con rol `driver`;
- lectura de pedidos disponibles y asignados;
- detalle;
- ejecución por `operateLiveOrder`.

Acciones reales:

- tomar pedido;
- marcar retirado;
- marcar entregado;
- abrir incidencia;
- cancelar cuando backend lo habilita.

Riesgo concreto:

- `FirebaseDriverOrdersAdapter.deliveryAddress()` lee desde un bloque `delivery`.
- `createLocalOrder` guarda la dirección en `customer.address`.
- Resultado probable: para pedidos locales, la UI del driver puede no ver la dirección de entrega aunque el pedido la tenga.

Este es un desajuste real entre contrato de escritura y lectura.

### 6.10 Login interno y roles

Estado: implementado.

Confirmado por:

- `FirebaseTeamAccessAdapter.kt`
- `TeamSessionStore.kt`
- `PublicApp.kt`

Lo real hoy:

- usa Firebase Auth por email/password;
- luego resuelve perfil en `/users/{uid}`;
- exige `active == true`;
- mapea roles `admin`, `store/local`, `driver/repartidor`;
- permite persistir sesión local.

Observación:

- si `keepSignedIn` es `false`, el adapter hace `auth.signOut()` y persiste sólo la sesión local serializada.
- La navegación sigue funcionando, pero la operación interna real después dependerá de cómo reaccionen los adapters que usan `Firebase.auth.currentUser`.
- Es una zona a revisar con prueba manual real, porque el modelo de “sesión visual local pero signOut inmediato” puede romper lecturas/mutaciones después del login.

### 6.11 Adapters / ports / use cases

Estado: implementados y usados.

Confirmado por:

- `core/runtime/PublicRuntime.kt`
- `AdminRuntime.kt`
- `StoreRuntime.kt`
- `DriverRuntime.kt`

Conclusión:

- no es una arquitectura decorativa;
- las capas sí están conectadas al runtime real de la app.

### 6.12 Tests

Estado: abundantes, pero mayormente de contrato estático.

Fortalezas:

- cubren arquitectura;
- cubren reglas;
- cubren wiring;
- cubren vocabulario operativo;
- cubren flujo de nacimiento y ciclo vivo en helpers del backend.

Limitaciones:

- no prueban Firebase real;
- no prueban UI Android instrumentada;
- no prueban despliegue ni índices en producción;
- no prueban que la app compile en esta sesión;
- no prueban navegación Compose con interacción real.

### 6.13 Reports

Estado: útiles como bitácora, pero no todos equivalen a verificación reproducida hoy.

Lo positivo:

- los reports recientes reflejan bastante bien la evolución de commits;
- varios riesgos residuales ya estaban bien anotados.

Cuidado:

- varios reports dicen haber corrido `./gradlew :app:compileDebugKotlin` y `assembleDebug`;
- en esta auditoría no se pudo reproducir por limitación del entorno;
- eso no invalida el historial, pero sí impide reconfirmarlo desde esta sesión.

### 6.14 README

Estado: desactualizado y materialmente inconsistente con el repo actual.

Contradicciones confirmadas:

- dice que Admin es “shell visual read-only”;
- hoy Admin sí ejecuta acciones vivas sobre pedidos.

- dice que Store / Driver son placeholders;
- hoy ambos tienen apps operativas reales.

- dice que Pedido Vivo Universal no está implementado;
- hoy sí existe contrato de nacimiento vivo, `version`, `nextAllowedActions`, eventos y `operateLiveOrder`.

Conclusión:

- el README no puede usarse hoy como resumen confiable del estado real de la app.

## 7. Qué está implementado, parcial, faltante o sólo declarado

### Implementado y conectado

- catálogo público Firestore read-only;
- Home / Shop / Search / Subcategory / Conventions / Plus / Local públicos;
- creación pública de pedidos por Functions;
- tracking público por callable;
- login interno por Firebase Auth + `/users`;
- Admin operativo sobre pedidos;
- Store operativo V1;
- Driver operativo V1;
- reglas Firestore cerradas a escritura cliente;
- auditoría por eventos e incidencias;
- `nextAllowedActions` y control de versión.

### Parcial

- `Local` público funciona, pero no como experiencia multi-store real;
- tracking público existe, pero depende de un único documento `orders` y no de un subsistema separado;
- `timeoutPolicy` / `fallbackPolicy` existen sólo a nivel declarativo;
- modelo de roles funciona, pero está acoplado a IDs de usuario como IDs de negocio.

### Declarado pero no funcional

- configuración Admin persistente;
- alta de roles desde Admin;
- editor de mundos públicos y convergencias Admin;
- parte del carril `adminOrderAction` desde UI actual.

### Faltante o no confirmado

- tests de integración Firebase emulator;
- tests instrumentados Android;
- confirmación de compilación Android en esta sesión;
- confirmación de deploy Functions;
- confirmación de operación con datos reales de producción;
- un modelo general de tiendas públicas para `Local`.

## 8. Riesgos reales antes de producción

### Riesgo 1 - README engañoso

Severidad: alta.

El documento principal del repo describe un estado anterior. Puede inducir decisiones equivocadas, auditorías incompletas o QA mal orientado.

### Riesgo 2 - Flujo `Local` público acoplado a `pizzeria-roma`

Severidad: alta.

La app pública parece más general de lo que realmente es. El pedido local no nace desde una selección de tienda genérica sino desde una tienda fija.

### Riesgo 3 - Dirección posiblemente invisible para Driver en pedidos locales

Severidad: alta.

El backend de `createLocalOrder` guarda `customer.address`, pero el driver lee `delivery.addressLine/locality`. Eso puede afectar entregas reales.

### Riesgo 4 - Configuración y alta de roles aparentan más de lo que hacen

Severidad: media-alta.

Hay mucho shell visual Admin que parece panel funcional, pero no aplica cambios reales.

### Riesgo 5 - Acoplamiento `storeId == auth.uid`

Severidad: media-alta.

Limita el modelo operacional y complica separar entidad comercial de operador humano.

### Riesgo 6 - Verificación real insuficiente contra Firebase

Severidad: alta.

Los tests actuales son sólidos para contrato, pero débiles para integración real. Antes de producción falta validar:

- rules con emulator;
- callables con auth real;
- queries reales con índices;
- navegación interna operando con usuarios de prueba;
- compilación y ejecución Android reproducibles.

### Riesgo 7 - `functions/package.json` tiene script `test` roto

Severidad: media.

Es una señal de tooling incompleto y puede romper pipelines o dar falsa sensación de cobertura.

### Riesgo 8 - `keepSignedIn = false` merece prueba manual real

Severidad: media.

El adapter firma sesión, construye `TeamSession`, y luego puede hacer `auth.signOut()`. La navegación visual queda, pero hay riesgo de que los adapters operativos no tengan usuario autenticado al empezar a leer/mutar.

### Riesgo 9 - Archivo `AdminApp.kt` demasiado grande

Severidad: media.

Más de 3000 líneas en un solo archivo elevan riesgo de regresión y dificultad de mantenimiento.

## 9. Conclusión final

La app Pédilo, en su estado real local auditado hoy, no está “vacía” ni en fase meramente visual.

El repo ya contiene:

- producto público funcional;
- backend operativo V1 para pedidos vivos;
- operación real de Admin, Store y Driver;
- reglas cerradas;
- una base arquitectónica razonable;
- buena cobertura de contratos.

Pero todavía no está en un punto cómodo de producción sin una etapa de consolidación porque conviven:

- features reales;
- documentación principal desactualizada;
- shells Admin no funcionales mezclados con operación real;
- un flujo `Local` demasiado específico;
- verificación de integración todavía insuficiente;
- un desajuste probable entre datos de entrega locales y lectura del driver.

## 10. Dictamen de auditoría

Dictamen: `operativa en V1, pero no lista para asumir producción sin una fase previa de alineación documental, validación de integración y cierre de riesgos funcionales concretos`.

Prioridades reales antes de seguir:

1. alinear `README.md` con el código;
2. decidir si `Local` seguirá mono-tienda o pasará a multi-store real;
3. corregir contrato de dirección para Driver;
4. definir qué partes de Admin son reales y cuáles seguirán visuales;
5. agregar validación con Firebase real o emulator;
6. revalidar compilación Android en un entorno con Gradle escribible.
