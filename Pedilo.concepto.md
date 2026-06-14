# PÉDILO — PLANO MAESTRO CONCEPTUAL CERRADO FINAL

**Tipo de documento:** plano maestro conceptual cerrado de la app.  
**Uso previsto:** base de producto para construir Pédilo sin redefinir reglas críticas durante el desarrollo.  
**Alcance:** app completa: Pedido Vivo Universal, usuario público, local, repartidor, Admin, sistema/backend, IA, WhatsApp, chat interno, notificaciones, pagos, envíos, tarifas, modo lluvia, saturación, cierre de caja, incidencias, auditoría, métricas, seguridad y producción intensa.  
**Carácter:** conceptual. No contiene código, nombres técnicos obligatorios, rutas, funciones, colecciones, pantallas finales ni decisiones visuales.  
**Regla de construcción:** lo que se implemente después debe respetar este documento como contrato de producto y operación.

---

## 0. Regla de lectura

Este documento no describe una versión implementada.  
Describe cómo debe comportarse Pédilo antes de escribir el desarrollo.

No debe usarse como:

- código;
- diseño visual final;
- prompt de Codex;
- copia de una app vieja;
- copia de otra empresa;
- listado de tareas sueltas;
- documento técnico de Firebase;
- improvisación de arquitectura.

Debe usarse como:

> **Plano conceptual cerrado para construir Pédilo con reglas de negocio claras, sin ambigüedades críticas y sin que el programador tenga que inventar el comportamiento del sistema.**

Cuando en etapa técnica haya que elegir nombres de estados, funciones, tablas, colecciones, clases, servicios o proveedores, esas elecciones deben respetar este contrato.

---

# 1. Identidad de Pédilo

Pédilo es un sistema de mensajería local y gestión de pedidos.

No es solamente:

- marketplace;
- delivery;
- carrito;
- catálogo;
- WhatsApp;
- panel administrativo;
- app de repartidores;
- app de locales.

Pédilo es una plataforma donde una persona puede:

- pedir productos a un local;
- pedir que Pédilo compre algo;
- solicitar un retiro;
- solicitar un envío;
- pedir un repartidor desde un comercio;
- consultar el seguimiento de un pedido;
- reportar un problema;
- hacer un reclamo posterior;
- recibir avisos;
- interactuar con el sistema por app y/o WhatsApp según corresponda.

La app conecta:

- usuario público;
- locales operativos;
- locales pasivos de catálogo;
- repartidores;
- Admin;
- backend/núcleo;
- IA;
- WhatsApp API;
- chat interno por pedido;
- notificaciones;
- pagos;
- cierre de caja;
- auditoría;
- métricas.

La unidad central de todo es:

> **El Pedido.**

---

# 2. Regla madre: todo es Pedido

En Pédilo, cada operación real debe explicarse desde un Pedido.

No hay:

- un pedido para el cliente;
- otro para el local;
- otro para el repartidor;
- otro para Admin;
- otro para WhatsApp;
- otro para la IA.

Hay un solo Pedido, con distintas lecturas según rol.

Regla central:

```text
Un solo Pedido.
Una sola verdad.
Distintas lecturas por rol.
Acciones permitidas por el núcleo.
Auditoría de lo importante.
Cierre seguro.
```

La UI no inventa estados.  
El rol no salta permisos.  
La IA no decide estados críticos.  
WhatsApp no gobierna.  
Admin puede intervenir, pero siempre dentro del sistema, con validación y auditoría.

---

# 3. Pedido Vivo Universal

## 3.1 Qué es

El Pedido Vivo Universal es la entidad que representa una operación real desde que alguien inicia una intención hasta que esa operación queda cerrada e histórica.

Un Pedido puede nacer por:

- pedido a local;
- compra directa;
- retiro/envío;
- solicitud de repartidor por local;
- intervención operativa autorizada si corresponde.

Un reclamo posterior no crea un Pedido operativo nuevo.
Un reclamo posterior crea un caso posterior vinculado a un Pedido histórico.

## 3.2 Qué conecta

El Pedido conecta:

- cliente;
- teléfono;
- local;
- repartidor;
- Admin;
- pago;
- comunicación;
- IA;
- WhatsApp;
- chat interno;
- notificaciones;
- auditoría;
- métricas;
- estados;
- responsables;
- acciones;
- incidencias;
- cierre.

## 3.3 Qué no es

El Pedido no es solo:

- una pantalla;
- una card;
- un número;
- un ticket;
- un carrito;
- un mensaje;
- una entrega;
- una tarea del repartidor;
- una orden del local;
- un chat;
- un registro de base de datos.

Es el hilo común que da coherencia al sistema.

## 3.4 Regla de vida

Mientras un Pedido está vivo, debe tener:

- identidad;
- tipo;
- estado operativo;
- responsable actual;
- actor asignado si corresponde;
- acciones permitidas;
- próxima salida posible;
- timeout o razón de no necesitarlo;
- fallback;
- auditoría mínima;
- visibilidad pública segura;
- estado financiero si aplica;
- estado de comunicación si aplica;
- estado de incidencia si aplica;
- posibilidad de cierre.

Regla dura:

```text
Ningún Pedido vivo puede quedar flotando.
```

Queda flotando si no se sabe:

- qué es;
- dónde está;
- quién debe actuar;
- qué se puede hacer;
- qué pasa si nadie actúa;
- cómo se cierra;
- qué debe ver el cliente.

---

# 4. Tipos de Pedido

Pédilo trabaja con cuatro tipos principales de Pedido.

Cada tipo comparte el mismo núcleo, pero tiene datos obligatorios, acciones y responsables propios.

---

## 4.1 Pedido de local

El cliente entra a un local de la app, elige productos y confirma.

### Datos obligatorios

- tipo de pedido: pedido de local;
- cliente;
- teléfono;
- local;
- productos;
- cantidades;
- precio de productos;
- dirección de entrega;
- forma de pago;
- precio de envío;
- validación de teléfono;
- número público de seguimiento;
- estado del local al confirmar;
- snapshot de productos/precios.

### Flujo conceptual

