package com.threepartners.oncologia.domain.chatbot;

import com.threepartners.oncologia.domain.estudio.CategoriaConsulta;

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
        String respuestaSugerida,
        CategoriaConsulta categoria,
        boolean respuestaConInformacionOficial
) {

    /**
     * Sin categoria y con la respuesta tomada como oficial: lo que usan las
     * acciones sobre citas (el texto lo reemplaza el backend con datos reales)
     * y el modo degradado.
     */
    public InterpretacionChatbot(Intencion intencion, Map<String, String> entidades, boolean listoParaEjecutar,
                                 String respuestaSugerida) {
        this(intencion, entidades, listoParaEjecutar, respuestaSugerida, null, true);
    }

    public String entidad(String clave) {
        String valor = entidades.get(clave);
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
