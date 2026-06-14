# PÉDILO — Informe de fidelidad conceptual y herramientas faltantes

Fecha: `2026-06-08`  
Branch auditada: `main`  
HEAD auditado: `fe803a3` — `Fix post-cleanup operational risks`

## 1. Fuentes usadas

Comparación realizada contra:

- `Pedilo.concepto.md`
- `README.md`
- `firestore.rules`
- `functions/index.js`
- `app/src/main/java/com/pedilo/app/**`
- `tests/*.test.js`
- `reports/auditoria-profunda-estado-real-app.md`
- `reports/limpieza-profunda-archivos-obsoletos.md`
- `reports/correccion-riesgos-post-limpieza.md`
- `git status --short`
- `git branch --show-current`
- `git log --oneline -20`

## 2. Estado de la fuente conceptual

En esta revisión sí se contó con la fuente conceptual real.

Archivo usado:

- `Pedilo.concepto.md`

Origen comprobado:

- se encontró primero en `/home/oem/Descargas/Pedilo.concepto.md`;
- se copió al workspace como `Pedilo.concepto.md`;
- luego se leyó completo desde el workspace.

## 3. Respuesta central

**La app actual refleja el corazón operativo de Pédilo, pero no refleja todavía el alcance completo del concepto cerrado.**

No es correcto decir que “solo funciona técnicamente”, porque hoy sí existen:

- un Pedido vivo central;
- backend como autoridad;
- operación real por roles;
- trazabilidad mínima;
- tracking público;
- incidencias operativas;
- protección de escritura directa a `/orders`.

Tampoco es correcto decir que “ya implementa Pédilo como producto conceptual completo”, porque el plano maestro exige además:

- validación real por WhatsApp;
- IA de asistencia controlada;
- chat interno por pedido;
- notificaciones operativas;
- pagos y cierres más ricos;
- solicitud de repartidor por local;
- módulos reales de configuración Admin;
- métricas nacidas de eventos;
- modos lluvia/saturación;
- producción intensa con colas, agrupación de alertas y salud del sistema.

Dictamen corto:

- **fidelidad al núcleo operativo**: `Parcial alto`
- **fidelidad al concepto cerrado completo**: `Parcial / incompleta`

## 4. Identidad de Pédilo

Concepto:

- Pédilo es un sistema de mensajería local y gestión de pedidos.
- No es solo marketplace, delivery, panel o catálogo.
- La unidad central es el Pedido.

Estado real:

- el pedido sí es la entidad central del sistema actual;
- público, Admin, Store y Driver convergen sobre `/orders`;
- el backend gobierna nacimientos y acciones vivas;
- la UI pública sigue teniendo una forma bastante cercana a catálogo + compra + tracking.

Clasificación: **Parcial alto**

Conclusión:

- la implementación actual ya salió del modelo “catálogo suelto + panel”;
- pero todavía no expresa toda la identidad de mensajería local, coordinación, comunicación y operación multi-canal definida en el concepto.

## 5. Pedido Vivo Universal

Concepto:

Un Pedido vivo debe tener:

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
- estado financiero;
- estado de comunicación;
- estado de incidencia;
- posibilidad de cierre.

Regla dura:

- ningún Pedido vivo puede quedar flotando.

Estado real:

Sí existe un núcleo vivo real en `functions/index.js` con:

- `orderType`
- `status`
- `publicStatus`
- `operationalStatus`
- `financialStatus`
- `communicationStatus`
- `incidentStatus`
- `archiveStatus`
- `responsibleRole`
- `currentResponsibleRole`
- `assignedActorId`
- `assignedActorRole`
- `nextAllowedActions`
- `timeoutPolicy`
- `fallbackPolicy`
- `version`
- `idempotencyKey`
- `liveSnapshot`
- `initialSnapshot`

Además:

- `tests/live_order_birth_contract.test.js` valida que el Pedido nazca no flotante;
- `tests/live_order_end_to_end_flow.test.js` valida el ciclo Store -> Driver -> cierre.

Clasificación: **Cumple en V1, pero no cumple todavía todo el alcance conceptual**

Qué sí cumple:

- un solo pedido como fuente de verdad;
- identidad;
- tipo;
- responsable actual;
- actor asignado;
- acciones permitidas;
- cierre y archivo;
- auditoría mínima;
- concurrencia por versión.

Qué queda parcial frente al concepto:

- el concepto exige cinco estados separados ricos; el repo hoy maneja separación, pero con vocabulario y profundidad bastante menores;
- `timeoutPolicy` y `fallbackPolicy` nacen, pero no se ve un motor operativo autónomo que los ejecute como sistema completo;
- no existe todavía el ecosistema de comunicación, notificación y colas que el concepto supone alrededor del Pedido vivo;
- no existe chat interno operativo;
- no existe caso posterior de reclamo realmente implementado.

## 6. Tipos de Pedido

Concepto cerrado:

1. pedido de local  
2. compra directa  
3. retiro / envío  
4. solicitud de repartidor por local

Estado real:

Sí existen en código:

- `local_order`
- `direct_purchase`
- `pickup_shipping`

No se encontró implementado como tipo operativo real:

- `solicitud de repartidor por local`

Clasificación: **Parcial**

Conclusión:

- la app actual cubre `3/4` tipos principales del concepto;
- falta bajar a implementación clara la solicitud de repartidor originada por local operativo.

## 7. Estados separados del Pedido

Concepto cerrado:

- `estado_operativo`
- `estado_financiero`
- `estado_comunicacion`
- `estado_incidencia`
- `estado_archivo`

Estado real:

Sí existen en el backend:

- `operationalStatus`
- `financialStatus`
- `communicationStatus`
- `incidentStatus`
- `archiveStatus`

Pero el repertorio implementado hoy es mucho más corto que el conceptual:

- no se ve `pendiente_validacion`;
- no se ve `esperando_confirmacion_cliente`;
- no se ve `esperando_aceptacion_local` como estado explícito;
- no se ve `en_entrega` como estado operativo separado del actual `picked_up`;
- no se ve `cerrado_operativamente` ni `cerrado_financieramente`;
- no se ve `solo_historico`;
- no se ve un `revision_pago` real como responsable o subflujo financiero operativo;
- no se ve un flujo de disputa de pagos implementado.

Clasificación: **Parcial**

Conclusión:

- la separación conceptual de estados empezó a implementarse;
- todavía falta bastante para igualar la granularidad cerrada del plano maestro.

## 8. Responsables del Pedido

Concepto cerrado:

Responsables válidos:

- `usuario_publico`
- `local`
- `repartidor`
- `admin`
- `sistema`
- `revision_pago`

Estado real:

Sí aparecen operativamente:

- `admin`
- `store`
- `driver`

No se observan implementados como responsables vivos explícitos:

- `usuario_publico`
- `sistema`
- `revision_pago`

Clasificación: **Parcial**

Conclusión:

- el repo actual cubre responsables humanos operativos;
- no cubre todavía toda la semántica conceptual de responsabilidad y fallback.

## 9. Regla “UI guía / backend decide”

Concepto:

- la UI muestra lo que corresponde;
- el backend decide;
- toda acción crítica debe validar rol, actor, estado, versión e impacto.

Estado real:

- `operateLiveOrder` valida actor, acción, versión y acciones permitidas;
- usa transacción;
- Store y Driver llaman Callable;
- público no escribe `/orders` directo;
- `firestore.rules` bloquea escrituras cliente sobre `/orders`, `/events` e `/incidents`.

Clasificación: **Cumple**

Conclusión:

- esta es una de las partes más fieles al concepto real.

## 10. Evaluación por rol

### 10.1 Usuario público

Concepto:

Debe poder:

- crear intención;
- confirmar pedido;
- responder validación;
- consultar tracking;
- reportar problema activo;
- cancelar si el estado lo permite;
- crear reclamo posterior si el pedido ya cerró.

Estado real:

Sí puede:

- crear pedido local;
- crear pedido Plus;
- confirmar;
- consultar tracking;
- recorrer catálogo público.

No se observó implementado realmente:

- validación operativa por WhatsApp;
- cancelación pública regulada por estado;
- reportar problema activo conectado al backend;
- crear reclamo posterior real vinculado a pedido histórico.