```text
Cliente elige productos
↓
Carga datos
↓
Confirma
↓
Sistema valida teléfono/datos
↓
Pedido entra al local
↓
Local acepta o rechaza
↓
Si acepta, prepara
↓
Cuando está listo, pasa a repartidores
↓
Repartidor toma
↓
Retira
↓
Entrega
↓
Verifica pago si corresponde
↓
Cierre operativo/financiero
↓
Archivo histórico
```

### Regla crítica

```text
El Pedido no pasa a repartidores antes de que el local lo acepte y confirme que puede prepararlo.
```

---

## 4.2 Compra directa

El cliente pide que Pédilo compre algo.

Ejemplo:

```text
Comprame una Coca grande y un kilo de pan.
```

### Datos obligatorios

- tipo de pedido: compra directa;
- cliente;
- teléfono;
- descripción estructurada;
- ítems si la IA puede ordenarlos;
- destino;
- forma de pago;
- precio de envío;
- validación del teléfono;
- número público de seguimiento.

### Regla de IA

La IA puede ayudar a estructurar:

- producto;
- cantidad;
- variante;
- observación;
- lugar sugerido;
- posible parada;
- duda a confirmar.

La IA no inventa datos críticos.

Si el pedido es ambiguo, se pide aclaración antes de crear un Pedido operativo.

---

## 4.3 Retiro / Envío

El cliente solicita retirar algo de un punto y enviarlo a otro.

### Datos obligatorios

- tipo de pedido: retiro/envío;
- cliente;
- teléfono;
- origen;
- destino;
- contacto en origen si existe;
- contacto en destino si existe;
- descripción del objeto;
- si está pago o no;
- quién paga;
- forma de pago;
- precio de envío;
- validación del teléfono;
- número público de seguimiento.

### Flujo conceptual

```text
Cliente carga origen
↓
Carga destino
↓
Describe objeto
↓
Declara pago/forma de pago
↓
Sistema calcula envío
↓
Cliente confirma
↓
Validación
↓
Pedido entra a operación
↓
Repartidor toma
↓
Retira
↓
Entrega
↓
Cierra cobro si corresponde
```

---

## 4.4 Solicitud de repartidor por local

Un local operativo solicita un repartidor para una operación propia.

### Datos obligatorios

- tipo de pedido: solicitud de repartidor;
- local solicitante;
- usuario activo del local;
- origen;
- destino;
- contacto de destino;
- si el producto ya está pagado;
- quién paga el envío;
- monto a cobrar si corresponde;
- forma de pago;
- precio de envío;
- número público de seguimiento.

### Regla de pago

El local debe declarar:

```text
producto_pagado = sí/no
envio_pagado_por = local / cliente_final
repartidor_cobra = sí/no
monto_a_cobrar = valor correspondiente
```

### Regla crítica

```text
Si el local no tiene usuario activo propio, no puede operar como local.
Puede existir como catálogo pasivo, pero no como actor operativo.
```

---

# 5. Pedido compuesto, tramos y paradas

## 5.1 Definición

Un Pedido sigue siendo uno solo, pero puede tener tramos internos.

Aparece cuando la operación incluye:

- compra en un lugar;
- retiro en un punto;
- parada adicional;
- entrega final;
- varios lugares;
- ítems por parada;
- cambios de recorrido.

Ejemplo:

```text
Comprame pan, pasá por la farmacia y después traelo a casa.
```

Eso debe estructurarse como:

- ítem de compra: pan;
- parada adicional: farmacia;
- destino final: casa;
- precio correspondiente;
- orden de recorrido;
- responsable;
- estado por tramo si corresponde.

## 5.2 Datos de cada tramo

Cada tramo puede tener:

- origen;
- destino;
- orden;
- descripción;
- ítems asociados;
- contacto;
- costo;
- responsable;
- estado;
- incidencia propia;
- evidencia si corresponde.

## 5.3 Regla

```text
El Pedido compuesto sigue siendo un solo Pedido,
pero sus tramos evitan ambigüedad operativa.
```

---

# 6. Cliente, teléfono y validación inicial

## 6.1 Teléfono como clave operativa

El teléfono es la clave práctica para:

- validar cliente;
- comunicar por WhatsApp;
- identificar cliente conocido;
- detectar cliente nuevo;
- detectar alertas;
- prevenir abuso;
- vincular reclamos;
- evitar pedidos falsos;
- proteger el flujo.

El usuario público no necesita login operativo para pedir.  
La identificación mínima se basa en teléfono, datos del pedido y tracking.

## 6.2 Validación antes de entrar al flujo operativo

Regla dura:

```text
Un Pedido no entra al flujo operativo real hasta pasar la validación inicial requerida.
```

## 6.3 Cliente conocido limpio

Si el número ya existe y no tiene alertas:

```text
Cliente confirma
↓
Teléfono conocido limpio
↓
Pedido entra al flujo normal
```

No se pide confirmación extra.

## 6.4 Cliente nuevo

Si el número es nuevo o no está verificado:

```text
Cliente confirma
↓
Validación WhatsApp
↓
Si confirma: entra al flujo operativo
↓
Si no confirma: no entra al pool operativo
```

## 6.5 Cliente con alerta

Si el número tiene alerta:

```text
Cliente confirma
↓
Sistema detecta alerta
↓
Se aplica regla configurada
↓
Puede ir a revisión Admin, validación reforzada o bloqueo
```

No entra directo.

## 6.6 Conducta abusiva

Si el cliente intenta:

- cancelar fuera de regla;
- forzar excepciones;
- repetir pedidos sospechosos;
- desconocer pagos;
- generar reclamos abusivos;

el sistema puede registrar alerta interna.

Esa alerta:

- queda auditada;
- puede afectar futuras validaciones;
- no necesariamente bloquea el pedido actual;
- puede requerir revisión Admin.

---

# 7. Estados separados del Pedido

El Pedido no debe depender de un único estado general.

Debe tener cinco estados separados:

```text
estado_operativo
estado_financiero
estado_comunicacion
estado_incidencia
estado_archivo
```

Esto evita mezclar entrega, pago, WhatsApp, problemas y archivo.

---

## 7.1 Estado operativo

Describe la vida operativa del Pedido.

Estados conceptuales:

