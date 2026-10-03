package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.config.CacheConfig;
import com.threepartners.oncologia.domain.chatbot.ConfiguracionGemini;
import com.threepartners.oncologia.domain.chatbot.ConfiguracionGeminiRepositoryPort;
import com.threepartners.oncologia.infrastructure.out.security.CifradorSecretos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

/**
 * La configuracion se lee en cada mensaje del chatbot: se cachea y se
 * invalida al guardar (despues del commit, ver CacheConfig). La clave viaja
 * cifrada a la base y se descifra solo aqui.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfiguracionGeminiRepositoryAdapter implements ConfiguracionGeminiRepositoryPort {

    private final ConfiguracionGeminiJpaRepository jpaRepository;
    private final CifradorSecretos cifrador;

    @Override
    // Sin fila no se cachea (la cache no admite nulos): es una busqueda por clave primaria.
    @Cacheable(cacheNames = CacheConfig.CONFIGURACION_GEMINI, unless = "#result == null")
    public Optional<ConfiguracionGemini> obtener() {
        return jpaRepository.findById(ConfiguracionGeminiJpaEntity.ID_UNICO).map(this::aDominio);
    }

    @Override
    @CacheEvict(cacheNames = CacheConfig.CONFIGURACION_GEMINI, allEntries = true)
    public void guardar(ConfiguracionGemini c) {
        jpaRepository.save(ConfiguracionGeminiJpaEntity.builder()
                .id(ConfiguracionGeminiJpaEntity.ID_UNICO)
                .apiKeyCifrada(c.tieneClave() ? cifrador.cifrar(c.apiKey()) : null)
                .modelos(String.join(",", c.modelos()))
                .actualizadoPor(c.actualizadoPor())
                .actualizadoEn(c.actualizadoEn())
                .build());
    }

    private ConfiguracionGemini aDominio(ConfiguracionGeminiJpaEntity e) {
        String apiKey = null;
        boolean ilegible = false;
        if (e.getApiKeyCifrada() != null) {
            try {
                apiKey = cifrador.descifrar(e.getApiKeyCifrada());
            } catch (IllegalArgumentException ex) {
                // No se corta el chatbot: se usa la clave del entorno y el admin ve el aviso.
                log.error("No se pudo descifrar la clave de Gemini guardada; se usa GEMINI_API_KEY. "
                        + "Hay que volver a ingresarla en la intranet (cambio APP_CLAVE_CIFRADO o JWT_SECRET)");
                ilegible = true;
            }
        }
        var modelos = Arrays.stream(e.getModelos().split(",")).map(String::strip).filter(m -> !m.isEmpty()).toList();
        return new ConfiguracionGemini(apiKey, ilegible, modelos, e.getActualizadoPor(), e.getActualizadoEn());
    }
}