Evidencia importante:

- `PublicConventionsClaimScreen` solo hace `sent = true`;
- no hay callable ni persistencia real para reclamos públicos.

Clasificación: **Parcial**

### 10.2 Local operativo

Concepto:

Debe poder:

- recibir pedidos propios;
- aceptar;
- rechazar con motivo;
- informar tiempo;
- marcar en preparación;
- marcar listo;
- informar faltantes;
- informar demora;
- abrir chat del pedido;
- solicitar repartidor;
- gestionar tienda.

Estado real:

Sí puede hoy:

- ver pedidos propios;
- aceptar;
- rechazar;
- marcar preparación;
- marcar listo;
- abrir incidencia.

No se observó hoy:

- chat interno por pedido;
- flujo real de “informar tiempo” como herramienta operativa específica;
- solicitud de repartidor por local como tipo conceptual completo;
- gestión real de tienda desde el módulo Admin/Local;
- productos faltantes y reemplazos como flujo vivo real.

Clasificación: **Parcial**

### 10.3 Local catálogo pasivo

Concepto:

- existe como catálogo, pero no como actor operativo si no tiene usuario activo propio.

Estado real:

- el concepto aparece en textos y copy Admin;
- no se observó un modelo operativo claro ya implementado que diferencie formalmente local pasivo vs local operativo más allá de la lógica de roles y visibilidad.

Clasificación: **Parcial / no bajado del todo**

### 10.4 Repartidor

Concepto:

Debe poder:

- tomar pedido disponible;
- marcar retirado;
- marcar en entrega;
- confirmar cobro;
- marcar entregado;
- reportar problema;
- hacer cierre de caja;
- quedar limitado por capacidad o situación financiera.

Estado real:

Sí puede hoy:

- ver pedidos disponibles y asignados;
- tomar;
- marcar retirado;
- marcar entregado;
- abrir incidencia.

No se observó hoy:

- estado explícito `en_entrega`;
- confirmación real de cobro;
- cierre de caja;
- bloqueo por cierre pendiente;
- capacidad configurable realmente aplicada al tomar pedidos.

Clasificación: **Parcial**

### 10.5 Admin Operación

Concepto:

Admin puede:

- intervenir pedidos;
- reasignar;
- cancelar;
- resolver incidencias;
- revisar pagos;
- revisar cierres;
- bloquear/desbloquear usuarios;
- auditar;
- activar/desactivar modos operativos.

Estado real:

Sí existe:

- supervisión de pedidos;
- intervención operativa;
- cancelación;
- resolución de incidencias;
- auditoría mínima por eventos.

No se observó implementado realmente:

- revisión real de pagos;
- revisión de cierres de caja;
- control real de modos lluvia/saturación;
- salud de sistema como herramienta viva;
- agrupación operativa de alertas.

Clasificación: **Parcial**

### 10.6 Configuración Admin

Concepto:

Debe ser un panel modular real de configuración que permita buscar, filtrar, listar, editar y ajustar elementos operativos.

Módulos conceptuales:

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

Estado real:

- `AdminApp.kt` expone una enorme estructura de Configuración;
- los textos internos son explícitos en que esos bloques siguen siendo visuales, consultivos o no persistentes;
- `tests/admin_visual_shell.test.js` valida ese carácter visual.

Clasificación: **No implementado todavía como herramienta real**

Conclusión:

- conceptualmente está muy diagramado;
- funcionalmente todavía no existe como sistema vivo.

### 10.7 Alta de roles

Concepto:

- Admin debe poder gestionar usuarios, accesos y control operativo;
- toda acción sensible debe quedar dentro del sistema y auditada.

Estado real:

- existe shell visual para cuentas, roles, activaciones y vínculos;
- no se detectó alta real de cuentas/roles/vínculos en backend ni adapters dedicados.

Clasificación: **No implementado todavía como herramienta real**

## 11. Auditoría

Concepto:

Todo evento importante debe registrar:

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

Estado real:

Sí existe:

- evento inicial;
- eventos operativos;
- motivo en acciones críticas;
- actor/rol;
- versiones;
- subcolección `events`.