```text
intencion
pendiente_validacion
creado
esperando_confirmacion_cliente
esperando_aceptacion_local
aceptado_por_local
rechazado_por_local
en_preparacion
listo_para_retiro
esperando_repartidor
repartidor_asignado
esperando_retiro
retirado
en_entrega
entregado
cancelado
cerrado_operativamente
```

---

## 7.2 Estado financiero

Describe el dinero.

Estados conceptuales:

```text
no_aplica
pendiente
efectivo_al_entregar
transferencia_pendiente
transferencia_informada
comprobante_pendiente
en_revision
confirmado
rechazado
cobrado
pendiente_cierre
cerrado_financieramente
disputado
```

---

## 7.3 Estado de comunicación

Describe mensajes externos, principalmente WhatsApp.

Estados conceptuales:

```text
no_requerida
pendiente
generada
en_cola
enviada
entregada
leida
respondida
fallida
expirada
requiere_manual
```

---

## 7.4 Estado de incidencia

Describe problemas.

Estados conceptuales:

```text
sin_incidencia
incidencia_abierta
en_revision
en_resolucion
resuelta
escalada_admin
cerrada
```

---

## 7.5 Estado de archivo

Describe si el Pedido sigue operativo o histórico.

Estados conceptuales:

```text
activo
cerrado
archivable
archivado
solo_historico
```

---

# 8. Compatibilidad de estados

## 8.1 Combinaciones válidas

```text
operativo = entregado
financiero = pendiente_cierre
archivo = activo
→ válido: pedido entregado, pero falta cierre financiero.
```

```text
operativo = cerrado_operativamente
financiero = cerrado_financieramente
incidencia = sin_incidencia / cerrada
archivo = archivable
→ válido: puede archivarse.
```

```text
operativo = cancelado
financiero = pendiente / disputado
archivo = activo
→ válido: cancelado, pero con dinero pendiente.
```

```text
operativo = en_entrega
incidencia = incidencia_abierta
archivo = activo
→ válido: pedido vivo con problema.
```

## 8.2 Combinaciones prohibidas

```text
archivo = archivado
+ chat operativo abierto
→ prohibido.
```

```text
archivo = archivado
+ operativo = en_entrega
→ prohibido.
```

```text
operativo = esperando_repartidor
+ repartidor asignado
→ prohibido.
```

```text
operativo = entregado
+ sin evento de entrega
→ prohibido.
```

```text
financiero = cerrado_financieramente
+ financiero = disputado
→ prohibido.
```

```text
operativo = listo_para_retiro
+ local no aceptó
→ prohibido.
```

```text
operativo = en_entrega
+ repartidor no marcó retirado
→ prohibido.
```

---

# 9. Responsables del Pedido

Todo Pedido vivo debe tener responsable actual.

Responsables válidos:

```text
usuario_publico
local
repartidor
admin
sistema
revision_pago
```

No son responsables principales:

- IA;
- WhatsApp.

La IA asiste.  
WhatsApp comunica.  
El responsable siempre debe ser real.

## 9.1 Ejemplos

Si falta validar teléfono:

```text
responsable = usuario_publico
estado_comunicacion = pendiente
fallback = admin si expira
```

Si espera local:

```text
responsable = local
```

Si espera repartidor:

```text
responsable = sistema / cola de repartidores
```

Si ya fue tomado:

```text
responsable = repartidor
actor = repartidor asignado
```

Si hay problema sensible:

```text
responsable = admin
```

## 9.2 Regla dura

```text
No se guarda un Pedido vivo sin responsable.
```

Si no hay responsable claro:

```text
responsable = admin
motivo = fallback_no_responsible
```

---

# 10. Acciones permitidas

Una acción permitida depende de:

- tipo de Pedido;
- estado operativo;
- estado financiero;
- estado de incidencia;
- estado de archivo;
- responsable;
- rol;
- actor;
- permisos;
- versión actual;
- impacto;
- reglas configuradas.

## 10.1 Regla UI

La interfaz solo muestra lo que corresponde.

Si una opción no aplica, no se muestra.

Ejemplos:

- si el producto no usa stock, no se muestra stock;
- si el Pedido está archivado, no se muestra chat operativo;
- si el cliente no puede cancelar, no se muestra cancelar;
- si el driver superó capacidad, no puede tomar;
- si el local no está operativo, no puede aceptar.

## 10.2 Regla backend

Aunque la UI muestre una acción, el backend/núcleo valida.

```text
UI guía.
Backend decide.
Auditoría registra.
```

## 10.3 Acciones críticas

Son críticas:

- crear Pedido real;
- cancelar;
- reasignar;
- marcar retirado;
- marcar entregado;
- aprobar pago;
- rechazar pago;
- activar modo lluvia;
- cambiar tarifa;
- desactivar usuario;
- bloquear repartidor;
- cerrar incidencia;
- archivar Pedido.

Toda acción crítica requiere:

- rol autorizado;
- actor válido;
- estado compatible;
- versión actual;
- impacto claro;
- confirmación si corresponde;
- auditoría;
- fallback si falla.

---

# 11. Matriz conceptual de acciones por rol

## 11.1 Usuario público

Puede:

```text
crear_intencion
confirmar_pedido
responder_validacion
consultar_tracking
reportar_problema_activo
cancelar_si_estado_lo_permite
crear_reclamo_posterior_si_pedido_cerrado
```

No puede:

```text
cancelar_en_entrega
modificar_pedido_aceptado_sin_revision
ver_chat_interno
ver_auditoria
ver_responsables_internos
reabrir_pedido_cerrado
```

---

## 11.2 Local

Puede:

```text
aceptar_pedido
rechazar_con_motivo
informar_tiempo
marcar_en_preparacion
marcar_listo
informar_producto_no_disponible
informar_demora
abrir_chat_del_pedido
solicitar_repartidor
configurar_tienda
```

No puede:

```text
marcar_entregado
operar_pedido_ajeno
cambiar_estado_de_repartidor
cerrar_pago_dudoso
borrar_auditoria
cambiar_tarifas_globales
```

---

## 11.3 Repartidor

Puede:

```text
tomar_pedido_disponible
marcar_retirado
marcar_en_entrega
confirmar_cobro
marcar_entregado
reportar_problema
hacer_cierre_de_caja
```

No puede:

