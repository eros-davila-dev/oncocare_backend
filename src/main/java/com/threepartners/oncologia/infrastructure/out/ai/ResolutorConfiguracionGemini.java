package com.threepartners.oncologia.infrastructure.out.ai;

import com.threepartners.oncologia.domain.chatbot.ConfiguracionGemini;
import com.threepartners.oncologia.domain.chatbot.ConfiguracionGeminiRepositoryPort;
import com.threepartners.oncologia.domain.chatbot.ConfiguracionGeminiVigente;
import com.threepartners.oncologia.domain.chatbot.OrigenConfiguracionGemini;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Decide que clave y que modelos usa el chatbot en cada mensaje: lo que el
 * administrador guardo en la intranet tiene prioridad y, lo que falte, sale
 * de las variables de entorno (GEMINI_API_KEY, GEMINI_MODEL, GEMINI_MODELS).
 * Asi el sistema funciona recien instalado y el administrador puede cambiar
 * la clave o los modelos sin reiniciar el backend.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResolutorConfiguracionGemini {

    private final GeminiProperties propiedades;
    private final ConfiguracionGeminiRepositoryPort repositorio;

    public ConfiguracionGeminiVigente vigente() {
        Optional<ConfiguracionGemini> guardada = leerGuardada();
        boolean ilegible = guardada.map(ConfiguracionGemini::claveIlegible).orElse(false);

        String apiKey;
        OrigenConfiguracionGemini origenClave;
        if (guardada.map(ConfiguracionGemini::tieneClave).orElse(false)) {
            apiKey = guardada.get().apiKey();
            origenClave = OrigenConfiguracionGemini.INTRANET;
        } else if (propiedades.apiKey() != null && !propiedades.apiKey().isBlank()) {
            apiKey = propiedades.apiKey().strip();
            origenClave = OrigenConfiguracionGemini.ENTORNO;
        } else {
            apiKey = null;
            origenClave = OrigenConfiguracionGemini.SIN_CONFIGURAR;
        }

        List<String> modelos;
        OrigenConfiguracionGemini origenModelos;
        if (guardada.isPresent() && !guardada.get().modelos().isEmpty()) {
            modelos = guardada.get().modelos();
            origenModelos = OrigenConfiguracionGemini.INTRANET;
        } else {
            modelos = propiedades.modelosEnOrden();
            origenModelos = OrigenConfiguracionGemini.ENTORNO;
        }
        return new ConfiguracionGeminiVigente(apiKey, origenClave, ilegible, modelos, origenModelos);
    }

    /** Si la base no responde, el chatbot sigue con las variables de entorno. */
    private Optional<ConfiguracionGemini> leerGuardada() {
        try {
            return repositorio.obtener();
        } catch (RuntimeException e) {
            log.error("No se pudo leer la configuracion de Gemini de la base; se usan las variables de entorno", e);
            return Optional.empty();
        }
    }
}
