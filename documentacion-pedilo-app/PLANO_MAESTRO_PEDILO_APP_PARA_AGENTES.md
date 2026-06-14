# Pédilo App - Plano Maestro para Agentes

Este archivo es el plano maestro de trabajo para agentes sobre Pédilo App.

Su objetivo es evitar que el dueño tenga que redefinir Pédilo en cada pedido. Un agente debe poder leer este documento, recibir una orden simple de modulo y trabajar sin achicar el alcance, sin inventar producto y sin cerrar con avances parciales disfrazados de terminado.

Este archivo no reemplaza las fuentes de verdad. Las ordena para trabajo.

## 1. Fuentes de verdad

Los documentos que mandan son, en este orden:

1. `00_PEDILO_APP.md`
2. `01_PRODUCTO_ROLES_REGLAS.md`
3. `02_PEDIDO_VIVO_UNIVERSAL.md`
4. `03_ARQUITECTURA_TECNICA.md`
5. `04_ETAPAS_DE_CONSTRUCCION.md`
6. `05_DECISIONES_EXTERNAS.md`

Si hay duda, contradiccion o falta de precision, el agente debe volver a estos documentos antes de decidir.

Los documentos `06` en adelante son antecedentes, reportes, certificaciones, evidencias historicas o registros de trabajos previos. Sirven para entender problemas detectados, contexto tecnico y riesgos ya observados. No son fuente de reglas nuevas de producto y no convierten un estado historico como "APTA" o "NO APTA" en estado definitivo de toda la app.

## 2. Que es Pédilo

Pédilo App es un sistema operativo de pedidos para coordinar usuarios, locales, repartidores y administracion sobre una misma entidad central: el Pedido Vivo Universal.

No es solamente un catalogo. No es un formulario aislado. No es una derivacion a chat. No es una pantalla tecnica con botones.

Pédilo debe permitir crear pedidos, operarlos por roles, seguir su avance, gestionar excepciones, registrar eventos, separar el estado operativo del financiero y sostener coherencia entre backend, datos, seguridad y pantallas.

La autoridad del pedido vive en backend. La interfaz muestra informacion, solicita acciones y recibe resultados validados.

## 3. App que se quiere terminar

La app que se quiere terminar es una aplicacion real, usable y gobernada por reglas comunes, donde cada modulo funciona como producto y no como maqueta.

Debe existir una operacion completa por roles:

- Usuario Publico crea pedidos, consulta tracking seguro, cancela cuando corresponde e inicia reclamos posteriores.
- Local / Store opera pedidos propios, acepta, rechaza, prepara, marca listo y gestiona catalogo cuando corresponda.
- Repartidor / Driver toma pedidos permitidos, retira, entrega, cobra cuando la etapa financiera lo habilite y cierra caja cuando corresponda.
- Admin supervisa, interviene, configura, administra roles, audita y resuelve casos.

La app debe sentirse como un producto real:

- los datos visibles deben ser reales o explicitamente controlados por el entorno;
- las acciones deben tener resultado verificable;
- los formularios deben validar antes de confirmar;
- los roles no deben mezclarse;
- los errores deben ser tratables por una persona;
- las pantallas deben servir para operar, no solo para demostrar que compilan;
- los cierres deben apoyarse en evidencia, tests y validacion de uso.

## 4. Pedido Vivo Universal

El Pedido Vivo Universal es la entidad central de Pédilo App. Cada pedido concentra estado, responsables, snapshots, eventos y vistas por rol.

Entidad principal:

- `/orders/{id}`

Entidades vinculadas:

- `/orders/{id}/events`
- `/orders/{id}/incidents`
- reclamos posteriores vinculados al pedido;
- datos financieros y cierres cuando exista el modulo financiero;
- comunicaciones asociadas cuando exista el modulo de comunicacion.

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
- snapshots de datos relevantes al confirmar.

Los ejes de estado son:

- Operativo: ubicacion real del pedido dentro del flujo.
- Financiero: cobro, disputa, deuda, cierre y caja.
- Comunicacion: estado de mensajes y canales sin autoridad operativa.
- Incidencia: excepciones activas o resueltas.
- Archivo: vida, cierre, archivo y consulta posterior.

