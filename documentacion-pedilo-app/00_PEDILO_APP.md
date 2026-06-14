# Pédilo App

Pédilo App es un sistema operativo de pedidos para coordinar usuarios, locales, repartidores y administracion sobre una misma entidad: el Pedido Vivo Universal.

La app permite crear pedidos, operarlos por roles, seguir su avance, gestionar excepciones, registrar eventos, separar el estado operativo del financiero y construir los modulos por etapas sin romper coherencia entre backend, datos y pantallas.

## Proposito

Pédilo resuelve la coordinacion completa de un pedido desde su creacion hasta su cierre operativo, financiero y administrativo. Su valor no esta en mostrar un catalogo aislado ni en derivar pedidos a un chat, sino en mantener un pedido vivo, trazable y gobernado por reglas comunes.

Cada rol ve una version adecuada del pedido y ejecuta solo las acciones que le corresponden. La autoridad de estado vive en backend. La interfaz muestra informacion, solicita acciones y recibe resultados validados.

## Principios

- Un pedido tiene una verdad operativa.
- Cada cambio relevante genera un evento.
- Cada accion valida rol, permiso, estado y version.
- La comunicacion informa, pero no gobierna el pedido.
- La IA asiste, pero no decide acciones sensibles.
- Un pedido cerrado no se reabre.
- Un reclamo posterior se vincula al pedido cerrado sin cambiar su cierre.
- Los cambios de configuracion afectan pedidos futuros; los pedidos vivos conservan snapshot.
- Las metricas nacen de eventos auditables.

## Roles

| Rol | Responsabilidad principal |
|-----|---------------------------|
| Usuario Publico | Crear pedidos, consultar tracking, cancelar cuando corresponda e iniciar reclamos posteriores. |
| Local / Store | Operar pedidos propios, aceptar o rechazar, preparar, marcar listo y gestionar catalogo. |
| Repartidor / Driver | Tomar pedidos permitidos, retirar, entregar, cobrar cuando aplique y cerrar caja en la etapa financiera. |
| Admin | Supervisar, intervenir, configurar, administrar roles, auditar y resolver casos. |

## Tipos de pedido

| Tipo | Alcance |
|------|---------|
| `local_order` | Pedido a local. |
| `direct_purchase` | Compra directa o plus. |
| `pickup_shipping` | Retiro y envio. |
| `store_driver_request` | Solicitud de repartidor desde local, auditada y gobernada por backend para prueba controlada. |

No se agrega otro tipo de pedido sin decision de producto.

## Modulos de la app

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

## Documentos de construccion

Leer en este orden:

1. `00_PEDILO_APP.md`
2. `01_PRODUCTO_ROLES_REGLAS.md`
3. `02_PEDIDO_VIVO_UNIVERSAL.md`
4. `03_ARQUITECTURA_TECNICA.md`
5. `04_ETAPAS_DE_CONSTRUCCION.md`
6. `05_DECISIONES_EXTERNAS.md`

## Limites generales

No se debe:

- inventar reglas de producto;
- mezclar responsabilidades entre roles;
- permitir que UI, chat, WhatsApp o IA sean autoridad del pedido;
- permitir que IA cancele, asigne, cierre, apruebe pagos dudosos o cambie estados sola;
- renombrar estados persistidos sin migracion y certificacion;
- desplegar produccion, publicar en Google Play o tocar datos reales sin autorizacion explicita;
- inventar razon social, titular legal, email oficial, textos legales definitivos ni datos comerciales oficiales;
- conservar placeholders o datos demo en builds de release.
