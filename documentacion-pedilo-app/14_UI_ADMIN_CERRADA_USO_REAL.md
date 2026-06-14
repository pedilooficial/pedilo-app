# 14 - UI Admin cerrada para uso real

Fecha: 2026-06-12

## Resultado

**NO APTA para uso humano real completo.**

La UI Admin quedo tecnicamente cerrada como mesa de trabajo operativa: muestra salud, prioridades, pedidos, configuracion y equipo con datos reales o bloqueos controlados. No se declara APTA porque no se pudo recorrer visualmente Admin en dispositivo/emulador con la cuenta indicada.

## Que estaba mal en Admin

- La pantalla principal mostraba Repartidores y Locales como modulos activos aunque no tenian fuente propia conectada.
- Habia textos genericos como "Sin datos" en lugares donde una persona Admin necesitaba entender si faltaba informacion, si habia un bloqueo o si debia revisar un pedido.
- El alta de cuentas aparecia con copy de funcion no disponible dentro de Equipo, lo que podia leerse como modulo roto.
- El detalle de pedido tenia faltantes poco humanos en entrega, historial, retiro y montos.

## Que se redisenio

- La entrada de Admin ahora prioriza una mesa de pedidos real:
  - Salud interna.
  - Prioridades.
  - Requieren accion.
  - Demorados.
  - Sin responsable.
  - Esperando local.
  - Pedidos por Hoy, Activos, Problemas, Cerrados y Revisar pedido.
- Las prioridades abren listas concretas de pedidos reales, no paneles abstractos.
- El detalle de pedido reemplaza faltantes genericos por mensajes humanos:
  - Revisar datos.
  - Sin responsable visible.
  - Sin movimientos cargados.
  - Dato de retiro no informado.
  - Monto no informado.
- Equipo conserva gestion real de usuarios existentes y deja el alta de cuentas fuera del flujo principal por seguridad.
- Los tests de Admin ahora verifican que el home no exponga Repartidores o Locales como modulos operables sin datos.

## Que se elimino

- Se elimino del home operativo el acceso visible a Repartidores y Locales como tarjetas activas sin datos.
- Se elimino el uso principal de "Sin datos" como respuesta operativa generica en Admin.
- Se elimino copy de "no disponible" en alta de cuentas como si fuera una funcion rota.
- No se agregaron datos demo, mocks ni fixtures visuales.

## Que quedo cerrado

- Admin entiende estado general de operacion desde salud, auditoria resumida y metricas.
- Admin ve prioridades accionables derivadas de pedidos.
- Admin abre listas de pedidos activos, demorados, sin responsable, con problemas y en revision.
- Admin abre un pedido y ve estado, ubicacion principal, secciones de resumen, operacion, entrega, pago, problemas, historial y opciones.
- Admin ejecuta solo acciones habilitadas por backend y con confirmacion.
- Admin ve errores humanos controlados.
- Admin gestiona configuracion operativa persistida.
- Admin gestiona usuarios existentes, roles y acceso activo/inactivo.
- Admin distingue informacion real, faltantes operativos y bloqueos controlados.

## Como opera ahora un Admin real

1. Entra a Admin y mira Salud interna para saber si hay alertas, incidentes, comunicacion fallida, finanzas o revision asistida.
2. Revisa Prioridades para atacar primero pedidos con accion requerida, demora, falta de responsable o espera del local.
3. Abre Pedidos para navegar Hoy, Activos, Problemas, Cerrados y Revisar pedido.
4. Entra a una lista y abre el pedido concreto.
5. En el pedido revisa estado, ubicacion actual y secciones de detalle.
6. Si backend habilita acciones, confirma la accion correspondiente con version esperada y motivo cuando aplica.
7. Usa Configuracion para pausar, activar modos operativos o habilitar/deshabilitar pedidos publicos.
8. Usa Equipo para activar/desactivar cuentas existentes o cambiar rol, sin crear altas inseguras desde la UI.

## Pantallas listas

- Operacion / Mesa Admin.
- Salud interna.
- Prioridades.
- Pedidos: Hoy, Activos, Problemas, Cerrados, Revisar pedido.
- Listas de pedidos filtradas.
- Detalle de pedido.
- Secciones del pedido.
- Configuracion operativa.
- Equipo y accesos existentes.

## Validacion visual

**No completada.**

Intentos realizados:

- `adb devices`: no habia dispositivos conectados.
- Se encontro AVD local `Pixel_6`.
- Se intento iniciar `/home/oem/Android/Sdk/emulator/emulator -avd Pixel_6 -no-window -no-audio -no-boot-anim`.
- El emulador fallo porque requiere aceleracion x86_64 y `/dev/kvm` no esta disponible: `VT disabled in BIOS or KVM kernel module not loaded`.

Por este bloqueo tecnico, Admin no fue recorrido visualmente con `javib184@gmail.com`. No corresponde declarar APTA.

## Validaciones tecnicas

Pasaron:

- `node --test tests/*.test.js` - OK, 33/33.
- `npm --prefix functions test` - OK, 33/33.
- `npm --prefix functions run build` - OK.
- `bash tools/guards/check_architecture.sh` - OK.
- `bash tools/guards/check_ui_quality.sh` - OK.
- `bash tools/guards/check_no_production_release.sh` - OK.
- `./gradlew :app:compileDebugKotlin` - OK.
- `./gradlew :app:assembleDebug` - OK.

## No probado

- Recorrido visual de Admin en dispositivo o emulador.
- Login real con `javib184@gmail.com`.
- Ejecucion visual de acciones sobre pedidos vivos reales.
- Confirmacion visual de Configuracion y Equipo en APK final.

## Dictamen

Admin queda tecnicamente cerrado y alineado a uso operativo real, pero **NO APTA para uso humano real completo** hasta completar recorrido visual Admin en un dispositivo o emulador funcional con KVM o hardware conectado.