No se observa todavía:

- la lista amplia de eventos conceptuales completa;
- auditoría rica de WhatsApp, IA, pagos, notificaciones, cierres, modos operativos;
- auditoría administrativa transversal más allá del pedido.

Clasificación: **Parcial**

## 12. Incidencias

Concepto:

- deben tener tipo, responsable, prioridad, tiempos, escalada, comunicación y resolución.

Estado real:

- existen incidencias como subcolección;
- existen apertura y resolución operativa;
- existe prioridad/atención en pedido;
- Admin puede intervenir.

No se observa todavía:

- catálogo amplio de tipos de incidencia bajado al sistema;
- tiempos conceptuales por incidente;
- escalada automática más rica;
- bandeja viva especializada de incidentes.

Clasificación: **Parcial alto**

## 13. Tracking público

Concepto:

Debe mostrar una vista simple y segura:

- recibido;
- validando;
- en preparación;
- coordinando envío;
- en camino;
- entregado;
- demora comunicable;
- revisión;
- cancelado.

Estado real:

- el tracking existe y está bien protegido;
- no expone datos internos;
- devuelve estados públicos simples.

Límites:

- no se observa “validando” como etapa real del flujo;
- no se ve un tratamiento conceptual completo de “demora comunicable”;
- no se ve vínculo real con reclamo posterior.

Clasificación: **Parcial alto**

## 14. Pagos

Concepto:

- efectivo;
- transferencia;
- verificación;
- disputa;
- quién cobra;
- si producto ya está pagado;
- cierre financiero separado del cierre operativo.

Estado real:

Sí existe:

- `paymentMethod`;
- `financialStatus`;
- algunos datos iniciales en pedidos.

No existe en evidencia auditada:

- verificación real de transferencia;
- confirmación de cobro por repartidor;
- disputa de pago operativa;
- comprobantes;
- responsable `revision_pago`;
- cierre financiero separado y completo.

Clasificación: **Parcial bajo**

## 15. Cierre de caja del repartidor

Concepto:

- cierre diario;
- bloqueo para nuevos pedidos si queda pendiente;
- revisión y aprobación/rechazo Admin;
- apoyo de IA sin autoridad.

Estado real:

- no se encontró implementación operativa real de cierre de caja.

Clasificación: **No implementado**

## 16. IA

Concepto:

- capa de asistencia controlada;
- interpreta, estructura, resume, clasifica, sugiere;
- no decide estados críticos.

Estado real:

- no se encontró implementación funcional de IA en el runtime auditado.

Clasificación: **No implementado**

## 17. WhatsApp API

Concepto:

- canal externo para validación, avisos, aclaraciones, excepciones y fallback.

Estado real:

- no se encontró implementación funcional de WhatsApp API;
- aparecen referencias de UI al teléfono “WhatsApp” y textos conceptuales en Admin;
- tests negativos controlan que no se haya colado como autoridad en el runtime actual.

Clasificación: **No implementado**

## 18. Chat interno por Pedido

Concepto:

- sólo por pedido vivo;
- sólo entre roles internos;
- no cambia estados por texto;
- queda auditado;
- al cierre queda solo lectura.

Estado real:

- no se encontró implementación de chat interno por pedido.

Clasificación: **No implementado**

## 19. Notificaciones

Concepto:

- cliente, local, repartidor y Admin reciben señales operativas;
- alta producción exige agrupación por prioridad, tipo, rol e impacto.

Estado real:

- no se detectó motor real de notificaciones operativas;
- en Admin sólo aparece como dominio visual/configurativo.

Clasificación: **No implementado**

## 20. Métricas

Concepto:

- nacen de eventos del Pedido;
- deben existir vistas por local, repartidor y Admin.

Estado real:

- no se detectó cálculo real, backend dedicado ni dashboards vivos de métricas;
- sólo aparecen como mundo conceptual visual dentro de Admin.

Clasificación: **No implementado**

## 21. Modos operativos y salud del sistema

Concepto:

- modo lluvia;
- modo saturación;
- mantenimiento;
- emergencia;
- salud visible de backend, WhatsApp, IA, notificaciones, colas, errores, saturación y tiempos.

