# Backend: Java 21 + Spring Boot 3.5, arquitectura hexagonal

Paquete raíz: `com.threepartners.oncologia`. Copia siempre la forma del módulo más parecido (`cita` es el más completo).

## Capas y dependencias

```
domain/<modulo>/          Entidades (Lombok @Builder/@Getter), enums, *RepositoryPort, *Port externos,
                          reglas puras (p. ej. cita.confirmar(), cita.marcarNoAsistio()).
                          Sin Spring, sin JPA, sin HTTP.
domain/shared/            Pagina, CriterioPaginacion, excepciones de negocio.
domain/auditoria/event/   Eventos de dominio (record) que disparan la auditoría.

application/<modulo>/     Un caso de uso por clase: XxxUseCase con @Service, @RequiredArgsConstructor,
                          @PreAuthorize por rol y @Transactional en el método público.
                          Orquesta dominio + puertos; publica eventos con ApplicationEventPublisher.

infrastructure/in/rest/   Controladores (@RestController, /api/v1/...), DTO record + Bean Validation,
                          mappers MapStruct en rest/mapper, GlobalExceptionHandler.
infrastructure/in/event/  Listeners asíncronos (auditoría).
infrastructure/out/       Adaptadores: persistence/jpa (JpaEntity + JpaRepository + RepositoryAdapter),
                          ai (Gemini), notification (n8n), security (JWT, BCrypt).
config/                   Security, CORS, filtros, @ConfigurationProperties.
```

Regla de dependencia: `infrastructure → application → domain`. El dominio nunca importa de las otras capas. Haz cumplir esto con un test **ArchUnit** (`ArquitecturaHexagonalTest`) para que no se degrade con el tiempo.

## Receta: nuevo módulo (ejemplo `estudio`)

1. `domain/estudio/`: `FaseEstudio`, `ParticipanteEstudio`, `MedicionRegistro`, `Consulta`, enums (`Fase`, `TipoMedicion`, `CanalMedicion`, `EstadoMedicion`, `ResultadoConsulta`) y puertos (`MedicionRegistroRepositoryPort`, ...). Las fórmulas de indicadores como métodos puros (`IndicadorCalculo.tns(long ni, long ncc)` → `OptionalDouble`/`Double` nullable).
2. `application/estudio/`: `IniciarMedicionRegistroUseCase`, `CerrarMedicionRegistroUseCase` (lo invocan otros casos de uso dentro de su transacción), `RegistrarMedicionManualUseCase`, `ConsultarIndicadoresEstudioUseCase`, `ExportarDatosSpssUseCase`, `GestionarParticipantesUseCase`, `ImportarCitasPretestUseCase`.
3. `infrastructure/out/persistence/jpa/`: entidades JPA + repositorios + adaptadores. Consultas de agregación con JPQL o `@Query(nativeQuery = true)` devolviendo proyecciones; nada de traer todas las filas a memoria para contar.
4. `infrastructure/in/rest/EstudioController` + DTOs en `dto/estudio/` + mapper.
5. Migración nueva con el siguiente número libre (V5 = ShedLock y V6 = estudio ya existen; ver `indicadores-tesis.md` §6).
6. Tests: unitarios del caso de uso con Mockito (patrón de `AgendarCitaUseCaseTest`), unitarios de las fórmulas, integración con Testcontainers para las consultas de agregación.

## Patrones concretos de este proyecto

- **Usuario autenticado**: el controlador obtiene `AutenticacionActual.usuarioId()`, `.rol()`, `.ipOrigen(request)` y los pasa al caso de uso. El caso de uso nunca lee el `SecurityContext` directamente.
- **Propiedad del recurso**: si el rol es `PACIENTE`, verificar que la entidad le pertenece; si no, responder `RecursoNoEncontradoException` (no 403, para no revelar existencia). Ver `RegistrarAsistenciaCitaUseCase.verificarPropiedadSiEsPaciente`.
- **Llamadas externas fuera de transacción**: Gemini/n8n nunca dentro de `@Transactional` (ver comentario en `ChatbotOrquestadorUseCase.procesar`). Timeouts configurados en `spring.http.client`.
- **Modo degradado**: si una integración externa falla, el sistema responde algo útil y registra el error; nunca un 500 con stack trace.
- **Columnas generadas** (`duracion_segundos`): mapear con `@Column(insertable = false, updatable = false)` para que `ddl-auto: validate` no falle y Hibernate no intente escribirlas.
- **Tiempo**: inyectar `java.time.Clock` (bean `Clock.system(ZoneId.of("America/Lima"))`) en los casos de uso que sellan o comparan fechas; en tests usar `Clock.fixed`.

## Endpoints nuevos previstos (resumen)

