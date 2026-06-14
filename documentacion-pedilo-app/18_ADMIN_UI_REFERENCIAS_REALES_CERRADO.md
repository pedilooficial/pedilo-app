# Pédilo App — Admin UI por referencias reales

## Resultado

APTA.

Admin Operaciones quedó cerrado como una mesa real de trabajo: el Admin ve qué atender primero, abre un pedido real, entiende estado y próximo paso, ejecuta una acción habilitada por backend, ve resultado, ve historial y vuelve a la mesa para continuar.

## Referencias reales usadas

- Shopify Help Center, "Understanding your order statuses": https://help.shopify.com/en/manual/fulfillment/managing-orders/order-status
- Zendesk Support review 2025, TechRadar: https://www.techradar.com/reviews/zendesk-support
- Toast POS system review, TechRadar: https://www.techradar.com/reviews/toast-pos-point-of-sale

## Qué lógica de uso se tomó

- Shopify: los estados no son decoración; sirven para encontrar trabajo pendiente. Se tomó la lógica de estado + tarea pendiente + filtros para ubicar pedidos que requieren acción.
- Zendesk: una mesa operativa necesita cola priorizada, contexto rápido, asignación/estado, trazabilidad y continuidad entre casos. Se tomó la lógica de ticket operativo: ver prioridad, abrir caso, actuar, volver al flujo.
- Toast: en operación de restaurante/despacho importan pedidos pendientes, pedidos integrados, estado de preparación/entrega y visión en tiempo real. Se tomó la lógica de pedidos vivos ordenados por urgencia y acción siguiente.

## Análisis de la UI anterior

La UI anterior ya leía datos reales y ejecutaba acciones por backend, pero no funcionaba como mesa humana cerrada. El Admin entraba a una estructura de vistas y listados que todavía se sentía como mapa documental: "Hoy", "Activos", "Problemas", "Cerrados" y secciones internas antes de llegar al trabajo concreto.

Problemas detectados:

- La prioridad real no estaba convertida en cola directa de trabajo.
- El primer pedido operable no aparecía en la primera lectura de la mesa.
- El detalle tenía acciones e historial, pero el operador debía buscar demasiado para entender próximo paso y resultado.
- Después de una acción no había una continuidad suficientemente explícita para volver a la mesa y seguir.
- La sección operativa mezclaba navegación de exploración con trabajo real.

## Qué se eliminó o dejó de ser visible como camino principal

- La dependencia de abrir primero mundos o ramas para operar.
- El uso de Repartidores y Locales como módulos visibles de relleno en la home operativa.
- El patrón de pantalla que mostraba métricas pero no abría un pedido concreto.
- Mensajes ambiguos como "solo lectura" o estructura interna como explicación del flujo.

## Qué se rediseñó y reconstruyó

- `AdminOperationDeskScreen` ahora muestra "Mesa de trabajo" debajo de prioridades.
- Se agregó `AdminDeskOrderRow` y `adminWorkQueue()` para ordenar pedidos por problema, revisión, acciones habilitadas y antigüedad.
- Se agregó `AdminDeskOrderCard`, con pedido, origen, estado, razón y próximo paso.
- La mesa abre directamente un pedido real con `AdminRoute.OperationOrderDetail(returnRoute = AdminRoute.Operation, ...)`.
- `AdminOrderDetailScreen` ahora muestra en primer plano:
  - estado humano;
  - próximo paso;
  - ubicación actual;
  - resultado reciente;
  - lectura rápida;
  - acciones backend permitidas;
  - historial reciente;
  - botón "Volver a mesa" después de una acción.
- Se mantuvo la autoridad de backend: la UI no escribe Firestore; ejecuta `adminOrders.executeLive(AdminLiveOrderActionRequest(...))`.
- Se actualizó `tests/admin_visual_shell.test.js` para proteger el nuevo contrato de detalle con lectura rápida, historial reciente y continuidad.

## Flujo real probado

Fecha de validación: 2026-06-13.

Usuario: sesión existente con rol Admin dentro de la app instalada en dispositivo físico.

