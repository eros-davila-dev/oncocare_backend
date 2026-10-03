# OncoCare — Backend

Sistema web con chatbot para la gestion de pacientes de la Fundacion Oncologica Three Partners (tesis UCV, 2026): pacientes, citas, recordatorios por Telegram, chatbot con Gemini, consultas y los indicadores del estudio (TPR, TNS, NCA). Spring Boot 3.5 (Java 21) con arquitectura hexagonal.

El frontend (portal del paciente e intranet del personal) es un repositorio aparte: **oncocare_frontend**, en la carpeta hermana.

## Estructura

```
oncocare_backend/          (este repositorio)
  src/, pom.xml, Dockerfile  API REST: hexagonal (domain / application / infrastructure), JWT, Flyway
  docker/                    docker compose de desarrollo y produccion, Caddy, Prometheus, init de PostgreSQL
  n8n/                       Workflows de Telegram: chatbot, recordatorios y alertas (n8n/README.md)
  docs/                      Plan de implementacion por fases y guia de operacion
  scripts/                   Restauracion y simulacro de respaldos
  .claude/skills/            Guia del proyecto para Claude Code
oncocare_frontend/         (repositorio aparte) Angular 21 + Tailwind 4: portal (4200) e intranet (4300)
```

## Levantar todo con Docker Compose

Requiere las dos carpetas una al lado de la otra (`oncocare_backend` y `oncocare_frontend`); si el frontend esta en otra ruta, definir `FRONTEND_DIR` en `docker/.env`.

```bash
cd docker
cp .env.example .env   # ajustar secretos y puertos
docker compose up -d --build
```

- Portal del paciente: http://localhost:4200
- Intranet del personal: http://localhost:4300
- Backend (Swagger UI): http://localhost:8080/swagger-ui.html
- n8n: http://localhost:5678 (la primera vez pide crear la cuenta de propietario)

Los puertos publicados se cambian en `docker/.env` (`PORTAL_HOST_PORT`, `BACKEND_HOST_PORT`, `POSTGRES_HOST_PORT`, `N8N_HOST_PORT`...) si ya estan ocupados en tu maquina.

Usuario administrador inicial (creado por la migracion `V2__seed_usuario_admin.sql`):

- Email: `admin@threepartners.org`
- Password: `Admin123!`

**Cambiar esta contraseña inmediatamente en un entorno real.**

## Produccion

```bash
cd docker
cp .env.prod.example .env   # completar dominios y secretos
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

Caddy publica el portal, la intranet (restringida por IP) y n8n con TLS automatico; nada mas queda expuesto. Respaldos diarios, restauracion, monitoreo y actualizaciones: ver `docs/OPERACION.md`. En produccion el backend no arranca si el administrador conserva la contrasena de la semilla (definir `ADMIN_PASSWORD_INICIAL`).

## Desarrollo local sin Docker

Requiere JDK 21 y una instancia de PostgreSQL 16 corriendo localmente (o via `cd docker && docker compose up -d postgres`).

```bash
cp .env.example .env   # completar con valores reales; nunca se commitea
./mvnw spring-boot:run
```

Hay dos `.env` con propositos distintos: `.env` (raiz) lo lee el backend al correr con Maven; `docker/.env` lo lee docker compose. Ninguno se versiona.

`spring-dotenv` carga `.env` automáticamente al iniciar (sin Docker no hace falta exportar las variables a mano). El archivo `.env` está excluido en `.gitignore`; solo `.env.example` (sin secretos reales) se versiona.

El frontend se levanta desde su propio repositorio (ver el README de oncocare_frontend). El backend debe permitir sus dos origenes: `CORS_ALLOWED_ORIGINS=http://localhost:4200,http://localhost:4300`.

## Pruebas

```bash
./mvnw verify   # unitarias + integracion (Testcontainers, requiere Docker)
```

Sin Docker, las pruebas de integracion pueden usar un PostgreSQL local con una base cuyo nombre contenga `test` (se vacia en cada ejecucion):

```bash
export TEST_DB_URL=jdbc:postgresql://localhost:5432/oncologia_test TEST_DB_USUARIO=postgres TEST_DB_PASSWORD=...
./mvnw verify
```

## Que cubre esta implementacion

Ver `prompt-sistema-oncologico.md` para el detalle completo del prompt original. En resumen:

- **Backend**: arquitectura hexagonal (`domain` / `application` / `infrastructure`), autenticacion JWT con refresh token persistido y revocable (logout real, rotacion en cada `/auth/refresh`), bloqueo de cuenta tras intentos fallidos, autorizacion por rol via `@PreAuthorize`, auditoria inmutable basada en eventos de dominio asincronos, modulos de pacientes/citas/tratamientos con validacion exhaustiva, portal de autoservicio de pacientes (registro, verificacion de correo, recuperacion de contrasena, completar ficha clinica), chatbot con Gemini embebido en el backend (ver mas abajo), puerto generico de dispositivos IoT (con adaptador REST y MQTT deshabilitado por defecto), webhooks para n8n y dispositivos autenticados con secreto compartido, catalogo de errores uniforme, paginacion en todos los listados, OpenAPI/Swagger.
- **Frontend**: componentes standalone con signals, interceptores JWT con renovacion automatica, guards de autenticacion y rol (incluido el portal de paciente: `mis-citas`, `mi-perfil`, `completar-perfil`), formularios reactivos con validacion en tres niveles (formato, unicidad asincrona contra la API, errores del backend), componentes UI reutilizables (`shared/ui`, `shared/components`), tema claro/oscuro persistente, panel de indicadores con ApexCharts, widget de chatbot embebido conectado al backend propio.
- **Fuera de alcance** (segun el propio prompt, seccion 3): drivers reales de dispositivos, HCE con HL7/FHIR, facturacion, telemedicina. El adaptador MQTT y el webhook de dispositivos quedan implementados como contrato funcional, no contra hardware real.