```text
tomar_si_supera_capacidad
tomar_si_bloqueado_financieramente
marcar_entregado_sin_retirar
cancelar_sin_regla
aprobar_pago_dudoso
operar_pedido_archivado
```

---

## 11.4 Admin

Puede, siempre por acción validada:

```text
intervenir_pedido
reasignar
cancelar_con_motivo
resolver_incidencia
activar_modo_lluvia
desactivar_modo_lluvia
activar_modo_saturacion
desactivar_usuario
desbloquear_usuario
aprobar_cierre_caja
rechazar_cierre_caja
modificar_configuracion
```

Regla:

```text
Admin tiene poder operativo total,
pero no escritura libre fuera del núcleo.
```

---

# 12. Usuario público

## 12.1 Qué ve

El usuario público ve:

- Home;
- Tienda;
- locales;
- productos;
- formularios de pedido;
- ticket;
- tracking;
- avisos públicos;
- reclamo si corresponde.

## 12.2 Qué no ve

No ve:

- auditoría;
- chat interno;
- responsables internos;
- conflictos internos;
- IDs técnicos;
- pagos internos;
- cierres de caja;
- problemas no comunicables;
- métricas internas.

## 12.3 Cancelación

El cliente puede cancelar solo si el estado lo permite.

Regla base:

```text
antes de aceptación local → puede cancelar según regla
aceptado/preparando → puede requerir revisión o no permitirse
retirado/en entrega → no cancela libremente
cerrado/archivado → no cancela
```

Si no puede cancelar:

- puede reportar problema;
- puede recibir explicación;
- puede ir a canal de asistencia según el caso.

Intentos abusivos quedan auditados como alerta.

---

# 13. Local operativo

## 13.1 Qué es

Es un comercio con usuario propio activo para operar en Pédilo.

## 13.2 Qué puede hacer

- recibir pedidos propios;
- aceptar;
- rechazar;
- informar tiempo;
- preparar;
- marcar listo;
- reportar producto faltante;
- reportar demora;
- usar chat interno;
- gestionar tienda;
- cargar productos;
- configurar stock opcional;
- ver producción;
- ver historial propio;
- ver finanzas propias.

## 13.3 Aceptación del Pedido

Al aceptar, el local debe confirmar:

- que puede preparar;
- disponibilidad de productos;
- tiempo estimado;
- observaciones relevantes;
- si ya recibió pago cuando corresponda.

Solo después el Pedido puede avanzar a repartidores.

## 13.4 Rechazo

Debe indicar motivo:

- sin stock;
- local cerrado;
- demora excesiva;
- producto no disponible;
- error del pedido;
- no puede cumplir.

Después del rechazo, el sistema debe:

- avisar al cliente;
- abrir camino de modificación, cancelación o revisión;
- no dejar el Pedido muerto.

---

# 14. Local catálogo pasivo

## 14.1 Qué es

Un local cargado por Admin como referencia o punto de compra, pero sin usuario operativo propio.

## 14.2 Qué no puede hacer

No puede:

- aceptar pedidos;
- rechazar pedidos;
- marcar preparado;
- usar chat interno como local;
- gestionar tienda;
- operar como actor.

## 14.3 Regla

```text
Un local sin usuario activo propio no es actor operativo.
```

---

# 15. Tienda del local

## 15.1 Qué es

La tienda es la cara visible del local hacia el cliente.

Puede contener:

- productos;
- precios;
- descripción;
- imágenes;
- disponibilidad;
- stock opcional;
- promociones;
- tiempo estimado;
- estado del local.

## 15.2 Producto

Un producto puede tener:

- nombre;
- precio;
- descripción opcional;
- imagen opcional;
- disponibilidad;
- stock opcional;
- categoría;
- observaciones;
- variantes/opciones si aplica.

## 15.3 Variantes y opciones

Para productos reales, puede haber:

- tamaño;
- gusto;
- extras;
- unidad;
- peso;
- combo;
- aclaraciones.

Regla:

```text
Si una opción modifica precio, disponibilidad o preparación,
debe quedar estructurada y visible antes de confirmar.
```

## 15.4 Stock opcional

Si el local usa stock:

- se muestra cantidad;
- se descuenta al vender;
- al agotarse se desactiva o queda no disponible.

Si no usa stock:

- no se muestran campos de stock;
- el producto se ofrece sin conteo.

---

# 16. Repartidor

## 16.1 Qué es

Es el actor que ejecuta retiro, envío y entrega.

## 16.2 Antes de tomar un Pedido debe ver

- tipo de Pedido;
- origen;
- destino;
- paradas;
- pago;
- si debe cobrar;
- monto total;
- monto de envío;
- si producto ya está pago;
- modo lluvia si aplica;
- zona extra si aplica;
- observaciones operativas;
- tiempo relevante.

## 16.3 Capacidad

La capacidad del repartidor es configurable por Admin.

Puede tener:

- límite base;
- pedidos activos actuales;
- habilitación operativa;
- habilitación financiera;
- bloqueo;
- ajuste por modo lluvia/saturación si se configura.

Regla:

```text
Un repartidor no puede tomar más Pedidos que su capacidad vigente.
```

## 16.4 Bloqueo financiero

Si tiene cierre pendiente:

- puede quedar bloqueado para tomar nuevos;
- no se le cortan automáticamente pedidos activos sin resolución;
- puede terminar los activos o Admin puede intervenir.

---

# 17. Admin

## 17.1 Qué es

Admin es el rol de control total, configuración, supervisión y excepción.

## 17.2 Regla central

```text
La operación normal debe ser automática.
Admin interviene cuando hace falta.
```

## 17.3 Admin puede

- ver operación completa;
- ver pedidos activos;
- ver problemas;
- ver locales;
- ver repartidores;
- modificar configuración;
- activar/desactivar modo lluvia;
- activar/desactivar saturación;
- intervenir pedidos;
- reasignar;
- cancelar;
- resolver incidencias;
- revisar pagos;
- revisar cierres;
- bloquear/desbloquear usuarios;
- auditar.

## 17.4 Admin no debe

- operar fuera del backend;
- modificar históricos como vivos;
- romper auditoría;
- cambiar snapshots sin intervención explícita;
- desactivar usuarios con pedidos activos sin resolver impacto.

---

# 18. Configuración Admin

