package com.threepartners.oncologia;

import com.threepartners.oncologia.soporte.PostgresIntegracionTest;
import org.junit.jupiter.api.Test;

/**
 * Levanta el contexto completo de Spring contra un PostgreSQL real: valida
 * que las migraciones Flyway sean coherentes entre si y con el mapeo de las
 * entidades JPA (spring.jpa.hibernate.ddl-auto=validate detecta cualquier
 * discrepancia).
 */
class OncologiaApplicationTests extends PostgresIntegracionTest {

    @Test
    void elContextoDeSpringCargaCorrectamenteConLasMigracionesAplicadas() {
    }
}
