# Estado actual Pedilo

Este documento es la unica fuente documental vigente del repo activo. Fue escrito desde el codigo actual despues de reconstruir Admin Operacion y limpiar documentacion historica.

## Estado general

Pedilo es una app Android Compose con backend Firebase/Cloud Functions. El pedido vivo en `/orders` es la fuente operativa principal. El usuario publico puede crear pedidos y consultar tracking. Los roles internos Admin, Store/Local y Driver/Repartidor operan pedidos mediante acciones permitidas por backend.

## Modulos existentes

- Android app: `app/src/main/java/com/pedilo/app`.
- Usuario Publico: `ui/publicuser`, catalogo, creacion de pedidos y tracking publico.
- Admin: `ui/admin`, operacion, configuracion y equipo.
- Store/Local: flujo operativo interno para pedidos del local.
- Driver/Repartidor: flujo operativo interno para entrega.
- Core: modelos, adapters, puertos, runtime y use cases.
- Backend Firebase Functions: `functions/index.js`.
- Reglas/config Firebase: `firestore.rules`, `firestore.indexes.json`, `firebase.json`.
- Tests/guards: `tests/` y `tools/guards/`.

## Activo

- Creacion publica de pedidos locales y Plus mediante Cloud Functions.
- Tracking publico por numero.
- Operacion interna de pedidos vivos para Admin, Store y Driver.
- Eventos e incidencias bajo cada pedido.
- Admin Configuracion reconstruida desde cero para Tarifa envio, con persistencia real de Modo lluvia, tarifa lluvia, costo de envio y adicional por distancia; los pedidos nuevos con envio toman esa configuracion en su snapshot financiero.
- Admin Equipo para roles/accesos existentes.
- Guards de arquitectura, UI y flujos operativos.

## En construccion

- Preparacion productiva final: deploy real, observabilidad, carga, legal/privacidad, pasarela de pago y publicacion.
- Invitacion/alta segura de nuevas cuentas internas.
- Profundizacion de monitoreo operativo fuera del alcance local actual.

## Aprobado

- Usuario Publico activo y no modificado durante este saneamiento.
- Backend/functions/reglas funcionales conservados.
- Admin Operacion reconstruido como herramienta de trabajo por flujo humano, con prioridad visual, pulso operativo, grupos, pedidos, ficha humana y resolución guiada.
- Tests/guards vigentes conservados y actualizados para impedir regreso de la UI vieja de Operacion.

## No aprobado

- Documentacion historica como fuente de verdad.
- Reportes, capturas, auditorias o dictamenes viejos como referencia activa.
- Pantalla vieja de Admin Operacion basada en archivo/busqueda/historial.
- Detalle de pedido tecnico con etiquetas crudas visibles para Admin.
- Acciones internas crudas o todas las acciones mezcladas en una sola vista.

## Admin Operacion

Admin Operacion quedo reconstruido desde cero alrededor de pedidos vivos y trabajo guiado. La primera pantalla muestra siempre las seis cards principales y una banda de pulso operativo para leer bloqueos, esperas y entregas en curso:

- Pedidos con problemas.
- En espera de aceptacion.
- Aceptados.
- En preparacion.
- En camino.
- Entregados / cerrados con problemas.

Cada card muestra cantidad, prioridad, resumen de situaciones y resultado esperado. Las cards con casos abren grupos internos; los grupos con casos abren pedidos. Las cards vacías no invitan a navegar. Cada pedido abre una ficha humana y la ficha abre una resolución guiada solo cuando corresponde al problema o estado del pedido.

## Navegacion actual de Admin Operacion

1. Admin -> Operacion.
2. Card principal con `Ver grupos` cuando hay casos.
3. Grupo interno con `Ver pedidos`.
4. Pedidos del grupo.
5. Ficha humana del pedido.
6. Resolución guiada si corresponde.
7. Resultado.
8. Regreso a pedidos u Operacion para ver la etapa actualizada.

La ruta vieja de archivo operativo fue eliminada. No existe `OperationsArchive` como destino activo.

## Detalle y acciones guiadas

La ficha de pedido muestra informacion humana sin explicar arquitectura interna:

- Estado actual entendible.
- Problema actual cuando existe.
- Color propio del problema.
- Acciones de resolución visibles solo para ese problema.
- Ticket/resumen humano del pedido, incluyendo persona usuaria, telefono, local/origen, pedido solicitado, destino, pago, total y repartidor cuando esos datos existen.
- Historial reciente humano.

Las acciones no se muestran todas juntas. Cada problema expone solo opciones necesarias. Ejemplos vigentes:

- Local no responde: contactar local o cancelar pedido.
- Sin repartidor: buscar repartidor o asignar manualmente.
- Pago con conflicto: revisar pago o contactar persona usuaria.

La pantalla de resolución incluye objetivo, datos para resolver, canales reales cuando existen, sugerencias, alternativas y resultado final. WhatsApp se muestra como acción cuando hay teléfono disponible. Al resolver, el pedido debe cambiar de estado/color/lista segun la respuesta real y la recalculacion operativa. Las pantallas muestran continuidad humana de flujo: grupos, pedidos, ficha, resolución y resultado.

## Documentacion vieja eliminada

Se elimino del repo activo:

- `documentacion-pedilo-app/`.
- `reports/`.
- `reports.zip`.
- `Pedilo.concepto.md`.
- `firebase-debug.log`.

Tambien se retiro del README cualquier instruccion que apuntara a documentacion historica o reportes como bitacora activa.

## Codigo viejo eliminado

Se elimino arrastre de Admin Operacion anterior:

- Ruta `OperationsArchive`.
- Pantalla archivada/busqueda/historial de Operaciones.
- Cards y paneles legacy de Operacion.
- Detalle anterior basado en modo/presentacion tecnica.
- Helpers y composables no referenciados asociados a la UI anterior.
- Tests que exigian la interfaz vieja, reemplazados por guards de la navegacion nueva.

## Fuente vigente

Para futuros agentes, la fuente vigente es solamente:

- Codigo actual del repo.
- `ESTADO_ACTUAL_PEDILO.md`.
- README solo como puntero a este documento.

No usar archivos historicos externos, reportes eliminados, capturas, auditorias viejas o dictamenes anteriores para decidir el estado activo de Pedilo.
