# Etapas de Construccion

Pédilo App se construye por etapas. Cada etapa tiene un alcance cerrado, pruebas aplicables y criterios de bloqueo.

Orden:

Q -> B -> M -> C -> F -> D -> E -> G -> I -> J -> K -> L -> O -> P

## Regla de ejecucion

- Se ejecuta una etapa por vez.
- No se mezclan roles, backend, pagos, IA y release en una misma ejecucion.
- No se cambian reglas de producto durante implementacion.
- No se hace deploy ni publicacion sin autorizacion explicita.
- Las decisiones de dueño, legal, privacidad, dinero real o datos oficiales se registran como decisiones externas.

Cada etapa termina con reporte:

- etapa;
- documentos usados;
- archivos tocados;
- tests y guards;
- riesgos;
- decisiones externas detectadas;
- resultado;
- siguiente etapa posible o bloqueo.

## Q - Revalidacion tecnica

Objetivo: revisar el repo real antes de construir.

Incluye:

- HEAD y rutas;
- inventario tecnico;
- tests existentes;
- guards;
- placeholders;
- matriz conservar, ajustar o aislar.

Prohibido:

- features de negocio;
- deploy;
- cambios de producto.

Acepta si:

- el repo queda inventariado;
- los placeholders operativos quedan identificados;
- los tests y guards base tienen resultado claro.

Bloquea si:

- tests o guards base fallan sin explicacion;
- queda placeholder operativo sin registrar.

## B - Pedido Vivo Universal

Objetivo: endurecer nucleo, cinco ejes, transiciones, eventos, idempotencia y timeouts.

Incluye:

- contrato de nacimiento;
- acciones permitidas;
- version;
- eventos;
- concurrencia;
- timeouts y fallbacks iniciales.

Prohibido:

- UI por rol;
- pagos;
- IA;
- WhatsApp.

Acepta si:

- no hay estados imposibles;
- backend valida acciones;
- cada cambio relevante escribe evento.

Bloquea si:

- UI decide estados;
- el nacimiento de pedido se rompe;
- un pedido queda sin salida.

## M - Backend, Firebase y seguridad

Objetivo: asegurar Functions, Firestore, Rules y emulator.

Incluye:

- permisos por rol;
- colecciones;
- Rules;
- tests emulator;
- bloqueo de escrituras peligrosas.

Prohibido:

- deploy produccion;
- cliente escribiendo directo en `/orders`;
- colecciones sin Rules.

Acepta si:

- Functions validan rol, estado y version;
- Rules bloquean escrituras no autorizadas;
- eventos quedan protegidos.

Bloquea si:

- hay escritura directa de cliente a pedidos;
- una callable critica no valida rol.

## C - Usuario Publico

Objetivo: formularios, tracking y cancelacion publica.

Incluye:

- validaciones;
- tracking seguro;
- eliminacion de placeholders publicos;
- cancelacion si B y M lo permiten.

Prohibido:

- UI Admin, Store o Driver;
- WhatsApp real;
- pagos online.

Acepta si:

- Publico crea y consulta pedidos sin ver datos internos;
- cancelacion permitida pasa por backend.

Bloquea si:

- hay datos demo;
- hay ticket placeholder;
- auditoria interna queda visible al publico.

## F - Admin

Objetivo: Admin real de operacion, configuracion, roles e intervencion.

Incluye:

- tablero operativo;
- configuracion persistida;
- CRUD de roles;
- modos operativos;
- intervenciones auditadas.

Prohibido:

- configuracion retroactiva sobre pedidos vivos sin auditoria.

Acepta si:

- configuracion y roles dejan de ser shell;
- las intervenciones quedan trazadas.

Bloquea si:

- roles o configuracion no tienen auditoria.

## D - Local / Store

Objetivo: operacion Local, catalogo, multi-store y solicitud de repartidor.

Incluye:

- pedidos propios;
- catalogo;
- validacion por `storeId`;
- `store_driver_request` si B y M lo soportan.

Prohibido:

- chat del modulo Comunicacion;
- finanzas completas;
- carrito multi-local.

