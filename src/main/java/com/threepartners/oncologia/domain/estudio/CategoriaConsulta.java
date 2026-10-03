package com.threepartners.oncologia.domain.estudio;

/**
 * Categorias de la ficha de registro de consultas (Anexo 2 de la tesis). El
 * pretest y el postest se comparan con las mismas categorias. Los mensajes
 * fuera de alcance no tienen categoria porque no son consultas.
 */
public enum CategoriaConsulta {
    CITAS,
    HORARIOS,
    INFORMACION_INSTITUCIONAL,
    REQUISITOS,
    UBICACION,
    SEGUIMIENTO_ADMINISTRATIVO,
    OTRO;

    /** Valor que devuelve Gemini; lo desconocido cae en OTRO. */
    public static CategoriaConsulta desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return valueOf(valor.strip());
        } catch (IllegalArgumentException e) {
            return OTRO;
        }
    }
}
