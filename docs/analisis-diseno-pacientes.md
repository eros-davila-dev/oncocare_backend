# Análisis de brecha visual — Pantalla Pacientes vs. mockup OncoCare

Comparación estructural, elemento por elemento, entre el mockup de referencia y la implementación actual (`pacientes-list.html` + componentes compartidos). Objetivo: identificar exactamente qué separa visualmente a ambas versiones antes de tocar código.

## 1. Estructura general de la página

**Mockup:** 3 bloques visualmente independientes, cada uno con su propia tarjeta (fondo blanco, `rounded-2xl`, `shadow-sm`), separados por un espacio consistente (~20px):
1. Tarjeta "hero" (título + descripción + ilustración).
2. Fila de 4 tarjetas de estadísticas, separadas entre sí.
3. **Una sola tarjeta grande** que contiene, apiladas con líneas divisorias internas: pestañas + botón, filtros, tabla, pie de paginación.

**Implementación actual:** el hero y las stat cards ya son tarjetas correctas. El problema real está en el bloque 3: hoy `ui-tabs` (sin tarjeta), `ui-data-table-toolbar` (tarjeta propia con borde) y `ui-data-table` (tarjeta propia con borde, incluye su paginación) se apilan como **tres tarjetas separadas con espacio entre ellas** (`space-y-4`), en vez de una sola tarjeta continua con divisores internos. Este es el motivo principal por el que "no se ve igual": el mockup no tiene 3 recuadros con espacio entre pestañas/filtros/tabla, tiene uno solo.

**Corrección:** envolver tabs + toolbar + tabla en un único contenedor `rounded-2xl border shadow-sm bg-white`, quitando el borde/sombra propios de `ui-data-table-toolbar` y `ui-data-table` cuando se usan en este contexto, y usando `border-b` entre secciones internas en vez de espacio exterior.

Como `ui-data-table` y `ui-data-table-toolbar` son componentes compartidos usados en 6 pantallas más, no se les puede quitar el borde/sombra global sin afectarlas. Solución: la tarjeta contenedora se agrega **en `pacientes-list.html`**, y las utilidades internas (toolbar/tabla) se insertan dentro sin su propio borde exterior mediante una clase de "modo embebido" opcional, o replicando su marcado sin el wrapper con borde solo en esta pantalla. Se opta por la opción más simple y de menor riesgo: dejar `ui-data-table-toolbar` y `ui-data-table` **tal cual** (no se tocan, cero riesgo para otras 5 pantallas) y en su lugar construir el contenedor unificado directamente en `pacientes-list.html`, ocultando visualmente los bordes duplicados con márgenes negativos no es viable de forma limpia — en su lugar, se acepta un cambio mínimo y seguro: se agrega un input opcional `envolver = input(true)` a ambos componentes que, en `false`, quita `rounded-2xl border shadow-sm` de su contenedor raíz (por defecto `true`, cero cambio visual en las otras 5 pantallas).

## 2. Tarjetas de estadísticas (stat cards)

**Mockup**, de arriba hacia abajo dentro de cada tarjeta:
1. Icono solo, en círculo de color suave, alineado a la izquierda.
2. A la derecha del icono (misma fila): número grande en negrita.
3. Extremo derecho de la fila: píldora de tendencia (flecha + porcentaje).
4. Debajo del número: etiqueta en gris ("Pacientes registrados") + "vs. mes anterior" en la misma línea o inmediatamente debajo.

**Implementación actual:** icono + etiqueta van juntos en la fila superior; el valor grande va debajo de ambos; la píldora de tendencia está arriba a la derecha (esto sí coincide); el texto de comparación va en una línea aparte debajo del valor. El orden visual (icono+etiqueta antes que el número) no coincide con el mockup (icono solo, número junto al icono).

**Corrección:** reordenar el template de `StatCardComponent`: icono solo en la fila superior (izquierda) + píldora de tendencia (derecha); número grande debajo, a ancho completo; etiqueta + "vs. periodo anterior" debajo del número en una sola línea de texto secundario.

## 3. Paginación

**Mockup:** pie de página con "Mostrando 1 a 5 de 1,254 resultados" a la izquierda, y a la derecha botones circulares de flecha (‹ ›) + píldoras numeradas (1, 2, 3, 4, 5, …, 251), la página activa en azul sólido.

**Implementación actual (`PaginationComponent`):** "Página X de Y · Z resultados" + dos botones de texto "Anterior"/"Siguiente". Estructuralmente distinto.

**Corrección:** reescribir `PaginationComponent` para mostrar el rango "Mostrando X a Y de Z resultados" y una fila de píldoras numeradas con elipsis cuando hay muchas páginas, más flechas circulares deshabilitables. Es un componente compartido por las 6 pantallas de listado: el contrato de entrada/salida (`totalElements`, `totalPages`, `pageNumber`, `cambiarPagina`) no cambia, así que las otras 5 pantallas se benefician del mismo rediseño sin código adicional.

## 4. Badges de estado

**Mockup:** cada badge lleva un pequeño ícono (check, reloj, etc.) antes del texto, no solo el texto.

**Implementación actual:** `ui-badge` ya soporta contenido proyectado libre (hoy se usa solo texto). No requiere cambios en el componente, solo agregar un `<ui-icon>` dentro del `<ui-badge>` en `pacientes-list.html` y desactivar el punto de color (`[conPunto]="false"`) para no duplicar el indicador.

## 5. Botón "+ Nuevo paciente"

**Mockup:** el botón vive en la fila de pestañas (extremo derecho), no en la tarjeta "hero" superior.

**Implementación actual:** el botón está dentro de `ui-page-header`, en la tarjeta hero.

**Corrección:** mover el botón a la fila de pestañas dentro de la tarjeta unificada del punto 1.

## 6. Formato de números

**Mockup:** "1,254" con separador de miles; "92%" con un decimal cuando aplica.