Los nombres conceptuales pueden estar en espanol y los wire names en ingles. La persistencia existente no se renombra sin migracion, tests y certificacion.

## 5. Flujo central del Pedido Vivo

El flujo operativo base incluye:

1. Crear pedido.
2. Validar nacimiento del pedido.
3. Aceptar o rechazar por Local cuando corresponde.
4. Marcar preparacion.
5. Marcar listo para retiro.
6. Tomar pedido por Repartidor.
7. Marcar retirado.
8. Marcar entregado operativamente.
9. Gestionar cancelaciones, incidencias o intervenciones cuando correspondan.
10. Resolver cierre operativo, financiero, comunicacional, de incidencia y archivo segun etapa.

Cada accion que cambie estado valida:

- usuario autenticado cuando corresponda;
- rol activo;
- permiso sobre el pedido;
- transicion permitida desde el estado actual;
- `expectedVersion`;
- idempotencia cuando la accion pueda repetirse;
- escritura de evento.

Errores esperados:

- `VERSION_MISMATCH`
- `INVALID_TRANSITION`
- `PERMISSION_DENIED`
- `ORDER_CLOSED`
- `failed-precondition`

## 6. Reglas que no se pueden romper

Estas reglas son fijas para cualquier agente:

- Un pedido tiene una verdad operativa.
- Cada cambio relevante genera un evento.
- Cada accion valida rol, permiso, estado y version.
- La comunicacion informa, pero no gobierna el pedido.
- La IA asiste, pero no decide acciones sensibles.
- Un pedido cerrado no se reabre.
- Un reclamo posterior se vincula al pedido cerrado sin cambiar su cierre.
- Los cambios de configuracion afectan pedidos futuros; los pedidos vivos conservan snapshot.
- Las metricas nacen de eventos auditables.
- Android no decide estados.
- Cloud Functions validan cambios de estado.
- Firestore persiste pedidos, eventos y datos vinculados.
- Security Rules bloquean accesos no permitidos.
- El cliente no escribe directo en `/orders`.
- Los eventos no se borran ni editan desde cliente.
- No se crean colecciones nuevas sin Rules y tests.
- No se despliega produccion ni se publica Google Play sin autorizacion explicita.
- No se inventan datos legales, comerciales, de soporte, privacidad, pagos, produccion o IA externa.

Estados incompatibles:

- archivado con chat operativo activo;
- archivado con pedido en entrega;
- esperando repartidor y repartidor asignado al mismo tiempo;
- entregado sin retiro previo;
- cerrado financieramente y disputado al mismo tiempo;
- listo para retiro sin aceptacion local previa;
- en entrega sin retiro previo;
- pedido cerrado reabierto.

## 7. Roles

### Usuario Publico

Crea pedidos mediante formularios validados, consulta tracking seguro, cancela cuando el estado lo permite e inicia reclamos posteriores sin reabrir pedidos cerrados.

No puede ver eventos internos, auditoria ni informacion operativa reservada. No se aceptan pedidos incompletos, datos demo como reales ni cancelaciones sin backend, evento y validacion de estado.

### Local / Store

Opera solamente pedidos de su `storeId`. Puede aceptar, rechazar, preparar y marcar listo segun transiciones permitidas. Gestiona catalogo, productos, variantes y stock cuando la etapa correspondiente este implementada.

No puede operar pedidos ajenos, avanzar estados fuera de orden, resolver finanzas completas dentro del modulo Store ni convertir catalogo pasivo en autoridad operativa.

### Repartidor / Driver

Toma pedidos disponibles, retira, entrega, cobra cuando la etapa financiera lo habilite y cierra caja segun reglas financieras.

No puede entregar sin retiro, tomar sobre capacidad, aprobar pagos dudosos ni incorporar GPS o mapas como alcance base.

### Admin

Supervisa la operacion, interviene pedidos, gestiona configuracion, administra usuarios y roles, audita eventos y resuelve casos.

No puede tratar un shell visual como modulo real, cambiar pedidos vivos desde configuracion sin intervencion auditada, crear roles sin trazabilidad ni ocultar acciones sensibles sin evento.

No existen roles adicionales salvo decision explicita de producto.

## 8. Tipos de pedido

Tipos definidos:

