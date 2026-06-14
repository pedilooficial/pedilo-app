# Producto, Roles y Reglas

Este documento define el comportamiento funcional de Pédilo App por rol y modulo.

## Usuario Publico

El Usuario Publico crea pedidos mediante formularios validados, consulta tracking seguro, cancela cuando el estado lo permite e inicia reclamos posteriores sin reabrir pedidos cerrados.

Reglas:

- Los formularios deben validar datos obligatorios antes de confirmar.
- El tracking publico muestra solo informacion segura para el usuario.
- La cancelacion publica debe ejecutarse por backend.
- El reclamo posterior se vincula al pedido y no cambia su estado cerrado.
- El usuario no ve eventos internos, auditoria ni informacion operativa reservada.

No permitido:

- confirmar pedidos incompletos;
- mostrar datos demo o placeholders como datos reales;
- exponer auditoria interna;
- cancelar sin evento y validacion de estado.

## Local / Store

El Local opera solamente pedidos de su `storeId`. Puede aceptar, rechazar, preparar y marcar listo segun las transiciones permitidas. Tambien gestiona catalogo, productos, variantes y stock cuando la etapa correspondiente este implementada.

Reglas:

- Cada accion del Local valida pertenencia por `storeId`.
- El carrito es mono-local.
- Un pedido no pasa a reparto antes de aceptarse y quedar listo cuando corresponda.
- `store_driver_request` forma parte del objetivo del producto y se implementa luego del nucleo backend y el modulo Store.

No permitido:

- operar pedidos ajenos;
- avanzar estados fuera de orden;
- resolver finanzas completas dentro del modulo Store;
- convertir catalogo pasivo en autoridad del estado operativo.

## Repartidor / Driver

El Repartidor toma pedidos disponibles, retira, entrega, cobra cuando la etapa financiera lo habilite y cierra caja segun las reglas financieras.

Reglas:

- La toma de pedido debe ser atomica y validada por backend.
- No puede haber doble toma.
- La entrega requiere retiro previo.
- La capacidad del repartidor debe respetarse cuando el modulo la implemente.
- Los cobros y cierres pertenecen al eje financiero.

No permitido:

- entregar sin retiro;
- tomar sobre capacidad;
- aprobar pagos dudosos;
- incorporar GPS o mapas como alcance base.

## Admin

Admin supervisa la operacion, interviene pedidos, gestiona configuracion, administra usuarios y roles, audita eventos y resuelve casos.

Reglas:

- Toda intervencion sensible queda auditada.
- La configuracion debe persistirse con version.
- Los pedidos vivos conservan snapshot de configuracion y tarifas.
- La administracion de roles se hace sobre usuarios reales y con trazabilidad.
- El tablero Admin debe diferenciar informacion operativa, configuracion, salud y auditoria.

No permitido:

- tratar un shell visual como modulo real;
- cambiar pedidos vivos desde configuracion sin intervencion auditada;
- crear roles sin trazabilidad;
- ocultar acciones sensibles sin evento.

## Pagos, tarifas y finanzas

El eje financiero es independiente del eje operativo. Un pedido entregado no queda automaticamente cerrado financieramente.

Reglas:

- Las tarifas se congelan en snapshot al confirmar el pedido.
- Los cambios de tarifa o modo operativo afectan pedidos futuros.
- El pago dudoso requiere resolucion; no cierra solo.
- Cobro en entrega y cierre de caja pertenecen al modulo financiero.
- La pasarela online no forma parte del alcance base.

No permitido:

- cambiar precios retroactivamente;
- cerrar financieramente un pedido disputado;
- aprobar pagos dudosos con IA;
- mezclar cierre operativo con cierre financiero.

## Incidencias, reclamos y cancelaciones

Una incidencia ocurre durante el pedido vivo. Un reclamo posterior ocurre despues del cierre y queda vinculado al pedido. Una cancelacion depende de rol, estado e impacto financiero.

Reglas:

- Toda cancelacion registra evento.
- Toda incidencia tiene apertura, responsable y resolucion.
- Producto no disponible requiere flujo entre Local, Admin y Cliente.
- El impacto financiero de una cancelacion debe definirse cuando corresponda.

No permitido:

- reabrir pedidos cerrados;
- cancelar sin evento;
- resolver reclamos cambiando el cierre del pedido;
- omitir impacto financiero cuando exista.

## Comunicacion

WhatsApp, chat, notificaciones y mensajes publicos comunican informacion del pedido. No cambian estados por si mismos.

Reglas:

- Debe existir fallback si WhatsApp falla.
- Los mensajes publicos deben ser seguros y adecuados al estado del pedido.
- El chat interno por pedido respeta permisos por rol.
- Las colas de comunicacion registran estado de envio sin autoridad operativa.

No permitido:

- usar WhatsApp como autoridad del pedido;
- cambiar estados desde texto de chat;
- abrir chat publico sin definicion de permisos y alcance.

## IA controlada

La IA puede asistir, estructurar datos, senalar riesgos y derivar casos. Las acciones sensibles requieren confirmacion humana o backend autorizado.

Reglas:

- La IA puede sugerir, no decidir.
- Las recomendaciones deben dejar claro el actor que confirma.
- Pagos dudosos, cancelaciones, asignaciones y cierres requieren validacion externa a la IA.

No permitido:

- que la IA cambie estados sola;
- que cancele, asigne, cierre o apruebe pagos dudosos sin autorizacion;
- que reemplace auditoria, Rules o Functions.

## Metricas, auditoria y salud

Las metricas de Pédilo derivan de eventos. Admin debe poder ver salud del sistema, pedidos trabados, alertas y estados relevantes.

Reglas:

- Los eventos son la base de auditoria.
- Los agregados deben trazarse a eventos.
- La salud del sistema se expone a Admin, no al publico.

No permitido:

- inventar metricas desde UI;
- exponer metricas internas al usuario publico;
- borrar o editar eventos desde cliente.
