package com.threepartners.oncologia.domain.chatbot;

import java.util.Map;

/**
 * Salida de Gemini ya interpretada (seccion 13): la intencion detectada, las
 * entidades que pudo extraer del mensaje (fecha, hora, especialidad, motivo),
 * si considera que hay informacion suficiente para ejecutar la accion, y una
 * respuesta en lenguaje natural en espanol lista para mostrar (usada tal cual
 * en preguntas generales o de aclaracion; ignorada y reemplazada por el
 * backend cuando la accion requiere datos reales, ej. el listado de citas).
 */
public record InterpretacionChatbot(
        Intencion intencion,
        Map<String, String> entidades,
        boolean listoParaEjecutar,
        String respuestaSugerida
) {

    public String entidad(String clave) {
        String valor = entidades.get(clave);
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
