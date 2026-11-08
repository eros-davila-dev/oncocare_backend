-- DATOS DE DEMOSTRACION solo para los modulos Pacientes, Citas y Tratamientos
-- (para mostrar el sistema funcionando). Son ficticios: NO son pacientes de la
-- fundacion ni resultados de la tesis.
--
-- A diferencia de cargar-datos-demo.sql, este script:
--   * no crea mediciones de registro ni consultas (las tablas del estudio);
--   * usa fechas desde el 1 de octubre de 2026, despues de las fases del
--     estudio (pretest: agosto; postest: setiembre), asi no entra en ningun
--     calculo de la tesis;
--   * deja acepta_recordatorios = false y sin correo: el sistema no intenta
--     enviarles recordatorios.
-- Todo queda marcado para poder quitarlo: documento DEMO-NNN y la observacion
-- "DATOS DE DEMOSTRACION" en citas y tratamientos. Para quitarlos:
--   quitar-datos-demo.sql
--
-- Uso: docker exec -i oncocare-postgres-1 psql -U oncologia -d oncologia < cargar-modulos-demo.sql

DO $$
DECLARE
    c_desde       CONSTANT DATE := date '2026-10-01';   -- primer dia despues del postest
    c_pacientes   CONSTANT INTEGER := 40;
    v_existentes  INTEGER;
    v_n           INTEGER;
    v_paciente    BIGINT;
    v_medico      BIGINT;
    v_medicos     BIGINT[];
    v_recepcion   BIGINT;
    v_registro    TIMESTAMP;
    v_dia         DATE;
    v_r           DOUBLE PRECISION;
    v_estado      TEXT;
    v_tipo        TEXT;
    v_total       INTEGER;
    v_inicio      DATE;
    v_paso        INTEGER;
    i             INTEGER;
    j             INTEGER;
    v_nombres     TEXT[] := ARRAY['María', 'José', 'Rosa', 'Juan', 'Carmen', 'Luis', 'Ana', 'Carlos', 'Julia', 'Jorge',
                                  'Elena', 'Pedro', 'Lucía', 'Miguel', 'Teresa', 'Víctor', 'Gladys', 'Raúl', 'Patricia', 'César'];
    v_apellidos   TEXT[] := ARRAY['Quispe', 'Flores', 'Sánchez', 'Rodríguez', 'García', 'Huamán', 'Mamani', 'Torres',
                                  'Chávez', 'Ramírez', 'Vargas', 'Castillo', 'Rojas', 'Mendoza', 'Gutiérrez', 'Paredes',
                                  'Espinoza', 'Salazar', 'Cárdenas', 'Ccori'];
    v_canceres    TEXT[] := ARRAY['Cáncer de mama', 'Cáncer de cuello uterino', 'Cáncer de próstata', 'Cáncer gástrico',
                                  'Cáncer de pulmón', 'Cáncer colorrectal', 'Leucemia', 'Linfoma', 'Cáncer de tiroides'];
    v_estadios    TEXT[] := ARRAY['I', 'II', 'IIA', 'IIB', 'IIIA', 'IIIB', 'IV'];
    v_convenios   TEXT[] := ARRAY['ESSALUD', 'SIS', 'EPS', 'PARTICULAR'];
    v_consultas   TEXT[] := ARRAY['Consulta oncológica', 'Control post tratamiento', 'Evaluación de resultados',
                                  'Primera consulta', 'Control de quimioterapia', 'Consulta de radioterapia'];
    v_tratamientos TEXT[] := ARRAY['QUIMIOTERAPIA', 'QUIMIOTERAPIA', 'RADIOTERAPIA', 'CIRUGIA', 'CUIDADOS_PALIATIVOS'];
