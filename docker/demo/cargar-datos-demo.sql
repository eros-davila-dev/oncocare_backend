-- DATOS DE DEMOSTRACION para probar el dashboard (tarjetas, "Dia por dia",
-- graficos). Son ficticios y aleatorios: NO son resultados de la tesis ni se
-- ajustan a ellos. Todo queda marcado para poder quitarlo:
--   * pacientes con documento DEMO-NNN y correo @demo.oncocare.test;
--   * registros, citas y consultas de esos pacientes, con la observacion
--     "DATOS DE DEMOSTRACION".
-- Mientras existan, el dashboard muestra un aviso. Para quitarlos:
--   quitar-datos-demo.sql (anula, no borra, lo que las protecciones del
--   estudio no permiten borrar).
--
-- Uso: docker exec -i oncocare-postgres-1 psql -U oncologia -d oncologia < cargar-datos-demo.sql

DO $$
DECLARE
    v_existentes  INTEGER;
    v_dia         DATE;
    v_paciente    BIGINT;
    v_cita        BIGINT;
    v_n           INTEGER := 0;
    v_inicio      TIMESTAMPTZ;
    v_seg         INTEGER;
    v_estado      TEXT;
    v_hora        TIME;
    v_r           DOUBLE PRECISION;
    v_canal       TEXT;
    v_categorias  TEXT[] := ARRAY['CITAS', 'HORARIOS', 'INFORMACION_INSTITUCIONAL', 'REQUISITOS', 'UBICACION',
                                  'SEGUIMIENTO_ADMINISTRATIVO', 'OTRO'];
    v_nombres     TEXT[] := ARRAY['Ana', 'Luis', 'Rosa', 'Jorge', 'Carmen', 'Pedro', 'Elena', 'Mario', 'Lucia', 'Raul'];
    v_medicos     BIGINT[];
    v_recepcion   BIGINT;
    v_abierta     TIMESTAMPTZ;
    v_resultado   TEXT;
    i             INTEGER;
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

    PERFORM setseed(0.42);
    -- Si antes se cargaron y quitaron, se sigue la numeracion (el documento es unico)
    SELECT coalesce(max(substring(documento_identidad FROM 6)::int), 0) INTO v_n
    FROM paciente WHERE documento_identidad ~ '^DEMO-[0-9]+$';

    -- Dias habiles de las ultimas 4 semanas
    FOR v_dia IN
        SELECT d::date FROM generate_series(current_date - 28, current_date - 1, interval '1 day') d
        WHERE extract(isodow FROM d) < 6
    LOOP
        -- Registros de pacientes nuevos por el personal (TPR)
        FOR i IN 1 .. (2 + floor(random() * 3))::int LOOP
            v_n := v_n + 1;
            v_inicio := (v_dia + time '08:30' + (random() * interval '8 hours')) AT TIME ZONE 'America/Lima';
            v_seg := 150 + floor(random() * 420)::int;
            INSERT INTO paciente (nombres, apellidos, documento_identidad, fecha_nacimiento, email, direccion,
                                  convenio_seguro, contacto_emergencia_nombre, activo, fecha_registro,
                                  tiempo_registro_segundos, acepta_recordatorios, medico_tratante_id)
            VALUES (v_nombres[1 + (v_n % 10)], 'Demo ' || lpad(v_n::text, 3, '0'), 'DEMO-' || lpad(v_n::text, 3, '0'),
                    date '1950-01-01' + floor(random() * 15000)::int, 'demo' || lpad(v_n::text, 3, '0') || '@demo.oncocare.test',
                    'Lima (demostracion)', 'PARTICULAR', 'Familiar demo', true,
                    (v_inicio + make_interval(secs => v_seg)) AT TIME ZONE 'America/Lima', v_seg, true,
                    v_medicos[1 + floor(random() * array_length(v_medicos, 1))::int])
            RETURNING id INTO v_paciente;
            INSERT INTO medicion_registro (tipo, canal, estado, usuario_id, paciente_id, inicio, fin, observacion)
            VALUES ('REGISTRO_PACIENTE', 'INTRANET', 'COMPLETADA', v_recepcion, v_paciente, v_inicio,
                    v_inicio + make_interval(secs => v_seg), 'DATOS DE DEMOSTRACION');  -- duracion_segundos la calcula la base
        END LOOP;

        -- Citas del dia (ausentismo) y sus recordatorios
        FOR i IN 1 .. (4 + floor(random() * 4))::int LOOP
            SELECT id INTO v_paciente FROM paciente WHERE documento_identidad LIKE 'DEMO-%' AND activo ORDER BY random() LIMIT 1;
            v_hora := time '08:00' + (floor(random() * 18) * interval '30 minutes');
            v_r := random();
            v_estado := CASE WHEN v_r < 0.70 THEN 'ATENDIDA' WHEN v_r < 0.90 THEN 'NO_ASISTIO' ELSE 'CANCELADA' END;
            INSERT INTO cita (paciente_id, medico_id, fecha, hora, tipo_consulta, estado, observaciones, origen,
                              fecha_creacion, fecha_hora_desenlace, desenlace_registrado_por)
            VALUES (v_paciente, v_medicos[1 + floor(random() * array_length(v_medicos, 1))::int], v_dia, v_hora,
                    'Control (demostracion)', v_estado, 'DATOS DE DEMOSTRACION', 'INTRANET',
                    (v_dia - 5 + time '10:00') AT TIME ZONE 'America/Lima',
                    CASE WHEN v_estado IN ('ATENDIDA', 'NO_ASISTIO') THEN (v_dia + v_hora + interval '20 minutes') AT TIME ZONE 'America/Lima' END,
                    CASE WHEN v_estado IN ('ATENDIDA', 'NO_ASISTIO') THEN v_recepcion END)
            RETURNING id INTO v_cita;
            IF random() < 0.75 THEN
                v_r := random();
                v_canal := CASE WHEN v_r < 0.6 THEN 'TELEGRAM' WHEN v_r < 0.85 THEN 'LLAMADA' ELSE 'CORREO' END;
                INSERT INTO recordatorio (cita_id, tipo, canal, programado_para, estado, intentos, enviado_en, respuesta, respondido_en)
                VALUES (v_cita, 'T24H', v_canal, (v_dia - 1 + v_hora) AT TIME ZONE 'America/Lima',
                        CASE WHEN random() < 0.93 THEN 'ENVIADO' ELSE 'FALLIDO' END, 1,
                        (v_dia - 1 + v_hora + interval '1 minute') AT TIME ZONE 'America/Lima',
                        CASE WHEN v_canal = 'TELEGRAM' AND random() < 0.55 THEN 'CONFIRMO' END,
                        CASE WHEN v_canal = 'TELEGRAM' THEN (v_dia - 1 + v_hora + interval '40 minutes') AT TIME ZONE 'America/Lima' END);
                -- sin respuesta no hay hora de respuesta
                UPDATE recordatorio SET respondido_en = NULL WHERE id = currval('recordatorio_id_seq') AND respuesta IS NULL;
                UPDATE recordatorio SET enviado_en = NULL WHERE id = currval('recordatorio_id_seq') AND estado = 'FALLIDO';
            END IF;
        END LOOP;

        -- Consultas al asistente (NCA)
        FOR i IN 1 .. (5 + floor(random() * 6))::int LOOP
            SELECT id INTO v_paciente FROM paciente WHERE documento_identidad LIKE 'DEMO-%' AND activo ORDER BY random() LIMIT 1;
            v_abierta := (v_dia + time '07:00' + (random() * interval '13 hours')) AT TIME ZONE 'America/Lima';
            v_r := random();
            v_resultado := CASE WHEN v_r < 0.62 THEN 'RESUELTA_BOT' WHEN v_r < 0.90 THEN 'RESUELTA_PERSONAL' ELSE 'NO_RESUELTA' END;
            INSERT INTO consulta (canal, sesion_id, paciente_id, intencion, resumen, resultado, abierta_en, cerrada_en,
                                  tiempo_primera_respuesta_ms, resuelta_por_usuario_id, observacion, turnos,
                                  ultima_actividad_en, categoria, derivada)
            VALUES (CASE WHEN random() < 0.55 THEN 'TELEGRAM' ELSE 'CHATBOT_WEB' END, 'demo-' || v_dia || '-' || i, v_paciente,
                    CASE WHEN v_resultado = 'RESUELTA_BOT' THEN 'GENERAL_QUERY' ELSE 'ESCALATE_TO_STAFF' END,
                    'Consulta de demostracion', v_resultado, v_abierta, v_abierta + interval '6 minutes',
                    1500 + floor(random() * 7500)::int,
                    CASE WHEN v_resultado = 'RESUELTA_PERSONAL' THEN v_recepcion END,
                    'DATOS DE DEMOSTRACION', 1 + floor(random() * 3)::int, v_abierta + interval '6 minutes',
                    v_categorias[1 + floor(random() * 7)::int], v_resultado <> 'RESUELTA_BOT');
        END LOOP;
    END LOOP;

    -- Algunas citas proximas, para la agenda
    FOR i IN 1 .. 8 LOOP
        SELECT id INTO v_paciente FROM paciente WHERE documento_identidad LIKE 'DEMO-%' AND activo ORDER BY random() LIMIT 1;
        INSERT INTO cita (paciente_id, medico_id, fecha, hora, tipo_consulta, estado, observaciones, origen)
        VALUES (v_paciente, v_medicos[1 + floor(random() * array_length(v_medicos, 1))::int],
                current_date + 1 + floor(random() * 6)::int, time '09:00' + (i * interval '30 minutes'),
                'Control (demostracion)', 'PROGRAMADA', 'DATOS DE DEMOSTRACION', 'INTRANET');
    END LOOP;

    INSERT INTO auditoria_accion (usuario_id, accion, entidad_afectada, entidad_id, valores_nuevos, ip_origen, fecha, resultado)
    VALUES (NULL, 'DATOS_DEMO_CARGADOS', 'SISTEMA', NULL, 'pacientes=' || v_n, 'script', now(), 'EXITO');

    RAISE NOTICE 'Datos de demostracion cargados: % pacientes ficticios', v_n;
END $$;