Acepta si:

- Store opera segun permisos y transiciones;
- no puede operar pedido ajeno.

Bloquea si:

- opera pedido ajeno;
- adelanta un estado invalido.

## E - Repartidor / Driver

Objetivo: operacion Driver y capacidad.

Incluye:

- tomar;
- retirar;
- entregar;
- capacidad;
- preparacion para cobro.

Prohibido:

- aprobar pagos dudosos;
- GPS o mapas como alcance base.

Acepta si:

- no hay doble toma;
- no hay entrega sin retiro.

Bloquea si:

- existe sobrecapacidad;
- se permite entrega invalida.

## G - Pagos, tarifas y finanzas

Objetivo: eje financiero, tarifas snapshot, cobro y cierre caja.

Incluye:

- estados financieros;
- cobro en entrega;
- cierre caja;
- tarifas y modos versionados.

Prohibido:

- pasarela online;
- IA aprobando pagos;
- cierre dudoso automatico.

Acepta si:

- entregado no cierra financieramente por error;
- los precios no cambian retroactivamente.

Bloquea si:

- pago dudoso cierra solo;
- una tarifa viva muta.

## I - Incidencias, reclamos y cancelaciones

Objetivo: excepciones operativas y reclamos posteriores.

Incluye:

- incidencias;
- reclamos;
- cancelaciones por rol;
- producto no disponible.

Prohibido:

- reabrir pedido cerrado.

Acepta si:

- incidencia y reclamo quedan separados;
- cancelacion registra evento e impacto cuando aplique.

Bloquea si:

- reclamo reabre pedido;
- cancelacion no define impacto requerido.

## J - Comunicacion

Objetivo: WhatsApp, chat interno, notificaciones y mensajes publicos.

Incluye:

- colas;
- fallback;
- permisos de chat;
- estado de comunicacion.

Prohibido:

- comunicacion como autoridad de estado.

Acepta si:

- comunica sin gobernar estados.

Bloquea si:

- texto, chat o WhatsApp cambian estados.

## K - IA controlada

Objetivo: IA asistiva y limitada.

Incluye:

- estructurar pedidos libres;
- sugerir;
- senalar riesgo;
- derivar a Admin.

Prohibido:

- IA decidiendo estados o pagos.

Acepta si:

- toda accion sensible requiere confirmacion o autorizacion backend.

Bloquea si:

- IA cancela, asigna, cierra o aprueba pagos dudosos sola.

## L - Metricas, auditoria y salud

Objetivo: metricas desde eventos y salud del sistema.

Incluye:

- agregacion;
- dashboard Admin;
- alertas;
- pedidos trabados.

Prohibido:

- metricas inventadas desde UI;
- metricas internas publicas.

Acepta si:

- las metricas trazan a eventos.

Bloquea si:

- no hay trazabilidad.

## O - Hardening

Objetivo: validar el sistema antes de release.

Incluye:

- todos los tests;
- guards;
- carga y estres;
- observabilidad;
- limpieza de datos demo;
- revision de logs sensibles.

Prohibido:

- publicacion Play;
- deploy produccion.

Acepta si:

- el sistema esta probado;
- no quedan placeholders operativos;
- no hay logs sensibles indebidos.

Bloquea si:

- falla un test critico;
- quedan datos demo en release.

## P - Release / Google Play

Objetivo: preparar release y Google Play.

Incluye:

- build release;
- AAB;
- checklist Play;
- Data Safety segun decisiones externas resueltas.

Prohibido:

- publicar sin autorizacion;
- inventar datos legales;
- avanzar sin hardening aprobado.

Acepta si:

- O esta aprobado;
- las decisiones externas de publicacion estan resueltas.

Bloquea si:

- falta titular legal;
- falta email oficial de soporte;
- falta aprobacion juridica;
- quedan placeholders.

## Paralelizacion

Permitido solo despues de certificar dependencias:

- C y F pueden avanzar parcialmente tras M.
- J y L pueden avanzar tras M.

No permitido:

- G antes de E y F;
- P antes de O;
- roles antes de B y M.