**Implementación actual:** los valores de `EstadisticasPacientes` se interpolan tal cual desde el backend (`1254`, sin separador). No es una variable "cruda" visible como tal (no hay ningún bug de texto sin traducir), pero sí falta el formato de miles que el ojo espera en un dashboard.

**Corrección:** aplicar `| number` (DecimalPipe) a los valores numéricos de las stat cards.

## 7. Filtros de la barra de herramientas

**Mockup:** Buscar + Tipo de cáncer + Estado + Género + Rango de fechas + Limpiar.

**Decisión (revisada — esta pasada prioriza la corrección visual, que es el reclamo actual):**
- **Tipo de cáncer**: `tipoCancer` es texto libre en el modelo (no un enum fijo), así que un dropdown real requeriría un endpoint nuevo de valores distintos existentes en la BD. Se **difiere** para no volver a ampliar el backend en esta pasada; queda como mejora pendiente documentada.
- **Estado**: no se duplica como dropdown — ya está cubierto por las pestañas (Todos/En tratamiento/Finalizados/Suspendidos); agregarlo sería redundante.
- **Género**: se omite — el modelo `Paciente` no tiene este campo; agregarlo sería un dato inventado.
- **Rango de fechas**: se omite en esta pasada por alcance; puede añadirse después sobre `fechaRegistro` o `fechaDiagnostico` si se pide explícitamente.

## 8. Resumen de cambios a implementar

| # | Cambio | Alcance |
|---|---|---|
| 1 | Input `envolver` en `ui-data-table-toolbar` y `ui-data-table` (default `true`, sin romper otras pantallas) | Compartido |
| 2 | Tarjeta unificada en Pacientes: pestañas+botón / filtros / tabla / paginación con divisores internos | Solo Pacientes |
| 3 | Reordenar `StatCardComponent`: icono solo arriba, número junto a tendencia, etiqueta+comparación debajo | Compartido (mejora todas las stat cards, incl. Dashboard) |
| 4 | Reescribir `PaginationComponent`: "Mostrando X a Y de Z" + píldoras numeradas | Compartido (6 pantallas) |
| 5 | Ícono dentro de `ui-badge` de Estado | Solo Pacientes |
| 6 | Mover botón "+ Nuevo paciente" a la fila de pestañas | Solo Pacientes |
| 7 | `| number` en valores de stat cards | Solo Pacientes (y Dashboard si aplica) |
| 8 | *(diferido)* Filtro real de "Tipo de cáncer" — requiere endpoint de valores distintos | Backend + Pacientes |

Ningún punto anterior toca autenticación, guards, modelos de Cita/Tratamiento/Usuario, ni los endpoints CRUD existentes de Paciente.

## 9. Segunda pasada — color uniforme y consistencia entre módulos

Feedback: la tarjeta superior de Pacientes usaba un degradado (`from-brand-50 via-white to-white`) en vez de un color plano, y el mismo tratamiento visual (tarjeta de encabezado + panel unificado) no estaba replicado en el resto de módulos.

**Cambios:**
- `ui-page-header` pasa a renderizar su propia tarjeta (`rounded-2xl border shadow-sm bg-brand-50`, color solido sin degradado) directamente en el componente compartido. Al vivir en un único lugar, los 7 usos existentes (Pacientes, Citas, Tratamientos, Usuarios, Dispositivos, Auditoría, Dashboard) quedan visualmente idénticos por construcción, no por copiar clases a mano.
- Pacientes: se retira el envoltorio a medida (degradado, ícono decorativo, frase) — ya no hace falta, la tarjeta viene del componente compartido. El botón "+ Nuevo paciente" vuelve al encabezado (mismo lugar que en los otros 5 módulos) y las pestañas quedan solas como primera fila del panel unificado.
- Citas y Auditoría: su `ui-data-table-toolbar` + `ui-data-table` (antes dos tarjetas separadas) pasan a compartir una sola tarjeta contenedora, igual que Pacientes.
- Tratamientos, Usuarios y Dispositivos no tienen barra de filtros — su tabla ya era una única tarjeta, así que la separación entre encabezado y tabla mejora automáticamente al pasar el encabezado a tener su propio fondo de color (antes no tenía tarjeta y se mezclaba visualmente con el fondo gris de la página).
- Se normalizó el espaciado exterior a `space-y-5` en las 6 pantallas de listado y el dashboard, para que la distancia entre encabezado y tabla sea idéntica en todos los módulos.

## 10. Tercera pasada — adopción del sistema de diseño TailAdmin

El usuario proporcionó una plantilla real (`free-angular-tailwind-dashboard-main`, TailAdmin Angular + Tailwind v4) y pidió copiar su diseño en general, separado por componentes reutilizables, para que todos los módulos compartan la misma vista.

**Estrategia**: en vez de tocar cada pantalla, se remapean los propios *tokens* de diseño que el código ya usa (`--color-brand-*`, `--color-slate-*`, `--color-success/warning/danger/info-*`, `--shadow-xs/sm/md/lg/xl`) a los valores exactos de TailAdmin en `styles.css`, y se ajustan los componentes compartidos (`shared/ui/*`, `shared/components/*`, `layout/shell`). Como los 6 módulos ya consumían estos componentes compartidos, el nuevo aspecto se propaga automáticamente sin editar cada pantalla — exactamente el patron de "componentes reutilizables" pedido.

**Cambios de tokens** (`styles.css`):
- Tipografia: Google Font "Outfit" (antes "Inter").
- Paleta `brand`: azul/indigo de TailAdmin (`#465fff` base) en vez del azul institucional anterior.
- Paleta `slate` (neutros): redefinida con los hex exactos de TailAdmin (antes era el slate por defecto de Tailwind).
- `success/warning/danger/info`: pasaron de un solo tono a escalas completas (25/50/100/500/600/700/950) usando los valores reales de TailAdmin (danger = su "error", info = su "blue-light").
- Sombras `shadow-xs/sm/md/lg/xl`: redefinidas con los valores sutiles de TailAdmin (antes las de Tailwind por defecto).
- Utilidades `.menu-item`, `.menu-item-active`, `.menu-item-inactive`, `.menu-item-icon-active/inactive`: copiadas tal cual de la plantilla para el sistema de navegacion del sidebar.