Estado real:

- no se observó implementación real de estos modos ni de salud operativa integral;
- sí aparecen mencionados en copy de Admin Configuración.

Clasificación: **No implementado**

## 22. Producción intensa

Concepto:

- 1.000 pedidos simultáneos;
- evitar duplicados, doble toma, doble cobro, estados imposibles y ruido operativo;
- trabajo crítico inmediato;
- trabajo secundario en cola;
- colas con idempotencia y fallback;
- agrupación de alertas;
- métricas desde eventos.

Estado real:

Sí existen bases importantes:

- idempotencia de creación;
- idempotencia por acción;
- control de versión;
- transacciones;
- prevención de doble toma por repartidor.

No se observó todavía:

- colas internas reales;
- procesamiento diferido explícito;
- agrupación real de alertas;
- tablero de salud;
- pipeline operacional para WhatsApp/notificaciones/IA.

Clasificación: **Parcial**

## 23. Herramientas reales hoy

### Público

- catálogo Firestore en lectura;
- pedido local por Function;
- pedido Plus por Function;
- tracking público.

### Backend / núcleo

- nacimiento transaccional;
- idempotencia;
- `operateLiveOrder`;
- `adminOrderAction`;
- eventos;
- incidencias;
- versión esperada;
- reglas de acceso.

### Admin Operación

- lectura de pedidos;
- detalle;
- clasificación;
- acciones operativas;
- historial reciente de eventos.

### Store

- pedidos propios;
- detalle;
- acciones Store.

### Driver

- pedidos disponibles/asignados;
- detalle;
- acciones Driver.

## 24. Herramientas que parecen existir pero hoy no son reales

- reclamo público persistente;
- Configuración Admin modular;
- alta real de roles y vínculos;
- pagos vivos completos;
- cierres de caja;
- WhatsApp operativo;
- IA operativa;
- chat interno por pedido;
- notificaciones reales;
- métricas reales;
- modos lluvia/saturación/mantenimiento/emergencia;
- salud integral del sistema;
- solicitud de repartidor por local.

## 25. Riesgos conceptuales principales

1. **Riesgo de ilusión de completitud**
   - Admin muestra mundos muy desarrollados visualmente que hoy no aplican cambios reales.

2. **Riesgo de lectura excesivamente optimista del núcleo**
   - el pedido vivo está bien encaminado, pero todavía representa solo una parte del contrato conceptual completo.

3. **Riesgo de reclamo falso**
   - el usuario público ve un formulario de reclamo que hoy no genera un caso real.

4. **Riesgo de brecha multi-canal**
   - el concepto depende de WhatsApp, IA, notificaciones y chat interno; el repo actual aún no los implementa.

5. **Riesgo financiero**
   - el concepto separa fuerte operación, pago y cierre; la implementación actual todavía está muy por debajo en esa capa.

6. **Riesgo de operación intensa incompleta**
   - hay buena base de idempotencia y versión, pero faltan colas, agrupación de alertas y visibilidad de salud del sistema.

## 26. Dictamen final

**La app actual es fiel al concepto de Pédilo en su núcleo operativo básico, pero todavía no es fiel al concepto cerrado completo del plano maestro.**

Resumen honesto:

- **sí cumple** la idea central de “un solo Pedido, backend como verdad, roles operativos, auditoría mínima y cierre controlado”;
- **cumple parcialmente** la separación conceptual de estados, responsables, tipos y flujo vivo;
- **no cumple todavía** gran parte del alcance conceptual de comunicación, pagos, cierres, IA, WhatsApp, chat interno, configuración real, notificaciones, métricas y operación intensa completa.

## 27. Respuesta final a la pregunta central

¿La app actual refleja realmente el concepto de Pédilo o sólo funciona técnicamente?

Respuesta:

**Refleja realmente una porción importante del concepto de Pédilo, pero todavía no refleja el sistema completo definido en `Pedilo.concepto.md`.**

En otras palabras:

- no es solo una app técnicamente funcional;
- tampoco es todavía la traducción completa del plano maestro cerrado.
