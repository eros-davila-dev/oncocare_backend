package com.threepartners.oncologia.infrastructure.out.persistence.jdbc;

import com.threepartners.oncologia.domain.dashboard.DatosDemoPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatosDemoJdbcAdapter implements DatosDemoPort {

    private final JdbcTemplate jdbc;

    @Override
    public int pacientesDemoActivos() {
        Integer n = jdbc.queryForObject(
                "SELECT count(*) FROM paciente WHERE activo AND documento_identidad ~ '^DEMO-[0-9]+$'", Integer.class);
        return n == null ? 0 : n;
    }
}