**Componentes actualizados** (radio `rounded-lg` en botones/inputs, superficie `dark:bg-white/[0.03]` en tarjetas, anillo de foco suave `focus:ring-3 focus:ring-brand-500/20`, bordes `dark:border-slate-800` en contenedores estaticos vs `dark:border-slate-700` en controles interactivos, igual que hace la plantilla): `ui-button`, `ui-badge`, `ui-input`, `ui-select`, `ui-card`, `ui-tabs`, `ui-modal`, `stat-card`, `data-table`, `data-table-toolbar`, `pagination` (ahora con "Mostrando X a Y" resaltado y botones cuadrados en vez de circulares), `row-actions`, `toast-notification`, `appointment-calendar`, `page-header`, y el shell (sidebar/header) reescrito para usar las utilidades `.menu-item*` reales.

**Bug real encontrado y corregido**: existian *dos* componentes `ModalComponent` distintos — el bueno (`shared/ui/modal/modal.ts`, con `descripcion`/`ancho`) no se usaba en ningun lado; los 4 modulos que necesitaban esas props (Usuarios, Citas, Dispositivos, detalle de Paciente) importaban el viejo (`shared/components/modal/modal.ts`, sin esos inputs), asi que Angular silenciosamente ignoraba `descripcion="..."` y `ancho="sm"` como atributos HTML inertes (no genera error de compilacion porque no son bindings con corchetes). Se migraron los 4 consumidores al modal correcto y se elimino el duplicado — esto por si solo ya explicaba parte de la inconsistencia visual entre modulos.

**Lo que NO se copio de la plantilla** (fuera de alcance, requeriria funcionalidad que no existe): notificaciones, dropdown de aplicaciones, calendario FullCalendar, chat, kanban, y cualquier pagina de demostracion no relacionada con pacientes/citas/tratamientos.

## 11. Cuarta pasada — flujos de creacion/edicion consistentes via modal

Hallazgo: Usuarios y Dispositivos ya creaban registros mediante un modal (formulario embebido en la misma pantalla de listado), pero Pacientes, Citas y Tratamientos usaban paginas de ruta completa (`/pacientes/nuevo`, `/pacientes/:id/editar`, `/citas/nueva`, `/tratamientos/nuevo`) — dos flujos de creacion distintos conviviendo en la misma app. El usuario pidio unificar todo bajo el patron de modal.

**Cambios:**
- **Tratamientos** y **Citas**: la logica del formulario (FormGroup, validaciones, envio) se movio de su propio componente de ruta hacia el propio componente de listado (mismo patron que Usuarios/Dispositivos), con un `ui-modal` embebido en su `.html`. El campo "ID del paciente" (numero crudo que el usuario tenia que memorizar) se reemplazo por un `ui-select` real poblado desde `PacienteService.buscar('', 0, 100)` — sin esto, el flujo estaba incompleto: nadie conoce el ID interno de un paciente de memoria.
- **Pacientes**: dado que su formulario es mas complejo (pestañas, validador asincrono de documento unico, modo crear/editar), se convirtio `PacienteFormComponent` en un componente modal autocontenido (mismo patron que `PacienteDetalleComponent`): recibe `[pacienteId]`/`[abierto]` como inputs y emite `(guardado)`/`(cerrar)`, en vez de leer un parametro de ruta y navegar con el Router. `PacientesListComponent` lo invoca igual que a `PacienteDetalleComponent`, controlando su visibilidad con una señal.
- Se eliminaron las rutas `pacientes/nuevo`, `pacientes/:id/editar`, `citas/nueva`, `tratamientos/nuevo` de `app.routes.ts`, y las carpetas `cita-form`/`tratamiento-form` (su logica ya vive en el listado correspondiente).
- Resultado: las 5 pantallas con creacion/edicion (Pacientes, Citas, Tratamientos, Usuarios, Dispositivos) siguen ahora el mismo flujo — abrir modal, completar, guardar, toast de exito, cierre automatico y recarga del listado — usando el unico `ModalComponent` de `shared/ui/modal`.

## 12. Quinta pasada — sistema de diseño real (oncocare-design-system.zip)

El usuario proporciono el codigo fuente real del diseño (React 19 + Tailwind v4 + shadcn, generado con Lovable): `routes/index.tsx` (la pantalla de Pacientes completa), `components/layout/sidebar.tsx` y `styles.css`, con instruccion explicita de portarlo a Angular sin modificar el diseño.

**Diferencia clave con la plantilla TailAdmin adoptada en la pasada anterior**: esta fuente usa un sistema de tokens semanticos de un solo valor por rol (`primary`, `secondary`, `muted`, `accent`, `border`, `canvas`, etc., en formato oklch) en vez de una escala numerica 50-950. Se reemplazaron por completo los tokens anteriores (`--color-brand-*`, `--color-slate-*`) por este nuevo sistema, y se reescribieron los ~30 archivos que referenciaban los tokens viejos (grep confirmo 0 referencias residuales al finalizar).

**Cambios de tokens** (`styles.css`): tipografia Manrope (texto) + Nunito Sans (`font-display`, titulos), paleta oklch completa (`primary/secondary/muted/accent/destructive/border/input/ring/canvas/success/warning/info/lavender` + variantes `-soft`/`-hover`), radio proporcional derivado de una sola variable `--radius`, sombras semanticas `shadow-card/button/modal`, utilidades literales `.field` (inputs/selects), `.form-label`, `.metric-card`, `.status-badge` + `.status-success/info/warning/lavender` (se agrego `.status-danger` como extension consistente para el estado SUSPENDIDO, que la fuente no contemplaba).

