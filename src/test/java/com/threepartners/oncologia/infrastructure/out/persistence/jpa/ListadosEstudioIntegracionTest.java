package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import com.threepartners.oncologia.domain.estudio.CanalMedicion;
import com.threepartners.oncologia.domain.estudio.Consulta;
import com.threepartners.oncologia.domain.estudio.EstadoMedicion;
import com.threepartners.oncologia.domain.estudio.MedicionRegistro;
import com.threepartners.oncologia.domain.estudio.PeriodoMedicion;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;
import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.soporte.PostgresIntegracionTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los listados del modulo de estudio aceptan cualquier combinacion de filtros
 * nulos (PostgreSQL no infiere el tipo de "(:p IS NULL OR ...)" con p nulo).
 */
class ListadosEstudioIntegracionTest extends PostgresIntegracionTest {

    @Autowired
    private MedicionRegistroRepositoryAdapter mediciones;
    @Autowired
    private ConsultaRepositoryAdapter consultas;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void limpiar() {
        jdbc.execute("TRUNCATE medicion_registro, consulta RESTART IDENTITY CASCADE");
        var inicio = Instant.parse("2026-09-10T15:00:00Z");
        mediciones.guardar(MedicionRegistro.manual(TipoMedicion.REGISTRO_CITA, null, inicio, inicio.plusSeconds(300), null, null));
        consultas.guardar(Consulta.manual(CanalConsulta.LLAMADA, null, "Horario", true, inicio, null, null));
    }

    @Test
    void listaMedicionesSinFiltrosYConFiltros() {
        var criterio = CriterioPaginacion.de(0, 20);
        assertThat(mediciones.listar(null, null, null, null, criterio).contenido()).hasSize(1);

        var septiembre = new PeriodoMedicion(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        assertThat(mediciones.listar(septiembre, TipoMedicion.REGISTRO_CITA, CanalMedicion.MANUAL,
                EstadoMedicion.COMPLETADA, criterio).contenido()).hasSize(1);
        assertThat(mediciones.listar(septiembre, null, CanalMedicion.INTRANET, null, criterio).contenido()).isEmpty();
    }

    @Test
    void listaConsultasSinFiltrosYConFiltros() {
        var criterio = CriterioPaginacion.de(0, 20);
        assertThat(consultas.listar(null, null, null, criterio).contenido()).hasSize(1);
        var octubre = new PeriodoMedicion(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        assertThat(consultas.listar(octubre, null, null, criterio).contenido()).isEmpty();
    }
}