### Chatbot con Gemini (seccion 13)

El widget del navegador habla con `POST /api/v1/chatbot/mensaje` (backend propio), nunca directo con Gemini ni con n8n:

```
Angular (widget) -> Backend (ChatbotController) -> GeminiPort (interpreta intencion)
                                                  -> Backend decide y ejecuta con los
                                                     mismos casos de uso que el resto
                                                     del sistema (agendar/reprogramar/
                                                     cancelar/confirmar/consultar citas)
                                                  -> PostgreSQL
```

Gemini solo clasifica el mensaje en una intencion cerrada (`Intencion.java`) y extrae entidades (fecha, hora, especialidad, motivo) devolviendo JSON estructurado (`generationConfig.responseSchema`); nunca ejecuta acciones ni toca la base de datos. El endpoint es publico (permite preguntas generales de un visitante anonimo), pero si la peticion trae sesion iniciada, el chatbot identifica al paciente y puede gestionar sus propias citas.

Para activarlo, consigue una clave gratuita en <https://aistudio.google.com/apikey> y define `GEMINI_API_KEY` en `.env` (`GEMINI_MODEL` es opcional, por defecto `gemini-3.6-flash`). **Sin la clave el chatbot sigue funcionando** y responde en modo degradado ("no puedo procesar tu mensaje ahora, un miembro del equipo te ayudara"), nunca se cae la peticion ni se expone un error tecnico.

También se puede configurar sin tocar el servidor: el **administrador** entra a la intranet, *Configuración > Asistente IA*, pega la clave (se guarda cifrada con `APP_CLAVE_CIFRADO`), consulta los modelos que esa clave puede usar, los prueba y los ordena. El chatbot usa el primero disponible y, si agota su cuota o no responde, pasa al siguiente. Lo guardado en la intranet tiene prioridad sobre `GEMINI_API_KEY` / `GEMINI_MODEL` / `GEMINI_MODELS`, que quedan como respaldo.

- **n8n + Telegram**: el backend decide que recordatorio enviar, a quien y cuando (72 h / 24 h / 2 h antes; llamada de recepcion si el paciente no vinculo Telegram); n8n solo entrega por Telegram y reporta el resultado. Los workflows estan en `n8n/workflows/`, listos para importar; ver `n8n/README.md` (crear el bot en @BotFather, credencial, secreto del webhook, tunel HTTPS). Los workflows de envio de correo del portal de autoservicio (verificacion de cuenta, recuperacion de contrasena) tampoco estan incluidos: requieren credenciales SMTP/proveedor de correo propias de la fundacion; el contrato exacto que el backend ya invoca esta documentado en `n8n/README.md`.

### Correo transaccional

El backend envia tres correos: **verificacion** al registrarse en el portal, **restablecer contrasena** y **bienvenida** cuando un administrador crea un usuario (con un enlace de un solo uso, 72 h, para que la persona elija su contrasena). Los enlaces llevan al portal si la cuenta es de paciente y a la intranet si es del personal.

- Proveedor principal: API de correo (`MAIL_FLAG_QUIPU=true`, `MAIL_API_URL`, `MAIL_API_KEY`). Respaldo opcional: SMTP (`MAIL_FLAG_SMTP=true`, `MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD`). Con ambos activos se intenta la API y luego SMTP. Remitente: `MAIL_FROM_EMAIL` / `MAIL_FROM_NAME`. Las plantillas llevan la identidad de OncoCare; el logo (`oncocare_frontend/public/logo-correo.png`, version optimizada del logo del login) se carga desde `MAIL_LOGO_URL`, por defecto `<portal>/logo-correo.png`: debe ser una URL publica, asi que en local no se ve en una bandeja real.
- Los correos pasan por el outbox: se envian solo si la operacion que los origina se guardo, se reintentan si el proveedor falla, y el correo y el enlace con token no quedan guardados tras el envio. Los logs no registran destinatarios.
- Si una bandera esta activa sin su configuracion, el backend no arranca (mejor que descubrirlo con el primer paciente sin correo).

### Registro de pacientes (seccion 7)

`REGISTRO_PACIENTES_HABILITADO` (`.env`) esta en `false` por defecto; con el correo configurado se pone en `true`. Mientras este en `false`, `POST /api/v1/auth/registro` responde 503 con un mensaje claro y **no crea ninguna fila en la base de datos** — evita cuentas huerfanas en `PENDIENTE_VERIFICACION` que nunca podrian activarse sin el correo de verificacion. El chatbot tambien lo respeta: si alguien le pide registrarse, responde que el registro estara disponible proximamente en vez de mandarlo a un formulario que va a fallar.

Con el correo configurado (arriba), pon `REGISTRO_PACIENTES_HABILITADO=true` en `.env` y reinicia el backend — no requiere ningun otro cambio.