**Componentes reescritos para usar el nuevo sistema** (ademas de los tokens, se adoptaron las clases y estructura literales del origen): `ui-button` (variantes primary/secondary/outline/ghost/danger/success = `buttonVariants` de origen), `ui-badge` (ahora renderiza `.status-badge`, con el mapeo EN_TRATAMIENTO→success/PENDIENTE→warning/FINALIZADO→lavender/SUSPENDIDO→danger tomado literalmente de los datos de ejemplo de la fuente), `ui-input`/`ui-select` (usan `.field`), `ui-modal` (header con borde inferior separado del cuerpo, boton de cierre circular, `shadow-modal`), `ui-card`, `ui-tabs`, `ui-avatar` (iniciales sobre `bg-secondary`), `ui-page-header` (ahora es literalmente la seccion `bg-welcome`: eyebrow + titulo `font-display` + icono decorativo + slot `[metricas]` para anidar las stat-cards, igual que en el origen), `stat-card` (espejo de `.metric-card`, ya no tiene variantes de color por metrica: la fuente usa siempre `bg-primary-soft`), `data-table`/`data-table-toolbar`/`pagination` (clases y estructura exactas de la tabla e footer de `routes/index.tsx`), `row-actions`, `toast-notification`, `confirm-dialog`, `empty-state`, `skeleton-rows`.

**Shell reescrito** para replicar `sidebar.tsx` literalmente: sidebar colapsable (64→20 unidades de ancho, no solo mobile-drawer), logo que oculta el texto al colapsar, grupos con mayusculas exactas ("GESTIÓN", "CONFIGURACIÓN"), tarjeta de cita ("Juntos por más historias de vida.") en `bg-sidebar-accent`, boton de colapsar en la base. El header ahora incluye la barra de busqueda y el icono de notificaciones que trae la fuente (ambos decorativos/no funcionales tambien en el codigo original — no se inventó una funcionalidad nueva, solo se replico fielmente lo que el diseño real ya muestra).

**Se eliminaron 2 archivos de codigo muerto** encontrados durante esta pasada: `shared/components/patient-card` (componente sin ningun uso) y se confirmo (de la pasada anterior) que `shared/components/modal` ya habia sido removido.

**Limites respetados de la pasada anterior, no revertidos por "no modificar nada"**: los filtros decorativos de Tipo de cáncer/Estado/Género/rango de fechas y la columna de checkboxes de seleccion masiva que aparecen en `routes/index.tsx` NO se replicaron, porque son controles sin ningun manejador (`onChange`/`onClick`) tambien en el codigo fuente original — agregarlos habria creado controles reales que aparentan funcionar pero no hacen nada, lo cual choca con el principio de no fabricar funcionalidad falsa sostenido durante toda la sesion. Esto es una excepcion deliberada, no un olvido.

## 13. Sexta pasada — QA: header amontonado, datos crudos, contraste en oscuro

**Header amontonado**: el header usaba `grid-cols-[auto_minmax(0,1fr)_auto]` con 3 hijos directos (copiado del origen), un mecanismo fragil ante ciertos anchos de viewport. Se reemplazo por un `flex` simple y robusto (`shell.html`): hamburguesa → breadcrumb (`min-w-0`+`truncate`) → buscador (`mx-auto`, oculto hasta `md:`) → iconos a la derecha via `ml-auto`. Tambien se elimino un `<span class="sr-only">` redundante en el buscador (la etiqueta accesible ahora es un `aria-label` directo en el input) que podia leerse como texto duplicado.

**Datos crudos encontrados y corregidos** (verificado con grep sobre las 7 pantallas de listado):
- Auditoria: `Accion` (`LOGIN_EXITOSO`), `Entidad` (`USUARIO #1`) y `Resultado` (`EXITO`) se mostraban como el valor crudo del backend. Se agrego `formatoEtiquetaEnum` (funcion plana extraida de `EtiquetaEnumPipe`, reutilizable fuera de plantillas) a las 3 columnas.
- Usuarios: `Rol` y `Especialidad` mostraban el enum crudo (`ADMIN`, `ONCOLOGIA_CLINICA`) → ahora usan `formatoEtiquetaEnum`.
- Tratamientos: `Tipo` y `Estado` crudos → `formatoEtiquetaEnum`; `Paciente` mostraba `#7` (ID crudo) → se agrego una carga anticipada (`cargarPacientes()` en el constructor, no solo al abrir el modal) que resuelve el nombre real via `PacienteService.buscar` con un `Map<id, nombre>`.
- Citas: mismo problema y misma solucion para la columna `Paciente`; `Medico` sigue mostrando `#N` (no hay un endpoint de "listar todos los medicos" sin especialidad) pero ahora rotulado como `Médico #N` en vez de un numero suelto sin contexto.
- Dispositivos: `Tipo` crudo (`MONITOR_SIGNOS_VITALES`) → resuelto contra las mismas opciones ya usadas en el formulario; `Estado de conexion` crudo → `formatoEtiquetaEnum`.

**Contraste en modo oscuro**: en `.dark`, `--primary` pasa a ser un azul claro y `--primary-foreground` a un tono casi negro (convención estándar para que un boton primario siga siendo legible sobre fondo oscuro). Dos lugares tenían texto `text-white` hardcodeado sobre un fondo `bg-primary` (el boton de cerrar del widget de chat, el ilustrador del login): se corrigieron a `text-primary-foreground` para que seguir siendo legibles en ambos modos en vez de depender de un blanco fijo que solo funcionaba en modo claro.

**Espaciado**: se aumento el espacio entre la tarjeta de encabezado y el panel/tabla de `space-y-5` a `space-y-6` en las 7 pantallas, y se agrego `break-words` a las celdas de la tabla para que el contenido largo se ajuste en vez de forzar scroll horizontal.

## 14. Septima pasada — bugs reales detectados con capturas de pantalla reales