La configuración es un panel administrativo modular.

No es una base de datos cruda.

Debe permitir buscar, filtrar, listar, editar y ajustar elementos operativos.

## 18.1 Módulos de configuración

- configuración pública;
- pedidos;
- tarifas;
- locales;
- repartidores;
- pagos;
- WhatsApp;
- IA;
- notificaciones;
- modos operativos;
- seguridad;
- motivos de cancelación;
- motivos de incidencia;
- timeouts;
- fallbacks;
- cierres de caja.

## 18.2 Regla de impacto

```text
Configuración afecta Pedidos futuros.
Pedidos vivos conservan snapshot.
```

Si Admin modifica un Pedido vivo, eso no es configuración general; es intervención auditada.

---

# 19. Precios de envío

## 19.1 Fórmula cerrada

```text
precio_envio_total = tarifa_base_aplicable
                   + adicional_zona
                   + adicional_parada
                   + adicional_operativo_aprobado
```

## 19.2 Valores conceptuales trabajados

```text
envio_base_normal = 3500
envio_base_lluvia = 4000
adicional_zona = 1500
```

Los valores son configurables por Admin.

## 19.3 Reparto interno normal

```text
envio_base_normal 3500
→ repartidor 2500
→ Pédilo 1000
```

## 19.4 Reparto interno con lluvia

```text
envio_lluvia 4000
→ repartidor 3000
→ Pédilo 1000
```

La diferencia por lluvia compensa al repartidor.  
Pédilo mantiene su parte base salvo configuración explícita.

## 19.5 Adicional por zona

```text
adicional_zona 1500
→ repartidor 1500
→ Pédilo 0
```

Representa mayor recorrido operativo.

## 19.6 Adicional por parada

```text
adicional_parada
→ repartidor 100%
```

Representa trabajo operativo extra.

## 19.7 Snapshot de precio

Al confirmar el Pedido se congela:

- precio total;
- precio productos;
- precio envío;
- modo lluvia aplicado;
- zona extra aplicada;
- paradas;
- parte repartidor;
- parte Pédilo;
- forma de pago;
- quién cobra.

Después de confirmado, la configuración no cambia el precio del Pedido.

---

# 20. Modo lluvia

## 20.1 Qué es

Condición operativa activada o desactivada por Admin.

## 20.2 Puede afectar

- tarifa de envío;
- parte del repartidor;
- tiempos estimados;
- mensajes al cliente;
- umbrales de timeout;
- prioridad;
- disponibilidad;
- alertas.

## 20.3 Regla

```text
Modo lluvia no se aplica oculto.
Debe quedar visible, auditado y congelado en el Pedido si afectó precio.
```

---

# 21. Modo saturación operativa

## 21.1 Qué es

Modo para alta demanda o capacidad operativa limitada, aunque no haya lluvia.

## 21.2 Puede activarse por

- Admin;
- carga de pedidos;
- pocos repartidores;
- muchos locales demorados;
- tiempos altos;
- sistema en riesgo.

## 21.3 Puede afectar

- aceptación de nuevos pedidos;
- tiempos estimados;
- alertas;
- prioridad;
- disponibilidad;
- mensajes públicos.

## 21.4 Regla

```text
Modo lluvia y modo saturación no son lo mismo.
```

---

# 22. Pagos

## 22.1 Formas principales

- efectivo;
- transferencia.

## 22.2 Efectivo

```text
Cliente paga al repartidor
↓
Repartidor confirma cobro
↓
Pedido puede cerrar operativamente
↓
Monto entra al cierre de caja del repartidor
```

## 22.3 Transferencia

```text
Pedido informa total y alias correspondiente
↓
Cliente transfiere
↓
Repartidor verifica acreditación
↓
Si confirma: pago confirmado
↓
Si no: pago en revisión/disputa
```

## 22.4 Producto ya pagado

Si el producto ya está pagado al local:

```text
repartidor cobra solo envío
```

## 22.5 Solicitud de repartidor por local

El local debe declarar:

```text
producto_pagado = sí/no
envio_pagado_por = local / cliente_final
repartidor_cobra = sí/no
monto_a_cobrar = valor
```

## 22.6 Disputas de pago

Casos:

- transferencia no acreditada;
- cliente dice que pagó pero no aparece;
- pago menor;
- pago mayor;
- comprobante dudoso;
- repartidor informa cobro incorrecto;
- local declaró producto pagado y no lo estaba;
- cancelación con pago involucrado.

Estados conceptuales:

```text
pago_informado
pago_no_acreditado
pago_menor
pago_mayor
comprobante_dudoso
en_revision_admin
confirmado
rechazado
disputado
```

Regla:

```text
Un Pedido puede estar entregado,
pero no cerrado financieramente si hay disputa.
```

---

# 23. Cierre de caja del repartidor

## 23.1 Qué es

Proceso diario donde el repartidor regulariza lo cobrado y lo que corresponde a Pédilo.

## 23.2 Regla

El repartidor debe hacer cierre de caja diario.

## 23.3 Si no cierra

```text
estado = cierre_pendiente
puede_bloquear_nuevos_pedidos = sí
```

Si no tiene pedidos activos:

```text
bloqueo para tomar nuevos Pedidos
```

Si tiene pedidos activos:

```text
puede finalizar activos
no puede tomar nuevos
Admin puede intervenir o reasignar si hay riesgo
```

## 23.4 Formas de regularizar

- efectivo coordinado con Admin;
- transferencia;
- comprobante/captura.

## 23.5 IA

Puede verificar:

- monto esperado;
- monto informado;
- hora;
- comprobante;
- coincidencia con pedidos.

No aprueba casos dudosos sola.

## 23.6 Admin

Admin puede:

- aprobar;
- rechazar;
- marcar deuda;
- bloquear;
- desbloquear;
- auditar.

---

# 24. IA

## 24.1 Qué es

Capa de asistencia controlada.

No es autoridad.  
No es responsable principal.  
No reemplaza backend.  
No decide estados críticos.

## 24.2 Puede

- interpretar mensajes;
- estructurar pedidos libres;
- detectar datos faltantes;
- clasificar incidencias;
- sugerir acciones;
- preparar mensajes;
- resumir chats;
- verificar señales de pago;
- priorizar alertas;
- explicar acciones no disponibles.

