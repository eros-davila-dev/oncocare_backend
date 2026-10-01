package com.threepartners.oncologia.soporte;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base de los tests de integracion contra un PostgreSQL real (las consultas
 * de indicadores y los triggers de proteccion usan SQL especifico de
 * PostgreSQL, que H2 no reproduce).
 *
 * Origen de la base, en este orden:
 * 1. TEST_DB_URL / TEST_DB_USUARIO / TEST_DB_PASSWORD: una base existente y
 *    DEDICADA a tests (sus tablas se vacian entre pruebas). Util en equipos
 *    sin Docker.
 * 2. Testcontainers (postgres:16-alpine) si Docker esta disponible.
 * 3. Si no hay ninguna, los tests se omiten (no fallan) para no bloquear el
 *    build en una maquina sin base de datos.
 */
@SpringBootTest(properties = "app.jobs.habilitados=false")
public abstract class PostgresIntegracionTest {

    private static final String URL_EXTERNA = System.getenv("TEST_DB_URL");
    private static final PostgreSQLContainer<?> CONTENEDOR = iniciarContenedorSiHaceFalta();

    private static PostgreSQLContainer<?> iniciarContenedorSiHaceFalta() {
        if (URL_EXTERNA != null && !URL_EXTERNA.isBlank()) {
            return null;
        }
        if (!dockerDisponible()) {
            return null;
        }
        PostgreSQLContainer<?> contenedor = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("oncologia")
                .withUsername("oncologia")
                .withPassword("oncologia");
        contenedor.start();
        return contenedor;
    }

    private static boolean dockerDisponible() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            return false;
        }
    }

    @BeforeAll
    static void exigirBaseDeDatos() {
        Assumptions.assumeTrue(URL_EXTERNA != null || CONTENEDOR != null,
                "Sin Docker ni TEST_DB_URL: se omiten los tests de integracion con PostgreSQL");
        if (CONTENEDOR == null && !nombreDeBase(URL_EXTERNA).contains("test")) {
            // Los tests vacian tablas: nunca deben poder apuntar por error a la base de desarrollo.
            throw new IllegalStateException(
                    "TEST_DB_URL debe apuntar a una base dedicada cuyo nombre contenga 'test': " + URL_EXTERNA);
        }
    }

    private static String nombreDeBase(String url) {
        String sinParametros = url.split("\\?")[0];
        return sinParametros.substring(sinParametros.lastIndexOf('/') + 1).toLowerCase(java.util.Locale.ROOT);
    }

    @DynamicPropertySource
    static void propiedadesDeConexion(DynamicPropertyRegistry registry) {
        if (CONTENEDOR != null) {
            registry.add("spring.datasource.url", CONTENEDOR::getJdbcUrl);
            registry.add("spring.datasource.username", CONTENEDOR::getUsername);
            registry.add("spring.datasource.password", CONTENEDOR::getPassword);
        } else if (URL_EXTERNA != null) {
            registry.add("spring.datasource.url", () -> URL_EXTERNA);
            registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("TEST_DB_USUARIO", "postgres"));
            registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("TEST_DB_PASSWORD", ""));
        }
    }
}
