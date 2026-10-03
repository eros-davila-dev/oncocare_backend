# Frontend: Angular + Tailwind CSS 4

> Versión: el repositorio ya está en Angular 21 (compatible con todo lo que se pedía para Angular 20: standalone, signals, control flow `@if/@for`, `inject()`). No bajar de versión; si la tesis menciona Angular 20, basta con actualizar el texto.

## Dos aplicaciones sobre el mismo código (implementado en la Fase 5)

El sistema tiene dos públicos con necesidades distintas. Separarlos reduce el bundle del paciente, permite desplegarlos en dominios distintos (y restringir la intranet por red/IP) y evita que el paciente vea pantallas del personal.

**Decisión tomada**: dos proyectos en `angular.json` (`portal`, `intranet`) que comparten `src/app`. No se usa `projects/*` ni una librería aparte: duplicaba configuración sin beneficio, y el bundle de cada app ya contiene solo lo que sus rutas cargan.

```
oncocare_frontend/src/
├── main.portal.ts / index.portal.html        entrada del portal   (ng serve portal   → 4200)
├── main.intranet.ts / index.intranet.html    entrada de la intranet (ng serve intranet → 4300)
└── app/
    ├── core/config/audiencia.ts      InjectionToken AUDIENCIA ('portal' | 'intranet')
    ├── core/config/proveedores.ts    configuracionAplicacion(rutas, audiencia): providers comunes
    ├── portal/                       portal.routes.ts, portal.config.ts, portal-layout, inicio,
    │                                 preguntas (FAQ pública); usa features/paciente-portal,
    │                                 features/auth y el widget features/chatbot
    ├── intranet/                     intranet.routes.ts, intranet.config.ts (layout/shell, dashboard,
    │                                 agenda, consultas, pacientes, citas, tratamientos, estudio...)
    ├── core/ shared/ features/ layout/   código compartido; cada app solo importa lo que enruta
```

Reglas:
- Una pantalla nueva se registra en **las rutas de una sola app**. Si la necesitan ambas (p. ej. login), el componente lee `inject(AUDIENCIA)` para variar textos o destino.
- `AuthService.perteneceAEstaAplicacion()` / `urlDeSuAplicacion()` / `rutaInicio()`: un PACIENTE solo inicia sesión en el portal y el personal solo en la intranet; el `authGuard` cierra la sesión si la cuenta no es de esta app. Es una separación de producto: la seguridad sigue en el backend (`@PreAuthorize`).
- `environment.portalUrl` / `environment.intranetUrl` para enlazar de una app a la otra.
- El widget del chatbot vive solo en el portal.
- Despliegue: `oncocare_frontend/Dockerfile` copia `dist/portal` y `dist/intranet`; `nginx.conf` sirve el portal en el puerto 80 y la intranet en el 81 (cabeceras de seguridad comunes en `nginx-comun.conf`, proxy `/api`). El backend debe tener ambos orígenes en `CORS_ALLOWED_ORIGINS`.

## Convenciones (las que ya usa el código)

- Componentes standalone, `inject()` en lugar de constructor, estado con `signal`/`computed`, `input()`/`output()`; `ChangeDetectionStrategy.OnPush` en todos los componentes nuevos.
- Servicios por feature (`features/<feature>/<feature>.service.ts`) que devuelven `Observable` tipado con los modelos de `core/models`. URL base desde `environment.apiUrl`.
- Formularios reactivos tipados; validación en tres niveles: formato (validators), unicidad asíncrona contra la API (`documento-unico.validator.ts`), errores del backend mapeados al control.
- Rutas lazy con `loadComponent`, `authGuard` + `roleGuard(...)`. Nuevo rol `INVESTIGADOR` para el módulo de estudio.
- Interceptores funcionales: `jwtInterceptor` (renovación automática con refresh token) y `errorInterceptor` (toast).
- Gráficos con ApexCharts (`ng-apexcharts`), como el dashboard actual.
- Tests con Vitest.
- El backend omite los campos `null` (Jackson `non_null`) salvo en DTO marcados `@JsonInclude(ALWAYS)`. En plantillas compara con `!= null` (cubre `undefined`), nunca `!== null`: un campo ausente rompía el dashboard.

## Tailwind 4 y diseño

- El tema está en `styles.css` con `@theme inline` y tokens semánticos oklch (`--primary`, `--muted`, `--success`, `--warning`, `--destructive`...). Modo oscuro redefiniendo las variables bajo `.dark`. Usa siempre las clases semánticas (`bg-card`, `text-muted-foreground`, `border-border`, `bg-success-soft`); nunca hex ni la paleta numérica de Tailwind, para que el modo oscuro y un futuro rebranding funcionen sin tocar componentes.
- Reutiliza `shared/ui` (button, card, input, select, modal, tabs, badge) y `shared/components` (data-table, page-header, stat-card, empty-state, skeleton-rows) antes de crear algo nuevo.
- Accesibilidad: el portal lo usan pacientes oncológicos, muchos adultos mayores. Texto base ≥ 16 px, contraste AA, botones grandes, foco visible, etiquetas en todos los campos, sin depender solo del color para estados.
- Mobile-first en el portal (la mayoría llegará desde el enlace de Telegram en el celular).

## Pantallas nuevas clave

### Intranet
- **Dashboard de indicadores (rediseño)**: tres tarjetas principales TPR (min), TNS (%), NCA (%) con selector de fase (Pretest / Postest / Comparativo) y alcance (Muestra / Global); cada tarjeta muestra el n y la variación vs. pretest. Debajo: tendencia semanal, desglose por canal, NCA vs NCA_auto, métricas de apoyo de recordatorios. Valores `null` se muestran como "Sin datos", nunca como 0.
- **Agenda del día (recepción)**: lista de citas de hoy con acciones grandes "Llegó" / "No asistió", indicador de confirmación por Telegram y recordatorios enviados.
- **Bandeja de consultas**: consultas escaladas por el chatbot, con historial de la conversación y acciones "Resolver" / "No resuelta".
- **Módulo de estudio**: fases, participantes (código, consentimiento, exclusión), las tres fichas de captura pretest, importación de hojas de cálculo con vista previa y errores por fila, exportación SPSS.

### Formularios que miden TPR
`paciente-form`, el formulario de nueva cita y `completar-perfil` deben:
1. Al abrirse en modo creación, llamar `POST /mediciones/registro` y guardar el `medicionId` en una signal.
2. Enviar `medicionId` al guardar.
3. Eliminar el cálculo actual con `Date.now()` y el campo `tiempoRegistroSegundos`.
Encapsularlo en un servicio `MedicionRegistroService` (`iniciar(tipo, canal)`) para no duplicar lógica.

### Portal
- Inicio público con información útil (horarios, servicios, preguntas frecuentes) y acceso al chatbot.
- Mis citas con confirmar / reprogramar / cancelar.
- **Vincular Telegram**: botón que obtiene el deep link `https://t.me/<bot>?start=<token>` y muestra QR + estado de vinculación.
- Widget de chatbot con botones 👍/👎 por respuesta y opción "Hablar con una persona".

## Comandos

```bash
cd oncocare_frontend
npm run start:portal           # http://localhost:4200
npm run start:intranet         # http://localhost:4300
npm test                       # Vitest (proyecto intranet; las specs cubren el código compartido)
npm run build                  # build de producción de ambas apps; sin warnings de presupuesto
```