## 24.3 No puede

- cancelar sola;
- cobrar;
- aprobar pagos dudosos;
- asignar repartidor sola;
- cambiar estado crítico sola;
- cerrar pedidos;
- bloquear clientes sola;
- modificar auditoría;
- reabrir históricos.

## 24.4 Niveles de intervención

```text
Nivel 1: informa
Nivel 2: recomienda
Nivel 3: inicia flujo controlado
```

En ningún nivel ejecuta acción crítica sin backend.

---

# 25. WhatsApp API

## 25.1 Qué es

Canal de comunicación externa.

Sirve para:

- validar teléfono;
- confirmar datos;
- avisar estados;
- pedir aclaraciones;
- comunicar demoras;
- tratar excepciones;
- contactar cliente/local/repartidor si corresponde.

## 25.2 Qué no es

No es:

- núcleo;
- autoridad;
- chat interno;
- responsable principal;
- fuente única de verdad.

## 25.3 Estados

```text
no_requerido
pendiente
generado
en_cola
enviado
entregado
leido
respondido
fallido
expirado
requiere_manual
```

## 25.4 Si falla

```text
estado_comunicacion = fallido
se registra evento
se activa fallback
se notifica al responsable
puede escalar a Admin
```

Regla:

```text
WhatsApp comunica.
WhatsApp no gobierna.
```

---

# 26. Chat interno por Pedido

## 26.1 Qué es

Comunicación interna ligada exclusivamente a un Pedido vivo.

No es chat libre entre roles.

## 26.2 Participantes posibles

- Admin;
- local;
- repartidor;
- sistema/IA como asistencia o resumen.

El cliente no participa en este chat interno salvo que se diseñe otro canal distinto.

## 26.3 Condiciones

- solo por Pedido;
- solo mientras el Pedido está vivo;
- no cambia estados por texto;
- puede tener adjuntos controlados;
- queda auditado;
- al cerrar queda solo lectura.

## 26.4 Texto no cambia estado

Si local escribe:

```text
Ya está listo.
```

Eso no cambia el estado.

Debe existir acción validada:

```text
Marcar como listo.
```

## 26.5 Adjuntos

Pueden ser:

- comprobante;
- foto de evidencia;
- imagen de problema;
- nota;
- archivo operativo.

Regla:

```text
Los adjuntos del chat siguen la privacidad del Pedido.
```

## 26.6 Pedido cerrado

Cuando el Pedido se cierra/archiva:

```text
chat = solo lectura
```

Si hay reclamo posterior, se crea caso vinculado. No se reabre chat operativo.

---

# 27. Notificaciones

## 27.1 Qué son

Avisos que empujan acciones y evitan que el Pedido se trabe.

## 27.2 Cliente

Recibe:

- validación;
- pedido recibido;
- preparación;
- demora comunicable;
- en camino;
- entregado;
- cancelado;
- reclamo respondido.

## 27.3 Local

Recibe:

- pedido nuevo;
- pendiente de aceptar;
- timeout cercano;
- mensaje interno;
- problema;
- modo lluvia si afecta;
- cambio operativo.

## 27.4 Repartidor

Recibe:

- pedido disponible;
- asignación;
- retiro pendiente;
- entrega pendiente;
- problema;
- cierre de caja;
- bloqueo;
- mensaje interno.

## 27.5 Admin

Recibe:

- pedidos trabados;
- pedidos sin responsable;
- local no responde;
- driver no responde;
- cliente no responde;
- pago dudoso;
- WhatsApp falló;
- IA no resolvió;
- modo lluvia;
- saturación;
- errores del sistema.

## 27.6 Alta producción

No se muestran miles de alertas sueltas.

Se agrupan por:

- prioridad;
- tipo;
- rol;
- estado;
- impacto;
- urgencia.

---

# 28. Incidencias

## 28.1 Qué son

Problemas ligados a un Pedido.

## 28.2 Tipos base

- local no responde;
- repartidor no responde;
- cliente no responde;
- producto no disponible;
- dirección incompleta;
- pago no claro;
- demora local;
- demora repartidor;
- pedido duplicado;
- cliente reclama;
- entrega fallida;
- WhatsApp falla;
- IA no resuelve;
- sistema falla.

## 28.3 Cada incidencia debe tener

- Pedido;
- tipo;
- origen;
- responsable;
- prioridad;
- estado;
- acciones permitidas;
- comunicación;
- chat si corresponde;
- resolución;
- auditoría;
- tiempo de advertencia;
- tiempo de timeout;
- escalada.

---

# 29. Resolución de incidencias con tiempos

## 29.1 Local no responde

```text
estado = esperando_aceptacion_local
↓
tiempo_advertencia
↓
notificar local
↓
tiempo_timeout
↓
incidencia local_no_responde
↓
notificar Admin
↓
opciones: WhatsApp local / avisar cliente / cancelar / resolver
```

## 29.2 Repartidor no responde

```text
estado = repartidor_asignado o esperando_retiro
↓
timeout
↓
incidencia driver_no_responde
↓
Admin puede reasignar
↓
repartidor puede quedar limitado o bloqueado según regla
```

## 29.3 Cliente no responde

```text
repartidor reporta cliente_no_responde
↓
WhatsApp cliente
↓
espera breve
↓
si pedido vino de local, notificar local
↓
escalada Admin si no se resuelve
```

Regla operativa:

```text
La resolución de cliente no responde no puede secuestrar al repartidor indefinidamente.
Debe escalar rápido, con margen breve y salida segura.
```

## 29.4 Producto no disponible

```text
local informa producto_no_disponible
↓
cliente acepta reemplazo o rechaza
↓
si cliente no responde, Admin decide camino permitido
↓
pedido sigue modificado o se cancela
```

La IA puede ayudar a redactar, pero no decide reemplazo final.

---

# 30. Cancelaciones

## 30.1 Tipos

- antes de aceptación;
- después de aceptación;
- después de preparación;
- después de retiro;
- durante entrega;
- por cliente;
- por local;
- por repartidor;
- por Admin;
- por sistema.

## 30.2 Cada cancelación debe definir

- quién puede;
- en qué estado;
- si requiere confirmación;
- impacto operativo;
- impacto financiero;
- mensaje al cliente;
- auditoría;
- alerta si corresponde.