Primera vez en la sesion con capturas del sistema en ejecucion (no el mockup de referencia), lo que permitio encontrar 2 bugs concretos en vez de ajustar a ciegas:

**Badge "↓NaN%"**: el backend omite del JSON los campos de variacion cuando son `null` (Jackson con inclusion non-null, ya confirmado en pruebas curl de una pasada anterior). Eso significa que en el frontend la propiedad llega como `undefined`, no `null`. `StatCardComponent` solo comprobaba `variacion() !== null`, que es `true` para `undefined` — renderizaba el badge y calculaba `Math.abs(undefined).toFixed(1)` = `"NaN"`. Corregido a `variacion() != null` (comparacion laxa, descarta ambos casos).

## 15. Octava pasada — usuario real en auditoria, pestañas de Citas, scroll del menu de acciones, paginado de 20

**Auditoria "Usuario"**: mostraba `Usuario #1` (ID rotulado). Se agrego una carga anticipada de `UsuarioService.listar(0, 100)` en el constructor, igual que el patron ya usado para resolver nombres de paciente en Citas/Tratamientos, para mostrar el nombre real con fallback a `Usuario #N` solo si el usuario no aparece en esa primera pagina.

**Pestañas de Citas**: el filtro de estado (Todas/Programada/Confirmada/Atendida/Cancelada/No asistio) usaba `ui-badge` en fila, un diseño distinto al de las pestañas de Pacientes. Se reemplazo por el mismo `ui-tabs` (con el mismo patron `TABS`/`ESTADO_POR_TAB` que ya usa Pacientes) dentro del mismo layout de fila (pestañas a la izquierda, botones de accion a la derecha, con `grid-cols-[minmax(0,1fr)_auto]`), quedando visualmente identico entre ambos modulos.

**Scroll no deseado al abrir el menu "⋮"**: causa real encontrada — la tabla usa `overflow-x-auto` para el scroll horizontal, y la especificacion CSS obliga a que si `overflow-x` no es `visible`, el `overflow-y` (que estaba en `visible` por defecto) se compute tambien como `auto`. Eso recortaba el menu desplegable de `ui-row-actions` (que era `position: absolute`) y disparaba un scroll vertical interno en la tabla en vez de superponerse a la pagina. Se corrigio calculando la posicion del menu en JS (`getBoundingClientRect` del boton) y renderizandolo con `position: fixed`, que no es recortado por el `overflow` de ningun ancestro. El menu ahora tambien se cierra si la pagina hace scroll o cambia de tamaño (las coordenadas fijas quedarian desactualizadas).

**Paginado de 20**: se creo `shared/constants/paginacion.ts` (`TAMANO_PAGINA_POR_DEFECTO = 20`) y se referencio desde los 5 servicios (`PacienteService`, `CitaService`, `TratamientoService`, `UsuarioService`, `AuditoriaService`), los 5 componentes de listado que inicializan su señal `pagina` con un tamaño fijo, y los valores por defecto de `DataTableComponent`/`PaginationComponent` — un unico lugar a cambiar si se necesita otro tamaño en el futuro.

## 16. Novena pasada — boton Cancelar sin color, icono de fecha/hora negro

**Boton "Cancelar" invisible**: los 7 usos (Citas x2, Tratamientos, Usuarios, Dispositivos, formulario de Paciente, `confirm-dialog`) usaban `variante="ghost"` (sin fondo ni borde por defecto, solo texto gris — asi esta definido `ghost` en la fuente real tambien, es intencional para acciones secundarias de bajo enfasis como "Limpiar"). El boton "Cancelar" del modal de registro en la fuente real usa `variant="outline"` (con borde visible), no `ghost`. Se corrigieron los 7 usos a `variante="outline"` para que el boton de cancelar siempre se vea como un boton, no como texto suelto. Se dejo `variante="ghost"` donde corresponde (ej. "Limpiar" en Auditoria), que no es un boton de cancelar de formulario.

**Icono de fecha/hora negro en modo oscuro**: el icono del selector nativo de `<input type="date">`/`<input type="time">` (`::-webkit-calendar-picker-indicator`) lo dibuja el navegador, no la app — es negro fijo sin importar el tema de la pagina. Sobre el fondo oscuro de un campo en modo oscuro, quedaba casi invisible. Se agrego una regla global en `styles.css` (`.dark input[type="date"]::-webkit-calendar-picker-indicator { filter: invert(1) ... }`) que solo invierte el icono en modo oscuro, dejandolo igual en modo claro donde ya se ve bien.

## 17. Decima pasada — desplegable de fecha/hora sin tema, placeholders faltantes

**Desplegable de fecha/hora sin dark/light real**: el arreglo anterior (invertir el icono con `filter`) solo tocaba el icono, no el calendario/popup que abre el navegador al hacer clic. Ese popup es renderizado enteramente por el navegador (no por CSS de la app) y su tema depende de la propiedad `color-scheme`: sin declararla, el navegador siempre lo dibuja en claro sin importar el tema de la pagina. Se agrego `color-scheme: light` en `:root` y `color-scheme: dark` en `html.dark` (el mismo selector que ya usa `ThemeService` para alternar tema) — con eso el calendario, el selector de hora, los scrollbars nativos y el autocompletado del navegador respetan el tema automaticamente. Se elimino el hack anterior de invertir el icono a mano porque ahora es redundante (y quedaria doblemente invertido).

**Placeholders faltantes en los formularios**: de 28 usos de `ui-input` y 13 de `ui-select` en todo el sistema, ninguno pasaba un `placeholder` explicito — el input lo soporta pero nadie lo llenaba, asi que los campos se veian vacios sin ninguna pista de que escribir. En vez de agregar texto a mano en cada uno de los ~40 usos, se agrego un valor por defecto calculado dentro de `ui-input` y `ui-select` (`placeholderEfectivo`): si no se pasa un placeholder explicito, se deriva uno del `label` ("Ingresa {label}" / "Selecciona {label}"). Al vivir en los dos componentes compartidos, los ~40 campos del sistema quedan con placeholder sin tocar cada formulario, y cualquier campo nuevo lo hereda automaticamente.

