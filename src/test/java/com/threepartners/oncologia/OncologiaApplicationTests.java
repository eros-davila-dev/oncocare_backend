package com.threepartners.oncologia;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Levanta el contexto completo de Spring contra un PostgreSQL real en un
 * contenedor Testcontainers (seccion 4): valida que las migraciones Flyway
 * (V1/V2) sean coherentes entre si y con el mapeo de las entidades JPA
 * (spring.jpa.hibernate.ddl-auto=validate detecta cualquier discrepancia).
 */
@Testcontainers
@SpringBootTest
class OncologiaApplicationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("oncologia")
            .withUsername("oncologia")
            .withPassword("oncologia");

    @DynamicPropertySource
    static void propiedadesDeConexion(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void elContextoDeSpringCargaCorrectamenteConLasMigracionesAplicadas() {
    }
}