## 30.3 Regla

```text
No toda cancelación tiene el mismo costo ni la misma consecuencia.
```

---

# 31. Reclamos posteriores

## 31.1 Regla

```text
Un Pedido cerrado no se reabre.
Se crea un caso posterior vinculado al Pedido histórico.
```

Si aparece reclamo después:

```text
Se crea un caso posterior vinculado al Pedido histórico.
```

## 31.2 Caso posterior debe tener

- reclamo;
- Pedido vinculado;
- cliente;
- motivo;
- evidencia;
- responsable;
- estado;
- resolución;
- auditoría.

---

# 32. Tracking público

## 32.1 Qué es

Vista simple para el cliente.

Muestra:

- recibido;
- validando;
- en preparación;
- coordinando envío;
- en camino;
- entregado;
- demora comunicable;
- revisión;
- cancelado.

## 32.2 Qué no muestra

- auditoría;
- responsables internos;
- conflictos;
- chat interno;
- problemas sensibles;
- IDs técnicos;
- finanzas internas;
- acciones internas.

## 32.3 Pedido archivado

Si está archivado:

- no muestra detalles operativos viejos;
- muestra cierre simple;
- permite volver a pedir.

---

# 33. Métricas

## 33.1 Regla

Las métricas nacen de eventos del Pedido.

No nacen de pantallas sueltas.

## 33.2 Usuario público

No ve métricas internas.

Ve tracking de su Pedido.

## 33.3 Local

Ve:

- producción;
- pedidos recibidos;
- pedidos aceptados;
- pedidos rechazados;
- tiempos;
- ventas;
- rendimiento.

## 33.4 Repartidor

Ve:

- pedidos realizados;
- entregas;
- cobros;
- ganancias;
- cierres;
- deuda;
- habilitación.

## 33.5 Admin

Ve:

- pedidos activos;
- pedidos por estado;
- tiempos promedio;
- locales demorados;
- repartidores demorados;
- reclamos abiertos;
- pagos pendientes;
- cierres pendientes;
- modo lluvia;
- saturación;
- producción total;
- salud del sistema.

---

# 34. Auditoría

Todo evento importante queda registrado.

Debe registrar:

- qué pasó;
- cuándo;
- quién actuó;
- desde qué rol;
- estado anterior;
- estado posterior;
- motivo;
- impacto;
- comunicación;
- pago si aplica;
- intervención Admin;
- fallo si aplica.

Eventos mínimos:

- pedido creado;
- validación enviada;
- validación confirmada;
- local aceptó;
- local rechazó;
- local informó demora;
- local marcó listo;
- repartidor tomó;
- repartidor retiró;
- repartidor entregó;
- pago informado;
- pago confirmado;
- pago disputado;
- incidencia abierta;
- incidencia resuelta;
- WhatsApp enviado;
- WhatsApp falló;
- chat enviado;
- Admin intervino;
- modo lluvia activado;
- modo lluvia desactivado;
- cierre de caja hecho;
- cierre de caja rechazado;
- pedido cerrado;
- pedido archivado.

---

# 35. Seguridad y privacidad

## 35.1 Cliente

No ve:

- auditoría;
- chat interno;
- responsables internos;
- alertas internas;
- finanzas internas;
- datos de otros pedidos;
- detalles viejos de pedidos archivados.

## 35.2 Local

No ve:

- pedidos de otros locales;
- finanzas del repartidor;
- configuración global;
- acciones Admin sensibles;
- información innecesaria del cliente.

## 35.3 Repartidor

No ve:

- pedidos ajenos;
- finanzas del local;
- configuración;
- información interna innecesaria.

## 35.4 Admin

Ve lo necesario para operar y auditar.

Sus acciones sensibles quedan auditadas.

## 35.5 IA

Procesa solo lo necesario para:

- interpretar;
- validar;
- sugerir;
- comunicar;
- auditar.

No expone información fuera del rol correcto.

---

# 36. Modos operativos

## 36.1 Normal

Operación habitual.

## 36.2 Modo lluvia

Condición climática/operativa activada por Admin.

## 36.3 Modo saturación

Condición de exceso de demanda o baja capacidad.

## 36.4 Modo mantenimiento

Se usa para limitar operación por tareas internas.

Regla:

```text
Mantenimiento no rompe Pedidos vivos.
```

## 36.5 Modo emergencia

Se usa ante fallas graves:

- WhatsApp caído;
- IA caída;
- backend con errores;
- muchos pedidos trabados;
- falta masiva de repartidores;
- incidente operativo grave.

---

# 37. Salud del sistema

Admin debe poder ver:

- backend;
- WhatsApp;
- IA;
- notificaciones;
- colas;
- errores;
- pedidos trabados;
- tiempos altos;
- saturación;
- repartidores disponibles;
- locales activos;
- cierres pendientes.

Esto es obligatorio para producción intensa.

---

# 38. Producción intensa

Pédilo debe resistir 1.000 pedidos simultáneos.

## 38.1 Debe evitar

- duplicados;
- doble asignación;
- doble entrega;
- doble cobro;
- doble WhatsApp;
- Pedidos sin responsable;
- estados imposibles;
- chats activos en pedidos cerrados;
- Admin colapsado por ruido;
- Pedidos vivos sin fallback.

## 38.2 Necesita

- idempotencia;
- acciones transaccionales;
- validación de versión;
- colas internas;
- resúmenes;
- prioridades;
- agrupación de alertas;
- archivo de cerrados;
- métricas desde eventos;
- auditoría eficiente.

## 38.3 Inmediato y bloqueante

Debe procesarse de forma inmediata:

- crear Pedido;
- validar idempotencia;
- validar teléfono mínimo;
- cambiar estado operativo;
- tomar Pedido;
- asignar repartidor;
- marcar retirado;
- marcar entregado;
- cancelar;
- congelar precio;
- registrar evento crítico.

## 38.4 En cola / no bloqueante

Puede ir a cola:

- WhatsApp no crítico;
- notificaciones secundarias;
- métricas;
- resúmenes Admin;
- auditoría secundaria expandida;
- IA de resumen;
- reportes.

## 38.5 En cola con fallback