**Causa raiz probable de todos los reclamos de "separacion" anteriores**: los componentes Angular (`ui-page-header`, `ui-data-table`, `ui-card`, etc.) no tenian ningun estilo de host propio. Un elemento personalizado sin CSS es tratado por el navegador como una etiqueta desconocida, cuyo valor por defecto es `display: inline`. El margen vertical (`margin-top`/`margin-bottom`, que es lo que usa `space-y-*` de Tailwind para separar hermanos) **no tiene efecto en cajas inline** — por eso el espacio entre la tarjeta de encabezado y el panel de abajo podia no verse pese a que la clase `space-y-6`/`mt-6` estaba presente en el HTML. Se corrigio agregando `host: { class: 'block' }` (o `'contents'`/`'inline-flex'` segun el caso) a `ui-page-header`, `ui-data-table`, `ui-data-table-toolbar`, `ui-card`, `ui-stat-card`, `ui-tabs`, `ui-avatar` y `ui-modal`. Ademas, se reemplazo `space-y-6` (que depende de un selector de hermanos que puede comportarse de forma inesperada junto a bloques `@if`/`@for` de Angular) por `mt-6` explicito en el segundo elemento de cada pantalla — mas simple de verificar y depurar.

**Modo oscuro sin separacion visual real** (la causa raiz de "falta diseño dark/light" + "no hay separacion en esos dos card" eran el mismo bug): en `.dark`, `--canvas` (fondo de pagina) quedaba oklch 0.16 — mas CLARO que `--background`/`--card` (0.129), la relacion invertida respecto al modo claro (donde `--background` blanco puro es mas claro que `--canvas` gris). Con esa inversion, las tarjetas no se distinguian del fondo de la pagina en oscuro: mismo tono, ninguna separacion perceptible. Se corrigio la jerarquia completa de `.dark` para que `canvas` sea el mas oscuro y `background`/`card` un escalon mas claro (igual relacion que en claro, valores invertidos). Ademas, `.metric-card` (las 4 tarjetas de metricas) tenia un fondo blanco fijo (`oklch(1 0 0 / 88%)`) copiado tal cual del origen — invisible en oscuro (se veian como cajas grises claras flotando, exactamente lo que mostraba la captura). Se cambio a `var(--background)`, que ahora responde al tema correctamente.

## 18. Onceava pasada — rediseño del login

Pedido abierto ("Mejorar el diseño del login") sin captura de referencia; se trabajo sobre el layout dividido ya existente (panel de marca a la izquierda + formulario a la derecha), reforzando dos aspectos: coherencia con el lenguaje visual del resto de la app (iconos propios, no SVG suelto) y dos mejoras de usabilidad que faltaban en todo el sistema, no solo en el login.

**Panel de marca (izquierda)**: el corazon dibujado a mano en SVG inline (un elemento unico, fuera del sistema de iconos) se reemplazo por `ui-icon name="heart-pulse"` — el mismo componente de iconos que usa el resto de la app — a tamaño grande y opacidad baja como marca de agua decorativa, igual al patron ya usado en `page-header`. Se agrego una insignia/pill con el nombre de la fundacion y un anillo decorativo sutil (`border border-white/10`), y se ajusto la tipografia del titular a `text-3xl` para mayor jerarquia.

**Campos con icono inicial (mejora compartida)**: se agrego a `InputComponent` (`shared/ui/input/input.ts`) un input opcional `icono: NombreIcono | null`, que renderiza un `<ui-icon>` a la izquierda del campo con el padding ajustado automaticamente (`pl-10`). Se aplico en el login (`mail` en el correo, `lock` en la contraseña) pero, al vivir en el componente compartido, cualquier otro campo del sistema puede activarlo con el mismo input sin codigo adicional.

**Mostrar/ocultar contraseña (mejora compartida, no solo del login)**: ningun campo de tipo `password` del sistema (login, y el modal de "Nuevo usuario") tenia forma de revisar lo escrito antes de enviarlo. Se agrego a `InputComponent` un boton de alternar visibilidad (icono `eye`/`eye-off`, se agrego `eye-off` a `icon-data.ts` porque no existia) que aparece automaticamente para cualquier campo con `type="password"`, controlado por una señal local (`mostrarTexto`) y un `tipoEfectivo` computado que cambia el `type` real del `<input>`. Cero cambios de plantilla necesarios en los formularios que ya usan `type="password"` — el toggle aparece solo.

**Mensaje de error mas visible**: el error general de login (credenciales invalidas) pasa de texto suelto en rojo a una franja con fondo `bg-destructive-soft` e icono de alerta, coherente con el resto de mensajes de estado de la app.

**Boton de envio**: se agrego un icono de flecha (`chevron-right`) junto al texto "Iniciar sesion", siguiendo el mismo patron boton+icono ya usado en otras acciones primarias del sistema (ej. "+ Nuevo paciente").

Verificado: build de desarrollo, build de produccion y suite de tests, los tres sin errores.

## 19. Doceava pasada — logo oficial de la fundacion

El usuario proporciono el archivo real del logo (`logo.png`, 1254×1254, fondo transparente confirmado por pixel de esquina con canal alfa 0) desde su carpeta de Descargas. Se copio a `frontend/public/logo-oncocare.png` (servido en `/logo-oncocare.png` via el glob `public/**/*` ya configurado en `angular.json`).

