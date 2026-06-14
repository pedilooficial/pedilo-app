# Pedido Vivo Universal

El Pedido Vivo Universal es la entidad central de Pédilo App. Cada pedido concentra estado, responsables, snapshots, eventos y vistas por rol.

## Regla de autoridad

- El pedido vive en backend.
- La UI representa informacion y solicita acciones.
- Cloud Functions validan cambios de estado.
- Firestore persiste pedidos, eventos y datos vinculados.
- Security Rules bloquean accesos no permitidos.

## Entidad base

Entidad principal:

- `/orders/{id}`

Entidades vinculadas:

- `/orders/{id}/events`
- `/orders/{id}/incidents`
- reclamos posteriores vinculados al pedido
- datos financieros y cierres cuando exista el modulo financiero
- comunicaciones asociadas al pedido cuando exista el modulo de comunicacion

Campos conceptuales minimos:

- `orderType`
- `status`
- `operationalStatus`
- `financialStatus`
- `communicationStatus`
- `incidentStatus`
- `archiveStatus`
- `publicStatus`
- `storeId`
- `driverId`
- `version`
- snapshots de datos relevantes al confirmar

## Ejes de estado

| Eje | Funcion |
|-----|---------|
| Operativo | Ubicacion real del pedido dentro del flujo. |
| Financiero | Cobro, disputa, deuda, cierre y caja. |
| Comunicacion | Estado de mensajes y canales sin autoridad operativa. |
| Incidencia | Excepciones activas o resueltas. |
| Archivo | Vida, cierre, archivo y consulta posterior. |

Los nombres conceptuales pueden estar en espanol y los wire names en ingles. La persistencia existente no se renombra sin migracion.

## Transiciones operativas base

| Accion | Rol | Resultado conceptual |
|--------|-----|----------------------|
| Crear pedido | Sistema / Publico | Pedido recibido o en validacion inicial. |
| Aceptar | Local | Pedido aceptado por local. |
| Rechazar | Local | Pedido cerrado o cancelado segun regla aplicable. |
| Marcar preparacion | Local | Pedido en preparacion. |
| Marcar listo | Local | Pedido listo para retiro. |
| Tomar pedido | Repartidor | Repartidor asignado. |
| Marcar retirado | Repartidor | Pedido retirado. |
| Marcar entregado | Repartidor | Pedido entregado operativamente. |
| Cancelar | Rol permitido | Pedido cancelado con efecto auditado. |
| Abrir incidencia | Rol permitido | Incidencia abierta. |
| Resolver incidencia | Admin | Incidencia resuelta. |
| Intervenir | Admin | Intervencion auditada. |

## Estados incompatibles

No puede existir:

- archivado con chat operativo activo;
- archivado con pedido en entrega;
- esperando repartidor y repartidor asignado al mismo tiempo;
- entregado sin retiro previo;
- cerrado financieramente y disputado al mismo tiempo;
- listo para retiro sin aceptacion local previa;
- en entrega sin retiro previo;
- pedido cerrado reabierto.

## Validaciones obligatorias

Cada accion que cambie estado valida:

1. usuario autenticado cuando corresponda;
2. rol activo;
3. permiso sobre el pedido;
4. transicion permitida desde el estado actual;
5. `expectedVersion`;
6. idempotencia cuando la accion pueda repetirse;
7. escritura de evento.

Errores esperados:

- `VERSION_MISMATCH`
- `INVALID_TRANSITION`
- `PERMISSION_DENIED`
- `ORDER_CLOSED`
- `failed-precondition`

## Eventos

Los eventos son inmutables para clientes y permiten reconstruir la historia operativa del pedido.

Todo evento sensible registra:

- pedido;
- accion;
- rol y actor;
- estado anterior y nuevo cuando aplique;
- timestamp de servidor;
- version;
- motivo cuando exista cancelacion, incidencia o intervencion.

## Idempotencia y concurrencia

- La creacion de pedido debe usar clave idempotente.
- Las acciones criticas usan version esperada.
- Doble toma, doble cierre o doble cancelacion se rechazan o resuelven idempotentemente.
- Los tests de concurrencia deben cubrir acciones criticas por rol.

## Timeouts y fallbacks

El pedido no puede quedar sin salida. Deben existir responsables, escaladas y mensajes publicos seguros cuando haya demora o falla.

El motor de timeouts se implementa mediante scheduler o worker. Las escaladas operativas llegan a Admin cuando el flujo no pueda resolverse automaticamente.

## Cierre

Un pedido cerrado:

- no se reabre;
- puede tener reclamo posterior vinculado;
- puede tener revision financiera si el eje financiero lo permite;
- puede archivarse cuando cierre operativo, financiero y auditoria lo permitan.
