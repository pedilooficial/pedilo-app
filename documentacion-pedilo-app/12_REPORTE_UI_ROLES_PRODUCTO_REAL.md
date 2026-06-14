# 12 - Reporte UI roles producto real

Fecha: 2026-06-12

## Resultado

**NO APTA para certificacion visual completa de uso humano controlado.**

La UI visible se mejoro en Publico, Local/Store y Repartidor/Driver, y las validaciones tecnicas pasan. Publico fue abierto y revisado visualmente en celular fisico con la APK final. No se certifica APTA porque no se pudieron abrir visualmente Admin, Local y Driver en la build final con credenciales/sesiones reales disponibles para esos roles.

## Pantallas incompletas detectadas

- Publico: el home no explicaba de forma inmediata el flujo real de pedir, cargar datos y seguir el pedido. Los estados sin catalogo eran demasiado secos.
- Local/Store: la entrada mostraba pedidos o vacio, pero no guiaba que hacer, no mostraba tablero de carga y ocultaba catalogo/finanzas cuando no habia pedidos.
- Repartidor/Driver: la entrada no separaba con suficiente claridad capacidad, caja, disponibles, asignados, retiro, entrega y estados vacios.
- Admin: ya contaba con navegacion de operacion, salud, configuracion y equipo cubierta por tests de UI; no se toco en esta pasada.

## Que se redisenio

- Home publico:
  - nueva tarjeta "Pedi sin vueltas" con pasos Local, Datos y Segui;
  - botones directos "Ver tienda" y "Ayuda";
  - estado sin catalogo con explicacion de entorno de prueba autorizado;
  - icono check propio para la accion de ayuda.
- Local/Store:
  - tablero de conteos: Nuevos, Prep., Reparto, Rev.;
  - tarjeta "Que hacer ahora" con pasos operativos;
  - estado vacio guiado;
  - catalogo propio visible tambien sin pedidos;
  - cada pedido muestra "Siguiente" derivado de `nextAllowedActions`;
  - detalle muestra siguiente paso segun acciones habilitadas por backend.
- Repartidor/Driver:
  - tablero de conteos: Disp., Asig., Retiro, Entrega, Rev.;
  - tarjeta "Que hacer ahora" con toma, retiro, entrega y caja;
  - estado vacio guiado;
  - secciones Disponibles y Asignados mas claras;
  - cada pedido muestra "Siguiente" derivado de `nextAllowedActions`;
  - detalle muestra siguiente paso segun acciones habilitadas por backend.

## Que se elimino

- No se agregaron datos demo ni fixtures visuales.
- Se evito mostrar modulos vacios como si tuvieran informacion real.
- Se corrigieron etiquetas que se cortaban en celular real.

## Publico

Completado:

- Home con busqueda, flujo guiado, tienda, ayuda, categorias, ofertas/locales reales o vacios explicados.
- Vacio de catalogo explica que falta cargar catalogo autorizado, sin inventar locales.
- La navegacion a tienda/ayuda queda visible desde el primer viewport.

Validado visualmente:

- APK final instalada en telefono `EH423L012409`.
- Home publico abierto sin crashes.
- Captura final revisada: no hay demo real, la guia de flujo no se corta y los vacios son comprensibles.

## Admin

Estado:

- Mantiene UI existente de operacion, salud, configuracion y equipo.
- Los tests `admin_visual_shell`, `admin_operation_alignment` y relacionados pasan.

No probado visualmente:

- No se abrio Admin en la build final por falta de sesion/credenciales disponibles despues del cierre a Publico.

## Local / Store

Completado:

- Entrada con tablero operativo.
- Guia de acciones del local.
- Estado vacio util.
- Catalogo propio visible aunque no haya pedidos.
- Pedidos muestran siguiente accion humana segun backend.
- Solicitud de repartidor sigue visible solo como accion habilitada dentro del pedido.

Probado:

- Compilado y cubierto por tests de contrato.
- Durante la sesion se observo pantalla Local en dispositivo con el nuevo tablero y estado vacio; luego se ajustaron etiquetas y se recompilo.

No probado visualmente en build final:

- No se pudo reabrir Local con credencial final despues del logout.

## Repartidor / Driver

Completado:

- Entrada con tablero de disponibles/asignados/retiro/entrega/revision.
- Guia de trabajo real.
- Estado vacio util.
- Caja operativa visible.
- Pedidos muestran siguiente accion humana segun backend.

Probado:

- Compilado y cubierto por tests de contrato.

No probado visualmente:

- No se abrio Driver en dispositivo porque no hubo credencial/sesion Driver disponible.

## Flujo que puede probar una persona real

Con catalogo y usuarios Firebase de prueba autorizados:

1. Publico abre home, entra a tienda, elige local real y carga pedido.
2. Publico consulta tracking y puede solicitar cancelacion cuando el backend lo permite.
3. Local ve tablero, pedido propio, siguiente accion y opera aceptar/preparar/listo/solicitar repartidor.
4. Driver ve disponibles/asignados, toma, retira, entrega y cierra caja operativa.
5. Admin supervisa operacion, salud, configuracion y equipo.

## Validaciones ejecutadas

| Validacion | Resultado |
| --- | --- |
| `node --test tests/*.test.js` | OK, 33/33. |
| `npm --prefix functions test` | OK, 33/33. |
| `npm --prefix functions run build` | OK. |
| `bash tools/guards/check_architecture.sh` | OK. |
| `bash tools/guards/check_ui_quality.sh` | OK. |
| `bash tools/guards/check_no_production_release.sh` | OK. |
| `./gradlew :app:compileDebugKotlin` | OK. |
| `./gradlew :app:assembleDebug` | OK. |
| `adb install -r app/build/outputs/apk/debug/app-debug.apk` | OK. |
| `adb shell am start -n com.pedilo.app/.MainActivity` | OK. |
| `adb logcat -d -t 1000 AndroidRuntime:E '*:S'` | OK, sin crashes en la ventana revisada. |

## No probado visualmente

- Admin final en dispositivo.
- Local final en dispositivo despues del ultimo rebuild.
- Driver final en dispositivo.
- Flujo multirol real con usuarios Auth y catalogo Firebase autorizado.
- Acciones reales contra pedidos vivos desde celulares distintos.

## Dictamen

La UI quedo mas cerca de un producto real y las pantallas operativas por rol tienen estructura visible, guia y acciones alineadas al backend. El dictamen sigue **NO APTA para certificacion visual completa** hasta abrir y recorrer los cuatro roles en la APK final con usuarios reales de prueba.