**Un unico punto de cambio**: `BrandLogoComponent` (`shared/ui/brand-logo/brand-logo.ts`) ya existia como el lugar centralizado para la marca (su propio comentario decia "unico lugar a tocar si mas adelante se reemplaza por un logo oficial"). Se reemplazo el icono+texto armado a mano por un `<img src="/logo-oncocare.png">` — el archivo real ya incluye el icono, el nombre "OncoCare" y el lema, asi que no hace falta reconstruirlo con texto HTML aparte (habria duplicado la marca). El componente solo decide el alto segun el contexto (`conTexto`/`conTagline`, mismos inputs que ya consumian `login.html` y `shell.html`, sin tocar esos archivos): compacto en el sidebar colapsado, mediano en el sidebar expandido y la version movil del login, grande en el panel izquierdo del login (donde el logo es el elemento principal de marca de la pantalla).

Al vivir en el componente compartido, el logo real quedo aplicado de una vez en las 3 ubicaciones donde ya se usaba `ui-brand-logo` (sidebar expandido/colapsado y las dos apariciones en el login) sin editar cada sitio.

Verificado: build de desarrollo, build de produccion y suite de tests sin errores; se confirmo que `logo-oncocare.png` queda copiado en `dist/frontend/browser/` tras el build de produccion.

## 20. Treceava pasada — logo centrado y mas grande, marco en el formulario del login

**Logo pequeño y descentrado**: `BrandLogoComponent` decidia su alto (`h-24`/`h-12`/`h-10`) pero ninguno de los 2 lugares donde aparece en el login lo envolvia en un contenedor que lo centrara horizontalmente — quedaba pegado al borde izquierdo del panel por ser el comportamiento por defecto de un elemento en un contenedor `flex-col`/bloque. Se envolvio en `<div class="flex justify-center">` en ambas apariciones del login (panel de marca izquierdo y version movil sobre el formulario) y se aumentaron los altos (`h-24`→`h-36` con lema, `h-12`→`h-14` sin lema, `h-10`→`h-11` colapsado) para que se vea proporcional al espacio disponible.

**Formulario sin marco**: el bloque de "Bienvenido de nuevo" + campos + boton flotaba directamente sobre el fondo de la pagina sin ningun borde o superficie propia, por lo que se percibia "pelado"/plano. Se envolvio en una tarjeta (`rounded-2xl border border-border bg-card shadow-card p-8 sm:p-10`), el mismo lenguaje visual que ya usa `ui-card` en el resto del sistema (borde + sombra + superficie `bg-card`), y se le dio al panel derecho un fondo `bg-canvas` (en vez de heredar el `bg-background` blanco del contenedor raiz) para que la tarjeta blanca se distinga del fondo en vez de fundirse con el.

**Separacion entre subtitulo y campos**: el espacio entre "Inicia sesión para continuar..." y el primer campo paso de `mt-7` a `mt-8`, y el espacio entre los propios campos/fila de opciones/boton de `space-y-4` a `space-y-5`, para que se perciba una jerarquia clara entre el texto introductorio y el formulario.

Verificado: build de desarrollo, build de produccion y suite de tests sin errores.

## 21. Catorceava pasada — logo del login aun mas grande

El aumento anterior (`h-24`→`h-36`) seguia sin percibirse como suficientemente grande. Se subio el bucket "con lema" a `h-56` (224px) — un salto notorio. No se toco el bucket intermedio (`h-14`, sin lema) porque lo comparten el encabezado del sidebar expandido y el logo movil del login, y el contenedor del sidebar tiene una altura fija `h-16` (64px) con `overflow-hidden`: agrandarlo ahi habria recortado el logo contra el borde del sidebar. En su lugar, el logo movil del login (antes en el bucket chico compartido con el sidebar) pasa a pedir explicitamente `[conTagline]="true"`, ganando el mismo tamaño grande que el panel izquierdo sin afectar al sidebar.

Verificado: build de desarrollo, build de produccion y suite de tests sin errores.

## 22. Quinceava pasada — logo del login al triple de tamaño, API de tamaño explicita

Pedido literal: triplicar el tamaño del logo del panel del login (de `h-56`/224px a 672px). Dada la magnitud del salto, los dos booleanos `conTexto`/`conTagline` que hasta ahora inferian el tamaño dejaron de alcanzar (solo permiten 3 combinaciones utiles, y las necesidades de tamaño ya divergian demasiado entre el sidebar colapsado, el sidebar expandido, el logo movil del login y el logo hero del login). Se reemplazo por un input explicito `tamano: 'compacto' | 'normal' | 'grande' | 'hero'` en `BrandLogoComponent`, con un mapa fijo alto-por-tamaño:
- `compacto` (`h-11`, sidebar colapsado) y `normal` (`h-14`, sidebar expandido) sin cambios de valor, solo de nombre.
- `grande` (`h-20`, 80px) — nuevo, usado en el logo movil del login (antes compartia bucket con el sidebar).
- `hero` (`h-[42rem]`, 672px, valor arbitrario porque excede la escala estandar de Tailwind que llega hasta `h-96`/384px) — usado solo en el panel izquierdo del login, el unico lugar que pidio el aumento.

Se decidio NO triplicar tambien el logo movil del login (que hasta la pasada anterior reusaba el mismo bucket que el hero) porque, a diferencia del panel izquierdo (ancho de hasta la mitad de un monitor de escritorio), el logo movil vive dentro de una tarjeta de `max-w-sm` (384px) — un logo de 672px ahi excederia el ancho del contenedor y el `max-width:100%` del preflight de Tailwind lo recortaria/deformaria contra un boton `w-auto` + altura fija. Se le asigno el bucket `grande` (80px), una mejora moderada y segura para ese contexto en vez de heredar ciegamente el tamaño del hero.

Se actualizaron los 3 puntos de uso (`shell.html`, y las dos apariciones en `login.html`) al nuevo input; se confirmo con grep que no quedo ningun uso residual de `conTexto`/`conTagline`.

Verificado: build de desarrollo, build de produccion y suite de tests sin errores; se confirmo con grep sobre el CSS compilado que la regla `height:42rem` del bucket `hero` quedo incluida en el bundle final.

## 23. Dieciseisava pasada — fondo del panel del login, tamaño del logo ajustado y distribucion del contenido

