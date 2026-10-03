package com.threepartners.oncologia.domain.chatbot;

import java.util.List;

/**
 * Operaciones del administrador sobre la integracion con Gemini: ver la
 * configuracion vigente, consultar los modelos que la clave puede usar,
 * probar una clave y ver que modelos estan apartados por cuota.
 *
 * apiKey null en listarModelos/probar significa "la clave vigente".
 */
public interface AdministracionGeminiPort {

    ConfiguracionGeminiVigente vigente();

    List<ModeloGeminiDisponible> listarModelos(String apiKey);

    ResultadoPruebaGemini probar(String apiKey, String modelo);

    List<EstadoModeloGemini> estado(List<String> modelos);

    /** Olvida los modelos apartados: con otra clave u otra lista la cuota es otra. */
    void reiniciarRotacion();
}