| Módulo | Endpoints |
|---|---|
| Medición | `POST /mediciones/registro` (inicia sesión de medición); los `POST /citas` y `POST /pacientes` aceptan `medicionId` |
| Estudio | ver `indicadores-tesis.md` §7 |
| Consultas | `GET /consultas/bandeja` (escaladas), `PATCH /consultas/{id}/resolver`, `POST /chatbot/consultas/{id}/valoracion` |
| Agenda | `GET /citas/agenda?fecha=` (vista de recepción), `GET /citas/pendientes-cierre`, `PATCH /citas/{id}/atendida`, `PATCH /citas/{id}/no-asistio` |
| Recordatorios | `GET /recordatorios/pendientes` (n8n), `POST /recordatorios/{id}/resultado` (n8n) |
| Telegram | `POST /integraciones/telegram/enlace` (genera token de vinculación), `POST /integraciones/telegram/vincular` (n8n), `POST /integraciones/telegram/accion-cita` (n8n, botones), `DELETE /integraciones/telegram/vinculo` |
| Chatbot | `POST /chatbot/mensaje` acepta `canal` y, para Telegram, se invoca desde n8n con `X-Webhook-Secret` + `telegramChatId` |

Los endpoints que invoca n8n se autentican con `X-Webhook-Secret` (ya existe `WebhookSecretValidator`) **o** con la cuenta de servicio; documentar cuál usa cada uno. Para los nuevos, preferir el secreto compartido + lista de IPs, y crear un rol técnico `SERVICIO` en lugar de reutilizar `RECEPCIONISTA`.

## Jobs programados

`@Scheduled` + **ShedLock** (tabla `shedlock` en PostgreSQL) para que, con varias instancias del backend, cada job corra una sola vez:

- `MarcarMedicionesAbandonadasJob` (cada 15 min).
- `ProgramarRecordatoriosJob` (cada 15 min): crea filas en `recordatorio` para citas próximas según la política (72 h, 24 h, 2 h).
- `CitasPendientesCierreJob` (23:00) y `CierreAutomaticoInasistenciaJob` (diario, 48 h).
- `ConsultasSinRespuestaJob` (cada hora): escaladas > 48 h → `NO_RESUELTA`.

## Lecciones ya aprendidas en este repo

- **Filtros opcionales**: no uses `(:p IS NULL OR campo = :p)` en JPQL; PostgreSQL falla con "no se pudo determinar el tipo del parámetro" cuando `p` es nulo. Usa `JpaSpecificationExecutor` + `FiltrosJpa` (`infrastructure/out/persistence/jpa/FiltrosJpa.java`).
- **Agregaciones de indicadores**: SQL nativo con `NamedParameterJdbcTemplate` (`IndicadoresEstudioJdbcAdapter`), pasando instantes como `OffsetDateTime` en UTC.
- **JSON**: la aplicación omite nulos (`default-property-inclusion: non_null`); los DTO donde `null` significa algo ("sin datos") llevan `@JsonInclude(ALWAYS)`.
- **Spring Data no detecta repositorios anidados** dentro de otra clase: un archivo por repositorio.
- **Errores 4xx de Spring MVC**: `GlobalExceptionHandler` tiene un comodín `Exception`; los 4xx propios de Spring (parámetro faltante, enum inválido, JSON roto, 405, 404, 413, 415) se responden con su código y `SOLICITUD_INVALIDA`, y solo un error real da 500 (y se registra con traza). Si agregas un manejador, no rompas eso (`GlobalExceptionHandlerTest`).
- **Descargas de archivos**: el controlador devuelve `ResponseEntity<byte[]>` con `ContentDisposition.attachment()` y `CacheControl.noStore()`; el generador POI vive en `infrastructure/in/rest/archivo/` y recibe un record del caso de uso (ver `HojaExcel` para nulos, números y fechas).
- **Tests de integración**: extienden `soporte/PostgresIntegracionTest`; terminan en `Test` (Surefire no ejecuta `*IT`). Sin Docker, exporta `TEST_DB_URL=jdbc:postgresql://localhost:5432/oncologia_test` (el nombre debe contener `test`), `TEST_DB_USUARIO`, `TEST_DB_PASSWORD`.

## Errores y respuestas

Usar las excepciones existentes: `ValidacionDeNegocioException` (400/422), `ConflictoDeNegocioException` (409), `RecursoNoEncontradoException` (404), `RegistroNoDisponibleException` (503). El `GlobalExceptionHandler` las traduce a `ErrorResponseDto`. No crear respuestas de error ad hoc en los controladores.

## Comandos

```bash
cd oncocare_backend
./mvnw spring-boot:run           # requiere PostgreSQL (docker compose up postgres)
./mvnw test                      # unitarios + integración (Testcontainers necesita Docker)
./mvnw verify                    # + cobertura (añadir JaCoCo, meta ≥ 80 % en application/ y domain/)
```
