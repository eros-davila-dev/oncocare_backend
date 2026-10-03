package com.threepartners.oncologia.domain.chatbot;

import java.util.Optional;

public interface ConfiguracionGeminiRepositoryPort {

    Optional<ConfiguracionGemini> obtener();

    void guardar(ConfiguracionGemini configuracion);
}
