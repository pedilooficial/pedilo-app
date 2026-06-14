# Arquitectura Tecnica

Pédilo App se construye con una arquitectura donde backend, datos y seguridad gobiernan el pedido; Android representa vistas por rol y solicita acciones.

## Capas

| Capa | Responsabilidad |
|------|-----------------|
| Android UI | Vistas por rol, formularios, tracking y acciones de usuario. |
| Core Android | Modelos, contratos, adapters y validaciones locales no autoritativas. |
| Cloud Functions | Cambios de estado, permisos, idempotencia, eventos y acciones de negocio. |
| Firestore | Persistencia de pedidos, usuarios, locales, configuracion, eventos y datos vinculados. |
| Security Rules | Lectura y escritura permitida por rol, alcance y coleccion. |
| Tests y guards | Prevencion de regresiones, mezcla de roles y escrituras inseguras. |

## Firestore

Estructuras esperadas:

- `/orders/{id}`
- `/orders/{id}/events`
- `/orders/{id}/incidents`
- `/claims/{id}` o equivalente vinculado al pedido
- `/users/{uid}`
- `/stores/{id}`
- `/drivers/{id}` o datos de capacidad por usuario/rol
- `/config/{id}` para tarifas, modos, capacidades y versiones
- `/finance/*` o subcolecciones de cobros y cierres
- `/communications/*` para colas y estados de mensajes
- `/metrics/*` para agregados derivados de eventos

No se crean colecciones nuevas sin Rules y tests.

## Cloud Functions

Funciones de nucleo:

- crear pedido con contrato de nacimiento;
- operar Pedido Vivo por accion;
- calcular acciones permitidas;
- registrar eventos;
- validar rol, estado, version y permiso.

Funciones por modulo:

- cancelacion publica;
- configuracion Admin y roles;
- acciones Store;
- acciones Driver;
- timeouts y fallbacks;
- finanzas, cobros y cierres;
- incidencias y reclamos;
- comunicacion;
- IA asistiva;
- metricas desde eventos.

## Security Rules

Reglas obligatorias:

- el cliente no escribe directo en `/orders`;
- el usuario publico lee solo tracking seguro;
- Store lee y opera solo pedidos propios;
- Driver lee y opera solo pedidos permitidos;
- Admin lee y opera segun rol;
- los eventos no se borran ni editan desde cliente;
- configuracion, usuarios y finanzas quedan protegidos.

## Android

Regla superior: Android no decide estados.

Vistas por rol:

- Publico: formularios, tracking, cancelacion permitida y reclamos.
- Admin: tablero operativo, configuracion, roles, intervencion y salud.
- Store: pedidos propios, catalogo, preparacion, listo y solicitud de repartidor.
- Driver: disponibilidad, toma, retiro, entrega, cobro y cierre cuando exista el modulo financiero.

No permitido:

- pantallas con datos demo como reales;
- un rol accionando como otro;
- auditoria interna visible al publico;
- acciones locales que salteen Functions.

## Tests minimos

| Area | Cobertura requerida |
|------|---------------------|
| Pedido Vivo | contrato de nacimiento, acciones backend, eventos, version, idempotencia y concurrencia. |
| Seguridad | Rules, permisos por rol, escritura bloqueada en `/orders`, eventos inmutables. |
| Publico | formularios, tracking seguro, cancelacion permitida, ausencia de datos internos. |
| Admin | acciones, configuracion, roles, intervenciones y auditoria. |
| Store | pertenencia por `storeId`, flujo operativo y catalogo. |
| Driver | toma atomica, retiro, entrega, capacidad y cobro cuando corresponda. |
| Finanzas | snapshots de tarifa, cobros, disputas y cierre de caja. |
| Incidencias | separacion entre incidencia viva, reclamo posterior y cancelacion. |
| Comunicacion | cola, fallback, permisos y ausencia de cambios de estado por texto. |
| IA | asistencia sin acciones sensibles autonomas. |
| Metricas | agregados trazables a eventos. |
| Release | build, limpieza de placeholders, observabilidad y checklist. |

## Compatibilidad tecnica

Cuando el codigo existente use nombres persistidos distintos de los nombres conceptuales, se conserva la persistencia y se migra solo mediante etapa documentada, tests y validacion de datos. El objetivo tecnico es converger al modelo de cinco ejes sin romper pedidos vivos.

## Areas de implementacion

Trabajo tecnico pendiente:

- cinco ejes completos del Pedido Vivo;
- motor de timeouts y fallbacks;
- `store_driver_request` auditado;
- configuracion y roles reales en Admin;
- cancelacion publica y reclamos backend;
- finanzas, cobro y cierre caja;
- comunicacion;
- IA controlada;
- metricas y salud;
- hardening, carga y release certificado.