Dispositivo: `EH423L012409`, Redmi/klein_global, Android 16 según salida de Monkey.

Pedido usado: `#PDL-4BQDUP`.

Estado anterior visible:

- Mesa: `10 problemas · 9 sin responsable · 0 activos`.
- Cola: `Pedido #PDL-4BQDUP`, `Con problema`, prioridad `medium`, próximo paso `Acción: Abrir incidencia`.
- Detalle: `Con problema`, función `Retirar y entregar`, responsable `Admin`.
- Acciones disponibles: `3 permitidas`, versión `2`, acción visible `Abrir incidencia`.

Acción ejecutada:

- `Abrir incidencia`.
- Motivo ingresado desde UI: `Revision%20operativa%20real` (el teclado ADB ingresó `%20` literalmente, pero el backend recibió motivo no vacío y auditable).

Estado posterior visible:

- Detalle: `Resultado: Incidencia abierta.`
- Próximo paso cambió a `Resolver incidencia: Cierra la incidencia activa.`
- Historial/último movimiento: `admin abrió incidencia: Revision%20operativa%20real`.
- Continuidad: botón `Volver a mesa` llevó nuevamente a la mesa Admin.

## Cambio backend

La acción se ejecutó por el camino operativo real:

- UI: `AdminLiveOrderActionRequest(orderId, action, expectedVersion, reason)`.
- Use case/adapter: `adminOrders.executeLive(...)`.
- Callable: `operateLiveOrder`.
- Backend esperado y protegido por tests:
  - valida usuario operativo;
  - valida versión esperada;
  - valida acción permitida;
  - actualiza estado;
  - escribe evento en `orders/{orderId}/events`;
  - recalcula `nextAllowedActions`.

Evidencia visible del cambio backend:

- Antes: acción habilitada `Abrir incidencia`, versión `2`.
- Después: resultado `Incidencia abierta`, próximo paso `Resolver incidencia`, historial `admin abrió incidencia`.

## Evidencia generada

Capturas:

- `reports/admin-ui-referencias-reales/01_mesa_admin_actualizada.png`
- `reports/admin-ui-referencias-reales/02_cola_trabajo_pedido_real.png`
- `reports/admin-ui-referencias-reales/03_detalle_pedido_estado_anterior.png`
- `reports/admin-ui-referencias-reales/04_accion_habilitada_version_2.png`
- `reports/admin-ui-referencias-reales/05_resultado_incidente_abierta.png`
- `reports/admin-ui-referencias-reales/06_historial_evento_generado.png`
- `reports/admin-ui-referencias-reales/07_vuelta_a_mesa.png`

Validaciones técnicas:

- `node --test tests/admin_visual_shell.test.js tests/admin_operation_alignment.test.js tests/admin_operational_actions.test.js tests/operational_order_actions_backend.test.js` — PASS.
- `./gradlew lintDebug` — BUILD SUCCESSFUL.
- `./gradlew assembleDebug` — BUILD SUCCESSFUL.
- `adb install -r app/build/outputs/apk/debug/app-debug.apk` — Success.
- `adb shell dumpsys window | rg -n "mCurrentFocus|mFocusedApp"` — foco en `com.pedilo.app/.MainActivity`.

## Archivos modificados

- `app/src/main/java/com/pedilo/app/ui/admin/AdminApp.kt`
- `tests/admin_visual_shell.test.js`
- `documentacion-pedilo-app/18_ADMIN_UI_REFERENCIAS_REALES_CERRADO.md`

## Pendientes internos de Admin Operaciones

NINGUNO.

Hay campos del pedido real no informados (`Detalle no informado`, estado financiero no informado), pero no son pendientes internos de Admin Operaciones: la UI los traduce humanamente, no rompe el flujo, permite actuar, muestra resultado y registra historial.

## Dictamen final

APTA.

Admin Operaciones quedó cerrado como producto real para el flujo validado: entrar, priorizar, abrir pedido, entender estado, ejecutar acción, ver cambio, revisar historial y volver a la mesa.