BEGIN
    SELECT count(*) INTO v_existentes FROM paciente WHERE documento_identidad LIKE 'DEMO-%' AND activo;
    IF v_existentes > 0 THEN
        RAISE EXCEPTION 'Ya hay datos de demostracion cargados (% pacientes). Quitelos antes de volver a cargar.', v_existentes;
    END IF;

    SELECT array_agg(id ORDER BY id) INTO v_medicos FROM usuario WHERE rol = 'MEDICO' AND activo;
    SELECT id INTO v_recepcion FROM usuario WHERE rol = 'RECEPCIONISTA' AND activo ORDER BY id LIMIT 1;
    IF v_medicos IS NULL OR v_recepcion IS NULL THEN
        RAISE EXCEPTION 'Se necesita al menos un medico y una recepcionista activos';
    END IF;

    PERFORM setseed(0.2026);
    -- Si antes se cargaron y quitaron, se sigue la numeracion (el documento es unico)
    SELECT coalesce(max(substring(documento_identidad FROM 6)::int), 0) INTO v_n
    FROM paciente WHERE documento_identidad ~ '^DEMO-[0-9]+$';

    FOR i IN 1 .. c_pacientes LOOP
        v_n := v_n + 1;
        v_medico := v_medicos[1 + floor(random() * array_length(v_medicos, 1))::int];
        -- Registrado un dia habil entre el 1 y el 23 de octubre
        v_dia := c_desde + floor(random() * 23)::int;
        WHILE extract(isodow FROM v_dia) > 5 LOOP v_dia := v_dia + 1; END LOOP;
        v_registro := v_dia + time '08:30' + (random() * interval '8 hours');

        INSERT INTO paciente (nombres, apellidos, documento_identidad, fecha_nacimiento, telefono, direccion,
                              tipo_cancer, estadio_clinico, fecha_diagnostico, medico_tratante_id, convenio_seguro,
                              contacto_emergencia_nombre, activo, fecha_registro, acepta_recordatorios)
        VALUES (v_nombres[1 + floor(random() * 20)::int],
                v_apellidos[1 + floor(random() * 20)::int] || ' ' || v_apellidos[1 + floor(random() * 20)::int],
                'DEMO-' || lpad(v_n::text, 3, '0'),
                date '1950-01-01' + floor(random() * 18000)::int,
                '900' || lpad(v_n::text, 6, '0'),
                'Lima (demostración)',
                v_canceres[1 + floor(random() * array_length(v_canceres, 1))::int],
                v_estadios[1 + floor(random() * array_length(v_estadios, 1))::int],
                v_dia - (15 + floor(random() * 200)::int),
                v_medico,
                v_convenios[1 + floor(random() * 4)::int],
                'Familiar (demostración)',
                true, v_registro, false)
        RETURNING id INTO v_paciente;

        -- Citas: 1 a 2 ya pasadas (con desenlace) y 1 a 2 proximas
        FOR j IN 1 .. (1 + floor(random() * 2))::int LOOP
            v_dia := v_dia + 2 + floor(random() * 6)::int;
            WHILE extract(isodow FROM v_dia) > 5 LOOP v_dia := v_dia + 1; END LOOP;
            EXIT WHEN v_dia >= date '2026-10-24';
            v_r := random();
            v_estado := CASE WHEN v_r < 0.80 THEN 'ATENDIDA' WHEN v_r < 0.92 THEN 'NO_ASISTIO' ELSE 'CANCELADA' END;
            INSERT INTO cita (paciente_id, medico_id, fecha, hora, tipo_consulta, estado, observaciones, origen,
                              fecha_creacion, fecha_hora_desenlace, desenlace_registrado_por)
            VALUES (v_paciente, v_medico, v_dia, time '08:00' + (floor(random() * 18) * interval '30 minutes'),
                    v_consultas[1 + floor(random() * array_length(v_consultas, 1))::int], v_estado,
                    'DATOS DE DEMOSTRACION', 'INTRANET', v_registro AT TIME ZONE 'America/Lima',
                    CASE WHEN v_estado = 'CANCELADA' THEN NULL ELSE (v_dia + time '18:00') AT TIME ZONE 'America/Lima' END,
                    CASE WHEN v_estado = 'CANCELADA' THEN NULL ELSE v_recepcion END);
        END LOOP;
        FOR j IN 1 .. (1 + floor(random() * 2))::int LOOP
            v_dia := date '2026-10-26' + floor(random() * 32)::int;
            WHILE extract(isodow FROM v_dia) > 5 LOOP v_dia := v_dia + 1; END LOOP;
            INSERT INTO cita (paciente_id, medico_id, fecha, hora, tipo_consulta, estado, observaciones, origen, fecha_creacion)
            VALUES (v_paciente, v_medico, v_dia, time '08:00' + (floor(random() * 18) * interval '30 minutes'),
                    v_consultas[1 + floor(random() * array_length(v_consultas, 1))::int],
                    CASE WHEN random() < 0.5 THEN 'CONFIRMADA' ELSE 'PROGRAMADA' END,
                    'DATOS DE DEMOSTRACION', 'INTRANET', v_registro AT TIME ZONE 'America/Lima');
        END LOOP;

        -- Tratamiento: un esquema por paciente (sesiones pasadas realizadas, proximas programadas)
        v_tipo := v_tratamientos[1 + floor(random() * array_length(v_tratamientos, 1))::int];
        v_total := CASE v_tipo WHEN 'QUIMIOTERAPIA' THEN 6 WHEN 'RADIOTERAPIA' THEN 10 WHEN 'CIRUGIA' THEN 1 ELSE 4 END;
        v_paso := CASE v_tipo WHEN 'QUIMIOTERAPIA' THEN 14 WHEN 'RADIOTERAPIA' THEN 2 WHEN 'CIRUGIA' THEN 1 ELSE 7 END;
        v_inicio := v_registro::date + 3 + floor(random() * 7)::int;
        FOR j IN 1 .. v_total LOOP
            v_dia := v_inicio + (j - 1) * v_paso;
            WHILE extract(isodow FROM v_dia) > 5 LOOP v_dia := v_dia + 1; END LOOP;
            v_estado := CASE
                WHEN v_dia >= date '2026-10-25' THEN 'PROGRAMADO'
                WHEN random() < 0.93 THEN 'REALIZADO'
                ELSE 'SUSPENDIDO' END;
            INSERT INTO ciclo_tratamiento (paciente_id, tipo_tratamiento, numero_sesion, total_sesiones_esquema,
                                           fecha_sesion, medico_responsable_id, estado, observaciones)
            VALUES (v_paciente, v_tipo, j, v_total, v_dia, v_medico, v_estado, 'DATOS DE DEMOSTRACION');
        END LOOP;
    END LOOP;

    INSERT INTO auditoria_accion (usuario_id, accion, entidad_afectada, entidad_id, valores_nuevos, ip_origen, fecha, resultado)
    VALUES (NULL, 'DATOS_DEMO_CARGADOS', 'SISTEMA', NULL,
            'modulos pacientes/citas/tratamientos: ' || c_pacientes || ' pacientes ficticios desde ' || c_desde,
            'script', now(), 'EXITO');
END $$;
