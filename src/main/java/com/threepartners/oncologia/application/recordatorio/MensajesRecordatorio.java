package com.threepartners.oncologia.application.recordatorio;

import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.recordatorio.TipoRecordatorio;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Textos de los recordatorios. Se arman en el backend (no en n8n) para que
 * sean consistentes y revisables, y con datos minimos: nombre de pila, fecha
 * y hora. Nunca diagnostico, tratamiento ni documento (Ley 29733: un celular
 * puede verlo otra persona).
 */
final class MensajesRecordatorio {

    private static final Locale ES_PE = Locale.forLanguageTag("es-PE");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES_PE);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", ES_PE);

    private MensajesRecordatorio() {
    }

    static String texto(TipoRecordatorio tipo, Paciente paciente, Cita cita) {
        String saludo = paciente.nombrePila().isBlank() ? "Hola" : "Hola " + paciente.nombrePila();
        String hora = HORA.format(cita.getHora());
        return switch (tipo) {
            case T72H, T24H -> "%s 👋 Te recordamos tu cita en la Fundación el %s a las %s. ¿Podrás asistir?"
                    .formatted(saludo, DIA.format(cita.getFecha()), hora);
            case T2H -> "%s, tu cita en la Fundación es hoy a las %s. ¡Te esperamos! Si no puedes venir, avísanos aquí."
                    .formatted(saludo, hora);
        };
    }

    static String fechaTexto(Cita cita) {
        return DIA.format(cita.getFecha());
    }

    static String horaTexto(Cita cita) {
        return HORA.format(cita.getHora());
    }
}