- `local_order`: pedido a local.
- `direct_purchase`: compra directa o plus.
- `pickup_shipping`: retiro y envio.
- `store_driver_request`: solicitud de repartidor desde local, auditada y gobernada por backend para prueba controlada.

No se agrega otro tipo de pedido sin decision de producto.

## 9. Modulos

Los modulos de Pédilo son:

- Pedido Vivo Universal.
- Backend, Firestore y seguridad.
- Usuario Publico.
- Admin.
- Local / Store.
- Repartidor / Driver.
- Pagos, tarifas y finanzas.
- Incidencias, reclamos y cancelaciones.
- Comunicacion.
- IA controlada.
- Metricas, auditoria y salud.
- Hardening y release.

Un modulo no se considera terminado por existir una pantalla, una ruta, un boton o una compilacion exitosa. Debe funcionar dentro del sistema de roles, backend, datos, seguridad, eventos y validacion que le corresponda.

## 10. Etapas de construccion

El orden de construccion es:

Q -> B -> M -> C -> F -> D -> E -> G -> I -> J -> K -> L -> O -> P

Reglas de ejecucion:

- Se ejecuta una etapa por vez.
- No se mezclan roles, backend, pagos, IA y release en una misma ejecucion.
- No se cambian reglas de producto durante implementacion.
- No se hace deploy ni publicacion sin autorizacion explicita.
- Las decisiones de dueño, legal, privacidad, dinero real o datos oficiales se registran como decisiones externas.

Paralelizacion permitida solo despues de certificar dependencias:

- C y F pueden avanzar parcialmente tras M.
- J y L pueden avanzar tras M.

Paralelizacion no permitida:

- G antes de E y F.
- P antes de O.
- Roles antes de B y M.

## 11. Como debe trabajar un agente

Antes de tocar codigo, el agente debe:

1. Leer este plano.
2. Identificar el modulo o etapa pedida.
3. Revisar los documentos fuente aplicables.
4. Revisar el repo real, HEAD, rutas, tests y guards relevantes.
5. Separar reglas de producto de reportes historicos.
6. Detectar decisiones externas o autorizaciones que bloqueen parte del trabajo.

Durante el trabajo, el agente debe:

- construir el modulo completo segun el objetivo de Pédilo;
- respetar roles, permisos, estado, version y eventos;
- mantener backend, Firestore, Rules, Android y tests coherentes cuando apliquen;
- conservar nombres persistidos existentes salvo migracion documentada;
- usar datos controlados y no placeholders como datos reales;
- validar flujos reales del rol, no solo componentes aislados;
- registrar riesgos, bloqueos y decisiones externas;
- evitar cambios fuera del modulo o etapa salvo que sean necesarios para cerrar correctamente.

Al cerrar, el agente debe entregar evidencia real, no solo un resumen.

## 12. Que puede decidir un agente

El agente puede decidir:

- organizacion tecnica interna compatible con la arquitectura existente;
- nombres de clases, funciones, tests y componentes no persistidos;
- adaptaciones de UI necesarias para que el modulo sea usable;
- validaciones locales no autoritativas;
- cobertura de tests proporcional al riesgo del modulo;
- refactors acotados que hagan posible cumplir la regla documentada;
- aislamiento de codigo viejo, demo o placeholder cuando impida producto real;
- orden interno de implementacion dentro de una etapa autorizada.

El agente debe elegir siempre la opcion que mantenga coherencia con Pédilo, reduzca riesgo y no cambie reglas de producto.

## 13. Que no puede decidir un agente

El agente no puede decidir:

- cambios de producto;
- roles nuevos;
- tipos de pedido nuevos;
- reglas comerciales nuevas;
- reglas legales, privacidad o textos definitivos;
- titular legal, razon social o email oficial;
- publicacion Google Play;
- deploy Firebase o produccion;
- uso de datos reales;
- integracion con pasarela de pago real;
- banco, conciliacion automatica o dinero real;
- proveedor real WhatsApp/push;
- IA externa con costo o datos sensibles;
- renombrar estados persistidos sin migracion y certificacion;
- convertir reportes historicos en estado definitivo;
- llamar terminado a un modulo que solo compila.

Si aparece una de estas decisiones, el agente debe registrarla como decision externa o bloqueo, no inventarla.

## 14. Que significa terminado

Un modulo esta terminado solo si:

- cumple el objetivo funcional definido para su etapa;
- funciona para el rol correspondiente de punta a punta;
- respeta autoridad backend, Rules y permisos;
- no mezcla responsabilidades entre roles;
- no rompe el Pedido Vivo;
- no crea estados incompatibles;
- registra eventos obligatorios;
- valida version, permisos, estado e idempotencia cuando corresponde;
- protege informacion interna del publico;
- elimina placeholders operativos y datos demo tratados como reales;
- tiene tests y guards relevantes ejecutados;
- tiene evidencia visual o funcional de uso real cuando aplica;
- deja documentados riesgos, bloqueos y decisiones externas.

Compilar no significa terminado. Tener UI no significa terminado. Pasar una prueba aislada no significa terminado. Un modulo terminado debe poder ser usado por una persona en el flujo real previsto por Pédilo.

## 15. Evidencia obligatoria de cierre

Cada cierre de modulo o etapa debe incluir:

- etapa o modulo trabajado;
- documentos fuente usados;
- archivos tocados;
- resumen de comportamiento implementado;
- flujo real validado;
- roles validados;
- backend, Rules, Firestore y Android afectados;
- eventos generados o protegidos;
- tests ejecutados, con resultado;
- guards ejecutados, con resultado;
- evidencia visual cuando haya UI;
- evidencia de emulator o backend cuando haya Functions, Rules o Firestore;
- riesgos residuales;
- decisiones externas detectadas;
- bloqueos si existen;
- resultado final;
- siguiente etapa posible.

Cuando haya UI Android, la evidencia debe mostrar pantallas reales del flujo, no solo capturas de pantallas vacias, placeholders o estados de carga. Cuando haya backend o seguridad, la evidencia debe incluir pruebas o emulator que demuestren validaciones y bloqueos.

## 16. Que no se acepta como cierre

No se acepta como cierre:

- "compila" como unica evidencia;
- "se ve bien" sin flujo real;
- una pantalla tecnica sin operacion de producto;
- botones sin backend cuando la accion debe ser autoritativa;
- datos demo o placeholders presentados como reales;
- reportes parciales sin archivos, tests ni evidencia;
- modulos que mezclan roles;
- acciones que saltean Functions;
- escrituras directas inseguras a `/orders`;
- cancelaciones sin evento;
- entregas sin retiro;
- doble toma de pedidos;
- pagos dudosos cerrados automaticamente;
- reclamos que reabren pedidos cerrados;
- metricas inventadas desde UI;
- chat, WhatsApp o IA cambiando estados;
- configuracion que modifica pedidos vivos retroactivamente sin auditoria;
- cierre financiero confundido con cierre operativo;
- publicar, desplegar o usar datos reales sin autorizacion.

## 17. Decisiones externas

Las decisiones externas no se inventan durante la construccion.

Pendientes obligatorias:

- EXT-01: razon social o titular legal.
- EXT-02: email oficial de soporte.
- EXT-03: aprobacion juridica de privacidad y datos.

Estas decisiones no bloquean la construccion tecnica de Q a O. Si siguen pendientes, P queda bloqueada para publicacion.

Requieren permiso directo del dueño:

- deploy Firebase o produccion;
- publicacion Google Play;
- uso de datos legales o comerciales oficiales;
- integracion con pasarela de pago real;
- activacion de IA externa con costo o datos sensibles;
- cambios de reglas de producto;
- modificacion de datos reales.

## 18. Uso en proximos pedidos

Cuando el dueño pida, por ejemplo, "Termina Local segun el Plano Maestro", el agente debe interpretar:

- trabajar el modulo Local / Store;
- respetar Pedido Vivo, backend, permisos, `storeId`, eventos y transiciones;
- no convertir Local en finanzas completas;
- no agregar chat como parte de Local;
- no permitir carrito multi-local;
- no operar pedidos ajenos;
- no cerrar con una pantalla shell;
- entregar evidencia real del modulo terminado.

La misma regla aplica a cualquier modulo: el agente debe usar este plano como mapa de alcance, reglas, limites, evidencia y cierre.

## 19. Regla final

El trabajo del agente es ordenar, construir, validar y cerrar modulos completos segun Pédilo.

El trabajo del agente no es redefinir Pédilo.