Puede ir a cola, pero con fallback:

- WhatsApp de validación;
- WhatsApp de cliente no responde;
- mensaje de pago;
- alerta de incidencia.

Si falla la cola:

- no se pierde el Pedido;
- se registra fallo;
- se reintenta con idempotencia;
- se alerta internamente.

---

# 39. Idempotencia y concurrencia

## 39.1 Doble confirmación

Si el cliente confirma dos veces:

```text
misma intención + misma ventana + misma clave
→ no crea dos Pedidos
```

## 39.2 Dos repartidores toman el mismo Pedido

Solo gana uno.

El otro recibe:

```text
Este Pedido ya fue tomado.
```

## 39.3 Acción sobre estado viejo

Toda acción debe validar versión actual.

Si el Pedido cambió:

```text
acción rechazada
UI refresca estado real
```

## 39.4 WhatsApp duplicado

No se debe enviar dos veces el mismo mensaje operativo por reintento.

Clave conceptual:

```text
tipo_mensaje + Pedido + destinatario + etapa
```

---

# 40. Tests conceptuales de colapso

## 40.1 Test de Pedido vivo

Todo Pedido vivo debe responder:

- qué es;
- en qué estado está;
- quién debe actuar;
- qué puede hacerse;
- qué pasa si nadie actúa;
- qué ve el cliente;
- qué queda auditado.

Si no puede responder, no está listo.

## 40.2 Test de acción

Toda acción debe responder:

- quién la ejecuta;
- qué rol tiene;
- si puede;
- si el estado permite;
- si requiere confirmación;
- si impacta pagos;
- si impacta cliente;
- si notifica;
- si audita;
- qué pasa si falla.

## 40.3 Test de comunicación

Todo mensaje debe responder:

- por qué se envía;
- a quién;
- qué estado lo habilita;
- qué pasa si falla;
- si queda registrado;
- si puede duplicarse;
- si cambia algo o solo informa.

## 40.4 Test de rol

Cada rol debe responder:

- qué ve;
- qué puede hacer;
- qué no puede hacer;
- qué pasa si se equivoca;
- qué pasa si no responde;
- quién lo controla.

---

# 41. Reglas duras finales

1. El Pedido es la única verdad operativa.
2. Ningún Pedido vivo queda sin responsable.
3. Ningún Pedido vivo queda sin fallback.
4. La UI no inventa estados.
5. El backend valida acciones críticas.
6. La IA ayuda, no decide estados críticos.
7. WhatsApp comunica, no gobierna.
8. WhatsApp no es responsable principal.
9. La IA no es responsable principal.
10. El chat interno solo vive con el Pedido.
11. El chat no cambia estados por texto.
12. Pedido cerrado no se reabre.
13. Reclamo posterior crea caso vinculado.
14. Admin puede intervenir todo, pero dentro del sistema.
15. Configuración afecta futuros; Pedidos vivos conservan snapshot.
16. Precio confirmado queda congelado.
17. Pago operativo y cierre financiero se separan.
18. Repartidor no toma más Pedidos que su capacidad.
19. Local saturado modifica entrada de pedidos.
20. Cierre de caja diario puede bloquear nuevos pedidos del repartidor.
21. Notificaciones se agrupan por prioridad.
22. Métricas nacen de eventos del Pedido.
23. Cliente no ve información interna.
24. Pedido archivado queda solo histórico.
25. Producción intensa exige idempotencia y concurrencia segura.
26. Toda excepción debe tener responsable, acción y auditoría.
27. Ningún actor puede operar fuera de su rol.
28. Pago dudoso no se cierra automáticamente.
29. Modo lluvia no tapa saturación; son modos distintos.
30. Trabajo crítico es inmediato; trabajo secundario puede ir a cola.
31. Toda cola debe tener idempotencia y fallback.
32. Todo evento crítico queda auditado.

---

# 42. Cosas que no deben entrar salvo decisión explícita

No introducir:

- GPS;
- mapas;
- tracking geográfico;
- rutas en vivo;
- lógica tipo Uber/Rappi;
- chat libre entre roles;
- acciones críticas solo por IA;
- pagos aprobados solo por IA;
- cancelaciones automáticas sin regla;
- pedidos cerrados editables;
- cliente viendo datos internos;
- roles inventados;
- operación manual como flujo normal;
- WhatsApp como única verdad;
- IA como autoridad;
- configuración que cambie pedidos vivos sin snapshot.

---

# 43. Cierre conceptual

Pédilo queda definido como una app de mensajería local centrada en el Pedido Vivo Universal.

Funcionamiento general:

```text
El cliente pide.
El sistema valida.
El Pedido nace.
El local acepta si corresponde.
El repartidor toma si corresponde.
El pago se informa y verifica según modalidad.
La IA ayuda a interpretar y resolver.
WhatsApp comunica hacia afuera.
El chat interno coordina roles dentro del Pedido.
Admin supervisa y resuelve excepciones.
El backend protege la verdad.
Todo queda auditado.
El Pedido se cierra.
Lo cerrado queda histórico.
```

Regla final:

```text
Pédilo puede fallar en un actor,
pero no puede perder la verdad del Pedido.

Puede fallar WhatsApp,
pero no puede perder la operación.

Puede fallar IA,
pero no puede frenar el núcleo.

Puede intervenir Admin,
pero no puede romper auditoría.

Puede cerrar un Pedido,
pero no puede reabrirlo como si siguiera vivo.

Puede tener 1.000 Pedidos,
pero ninguno debe existir sin responsable, acción siguiente y salida segura.
```

---
---

# Cláusula final de uso para desarrollo

Este documento cierra reglas de producto, operación y comportamiento conceptual de Pédilo.

La etapa técnica no debe redefinir estas reglas.
La etapa técnica debe traducirlas a arquitectura, datos, pantallas, funciones, validaciones, seguridad, pruebas y monitoreo.

Si durante el desarrollo aparece una tensión técnica, no se resuelve cambiando el concepto silenciosamente.
Se debe volver al plano maestro, identificar la regla afectada y resolverla de forma explícita.

Ningún programador, agente, asistente o implementación debe convertir una limitación técnica momentánea en una modificación silenciosa del concepto de Pédilo.
# FIN DEL DOCUMENTO