Tres problemas relacionados en el panel izquierdo del login:

**Fondo "celeste" apagaba el logo**: el panel usaba `bg-primary` (el azul institucional, tono medio-claro). Contra ese fondo, los tonos claros/celestes/lavanda del logo (los degradados de los corazones, la hoja, el texto "Care") perdian contraste y se veian "apagados". Se creo una utilidad nueva `bg-login-hero` en `styles.css` (mismo patron que `bg-welcome`, que ya existia para el encabezado de las paginas internas): un degradado oscuro navy → indigo → violeta (`oklch` con los mismos matices de tono que `--primary`/`--secondary`/`--lavender`, 258–300, pero mucho mas oscuro, 16–24% de luminosidad). Al ser oscuro y de la misma familia cromatica que el logo, el logo se ve nitido en vez de competir por el mismo rango tonal, y sigue combinando con el resto de la paleta de la app (no es un color inventado fuera de la familia de tonos ya establecida).

**Logo demasiado grande / mal distribuido**: el bucket `hero` de `BrandLogoComponent` bajo de `h-[42rem]` (672px) a `h-[30rem]` (480px) — mas chico pero se mantiene muy por encima del tamaño previo a la pasada de "triplicar". Ademas, el panel usaba `justify-between` entre 3 bloques (logo/texto/lista de beneficios): con el logo ocupando la mayor parte del alto disponible, el espacio restante se repartia de forma impredecible entre los otros dos bloques, produciendo una distribucion desprolija. Se cambio a un flujo natural: el logo lleva un `pb-8` fijo antes del bloque de texto (insignia + titulo + parrafo), y la lista de beneficios (Atención segura / Seguimiento continuo / Mejor calidad de vida) se ancla al fondo del panel con `mt-auto` + `pt-8`, en vez de depender de la distribucion automatica de `justify-between`. El resultado es predecible sin importar el alto exacto del logo: logo arriba centrado, texto inmediatamente debajo con espacio fijo, beneficios siempre pegados abajo.

Verificado: build de desarrollo, build de produccion y suite de tests sin errores.

## 24. Diecisieteava pasada — el logo dentro del anillo decorativo, no suelto

El panel del login ya tenia un anillo decorativo (`border border-white/10`, 420px) posicionado de forma independiente en el centro geometrico absoluto del panel (`top-1/2 left-1/2 -translate-x/y-1/2`), sin ninguna relacion con el logo (que vivia arriba, en el flujo normal). El usuario pidio que el logo quede centrado dentro de ese circulo — es decir, que ambos elementos formen una sola pieza visual (un sello/insignia), no dos elementos sueltos y desalineados.

**Cambio**: se elimino el `div` del anillo standalone (posicionado respecto a todo el panel) y se lo recreo envolviendo directamente al logo, en el mismo lugar donde el logo ya vivia en el flujo (arriba, antes del bloque de texto): `<div class="flex size-[420px] items-center justify-center rounded-full border border-white/10"><ui-brand-logo tamano="hero" /></div>`. Se mantuvo el tamaño original del anillo (420px, el mismo valor que ya traia el diseño) en vez de inventar uno nuevo. Se opto por esta posicion (arriba, en el flujo) en vez de mover el anillo+logo al centro geometrico absoluto del panel porque eso habria requerido sacar tambien el bloque de texto del flujo normal para evitar que se solapen con el anillo (un anillo de 420px de radio centrado en el 50% vertical del panel cubre una franja enorme, mas alta que el bloque de texto en viewports no muy altos) — riesgo de solapamiento sin poder verificarlo visualmente en este entorno.

**Tamaño del logo reducido para caber dentro del anillo**: el bucket `hero` bajo de `h-[30rem]` (480px, mas grande que el propio anillo) a `h-72` (288px, clase estandar de Tailwind, sin valor arbitrario), dejando ~66px de margen a cada lado dentro del anillo de 420px — un logo enmarcado con aire alrededor, no pegado al borde.

Verificado: build de desarrollo, build de produccion y suite de tests sin errores.

## 25. Dieciochoava pasada — quitar titular/parrafo, centrar el conjunto logo+anillo, duplicar el logo

**Se quito el titular y el parrafo** ("Juntos por más historias de vida" + el texto de acompañamiento) del panel izquierdo, dejando solo la insignia "Fundación Oncológica Three Partners" junto al logo. Al desaparecer ese bloque de texto (que antes forzaba una distribucion vertical en 2 secciones separadas: logo arriba, texto debajo), se libero espacio para centrar el conjunto logo+anillo+insignia como una sola composicion.

**Centrado real del conjunto**: el contenedor que agrupa anillo+logo+insignia paso a `flex flex-1 flex-col items-center justify-center`, ocupando todo el espacio vertical disponible entre el borde superior del panel y la lista de beneficios (que se mantiene abajo, ahora tambien centrada horizontalmente con `justify-center` en vez de alineada a la izquierda, para que el panel se vea como una sola composicion centrada en vez de una mezcla de bloques centrados y alineados a la izquierda).

**Logo al doble de tamaño**: el bucket `hero` paso de `h-72` (288px) a `h-[36rem]` (576px, el doble exacto). Como el logo ya no cabe dentro del anillo de 420px que se habia fijado en la pasada anterior, el anillo tambien crecio a `size-[640px]` para seguir enmarcando al logo con margen (en vez de dejar que el logo desborde el circulo, lo que habria roto el efecto de "sello" pedido explicitamente en la pasada anterior). No se duplico tambien el anillo de forma literal (420→840px) porque a ese tamaño no cabria en el ancho disponible del panel en la mayoria de resoluciones de escritorio comunes (una pantalla de 1440px de ancho deja ~640px utiles en el panel izquierdo); se eligio el tamaño minimo necesario para contener el logo duplicado con margen prolijo.

Verificado: build de desarrollo, build de produccion y suite de tests sin errores.
