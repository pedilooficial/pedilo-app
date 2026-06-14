# Pédilo — Corrección de riesgos post limpieza

Fecha de ejecución: 2026-06-08 19:34:33 -03

## Commit base

- `7324321` — `Actualizar fuente Pédilo - 2026-06-08 15:12:33`
- Referencia funcional declarada por el usuario: `462b56f` — `Validate end-to-end live order flow V1`

## Alcance ejecutado

Se corrigieron únicamente los riesgos pedidos:

1. dirección visible para Driver en pedidos locales;
2. `README.md` alineado al estado real actual;
3. diferenciación explícita entre Admin operativo real y Configuración / Alta de roles visual o no persistente;
4. `functions/package.json` sin script `test` roto;
5. verificación de referencias post limpieza.

No se tocaron deploy, producción, pagos, WhatsApp, mapas/GPS, diseño público ni configuración real de roles.

## Riesgos corregidos

### 1. Driver no perdía más la dirección en pedidos locales nuevos

Archivo: `functions/index.js`

- `createLocalOrder` ahora normaliza un bloque `delivery` al nacer el pedido local:
  - `delivery.addressLine = clean.customer.address`
  - `delivery.locality = ""`
- Ese bloque se guarda tanto en el snapshot vivo inicial como en el documento persistido de `/orders`.

Resultado:

- el pedido local nace con dirección legible para las capas operativas que esperan `delivery.*`;
- el flujo Store -> Driver sigue usando el mismo contrato de pedido vivo;
- no se redefines el modelo público ni el flujo validado.

### 2. Compatibilidad hacia atrás en Driver

Archivo: `app/src/main/java/com/pedilo/app/core/firebase/FirebaseDriverOrdersAdapter.kt`

- El detalle de Driver ahora prioriza:
  - `delivery.addressLine`
  - `delivery.locality`
- Si un pedido viejo no tiene bloque `delivery`, hace fallback a `customer.address`.

Resultado:

- pedidos locales históricos sin `delivery` no quedan mudos en la UI Driver;
- pedidos nuevos ya nacen con el formato esperado.

### 3. README alineado al estado real

Archivo: `README.md`

Se actualizó para reflejar el estado confirmado por código y tests:

- app pública funcional;
- catálogo Firestore en lectura;
- creación pública por Functions;
- tracking público operativo;
- login interno por Firebase Auth + `/users`;
- backend operativo V1;
- Admin operativo sobre pedidos;
- Store operativo V1;
- Driver operativo V1;
- flujo end-to-end V1 validado;
- pendientes reales antes de producción.

También quedó explícito:

- **Admin operación sobre pedidos** = real;
- **Configuración / Alta de roles** = visual, informativa o no persistente en el estado actual.

### 4. Script `test` de Functions corregido

Archivo: `functions/package.json`

Antes:

- `node --test test` apuntaba a una ruta inexistente.

Ahora:

- `cd .. && node --test tests/*.test.js`

Resultado:

- `npm --prefix functions test` ejecuta la suite real del repo;
- no se inventó una suite falsa local a `functions/`.

### 5. Verificación post limpieza

Búsquedas ejecutadas:

- `grep -R "design/" -n . 2>/dev/null || true`
- `grep -R "plan_" -n app/src/main 2>/dev/null || true`
- `grep -R "mockup" -n app/src/main 2>/dev/null || true`
- `grep -R "logopedilo" -n app/src/main 2>/dev/null || true`

Resultado:

- no quedaron referencias rotas relevantes dentro de código fuente Android;
- la única aparición de `design/` quedó en `reports/limpieza-profunda-archivos-obsoletos.md`, como registro histórico de la limpieza, no como dependencia viva del código.

## Archivos tocados

- `functions/index.js`
- `app/src/main/java/com/pedilo/app/core/firebase/FirebaseDriverOrdersAdapter.kt`
- `tests/local_order_flow.test.js`
- `tests/driver_operational_flow.test.js`
- `README.md`
- `functions/package.json`
- `reports/correccion-riesgos-post-limpieza.md`

## Tests agregados o ajustados

### `tests/local_order_flow.test.js`

Se reforzó el contrato para exigir que `createLocalOrder`:

- derive `delivery` desde `clean.customer.address`;
- lo incluya en el snapshot vivo inicial;
- lo persista junto al pedido.

### `tests/driver_operational_flow.test.js`

Se reforzó el contrato para exigir que Driver:

- siga leyendo `delivery.addressLine`;
- use fallback a `customer.address` cuando el bloque `delivery` no exista.

## Validaciones ejecutadas

### Pasaron

- `node --test tests/*.test.js`
- `npm --prefix functions test`
- `bash tools/guards/check_architecture.sh`
- `npm --prefix functions run build`
- `./gradlew :app:compileDebugKotlin`  
  Ejecutado con fallback local por entorno restringido:  
  `GRADLE_USER_HOME="$PWD/.gradle-tmp" ./gradlew --no-daemon --console=plain :app:compileDebugKotlin`
- `./gradlew :app:assembleDebug`  
  Ejecutado con fallback local por entorno restringido:  
  `GRADLE_USER_HOME="$PWD/.gradle-tmp" ./gradlew --no-daemon --console=plain :app:assembleDebug`
- `git diff --check`

### Incidencias de validación resueltas durante el trabajo

1. `npm --prefix functions test`
   - primer intento falló porque el script corregido aún corría desde `functions/` y los tests resolvían paths relativos a la raíz;
   - se corrigió a `cd .. && node --test tests/*.test.js`;
   - reejecución: OK.

2. `./gradlew :app:compileDebugKotlin`
   - primer intento falló por `~/.gradle` en solo lectura;
   - segundo intento en sandbox falló por descarga bloqueada del wrapper;
   - se relanzó fuera de sandbox con `GRADLE_USER_HOME` local;
   - luego apareció un error real de compilación (`Unresolved reference 'ADDRESS'`) en `FirebaseDriverOrdersAdapter.kt`;
   - se corrigió agregando la constante `ADDRESS = "address"`;
   - reejecución final: OK.

3. `./gradlew :app:assembleDebug`
   - ejecutado luego de corregir compilación;
   - resultado final: OK.

## Validaciones no ejecutadas

- Ninguna dentro del alcance pedido.

## Riesgos restantes

1. Los pedidos locales nuevos ya nacen con `delivery`, pero pedidos históricos sin ese bloque dependen del fallback de Driver a `customer.address`.
2. La sección Admin de Configuración / Alta de roles sigue siendo visual o no persistente; quedó documentado, no implementado.
3. Siguen pendientes, fuera del alcance de esta corrección:
   - validación contra Firebase real o emuladores según entorno objetivo;
   - hardening adicional;
   - release productiva.

## Dictamen final

**Aprobado dentro del alcance pedido.**

Quedaron corregidos los cinco riesgos concretos post limpieza sin reabrir arquitectura, sin redefinir la app y sin tocar deploy/producción. El flujo V1 sigue validado, Driver recupera dirección para pedidos locales, `README.md` ya refleja el estado real y `functions/package.json` deja de fallar por un script roto.
