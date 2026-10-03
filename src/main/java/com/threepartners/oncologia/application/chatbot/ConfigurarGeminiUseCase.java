package com.threepartners.oncologia.application.chatbot;

import com.threepartners.oncologia.domain.auditoria.event.OperacionAuditadaEvent;
import com.threepartners.oncologia.domain.chatbot.AdministracionGeminiPort;
import com.threepartners.oncologia.domain.chatbot.ConfiguracionGemini;
import com.threepartners.oncologia.domain.chatbot.ConfiguracionGeminiRepositoryPort;
import com.threepartners.oncologia.domain.chatbot.ConfiguracionGeminiVigente;
import com.threepartners.oncologia.domain.chatbot.EstadoModeloGemini;
import com.threepartners.oncologia.domain.chatbot.ModeloGeminiDisponible;
import com.threepartners.oncologia.domain.chatbot.ResultadoPruebaGemini;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * Configuracion del asistente desde la intranet: clave de API de Gemini y
 * modelos en orden de preferencia. Cuando un modelo agota su cuota el
 * chatbot pasa al siguiente (RotacionModelosGemini). Solo el administrador:
 * la clave da acceso a una cuenta de Google y cambiarla afecta al chatbot de
 * todos los pacientes.
 *
 * La clave nunca vuelve al navegador ni a la auditoria: solo sus ultimos 4
 * caracteres.
 */
@Service
@RequiredArgsConstructor
public class ConfigurarGeminiUseCase {

    private final ConfiguracionGeminiRepositoryPort repositorio;
    private final AdministracionGeminiPort gemini;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public record Vista(ConfiguracionGeminiVigente vigente, List<EstadoModeloGemini> estadoModelos,
                        Long actualizadoPor, Instant actualizadoEn) {
    }

    /**
     * @param apiKey        nueva clave; null o vacia = conservar la actual
     * @param eliminarClave true = borrar la clave guardada y volver a la del servidor
     */
    public record Cambios(String apiKey, boolean eliminarClave, List<String> modelos) {
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Vista consultar() {
        ConfiguracionGeminiVigente vigente = gemini.vigente();
        Optional<ConfiguracionGemini> guardada = repositorio.obtener();
        return new Vista(vigente, gemini.estado(vigente.modelos()),
                guardada.map(ConfiguracionGemini::actualizadoPor).orElse(null),
                guardada.map(ConfiguracionGemini::actualizadoEn).orElse(null));
    }

    /**
     * No devuelve la vista: la cache de la configuracion se invalida despues
     * del commit (CacheConfig), asi que el controlador consulta de nuevo.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void actualizar(Cambios cambios, Long usuarioId, String ipOrigen) {
        List<String> modelos = normalizarModelos(cambios.modelos());
        String nuevaClave = cambios.apiKey() == null || cambios.apiKey().isBlank() ? null : cambios.apiKey().strip();
        if (cambios.eliminarClave() && nuevaClave != null) {
            throw new ValidacionDeNegocioException("No se puede ingresar una clave nueva y eliminar la clave al mismo tiempo");
        }

        ConfiguracionGeminiVigente antes = gemini.vigente();
        Optional<ConfiguracionGemini> guardada = repositorio.obtener();
        String clave;
        if (cambios.eliminarClave()) {
            clave = null;
        } else if (nuevaClave != null) {
            clave = nuevaClave;
        } else {
            clave = guardada.map(ConfiguracionGemini::apiKey).orElse(null);
        }

        repositorio.guardar(new ConfiguracionGemini(clave, false, modelos, usuarioId, clock.instant()));
        // Otra clave u otro orden de modelos: la cuota aprendida ya no aplica.
        gemini.reiniciarRotacion();

        String claveAntes = antes.tieneClave() ? antes.origenClave() + " " + antes.claveEnmascarada() : "ninguna";
        String claveDespues = clave != null ? "INTRANET " + enmascarar(clave) : "ENTORNO";
        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(usuarioId,
                "CONFIGURACION_GEMINI_ACTUALIZADA", "CONFIGURACION_GEMINI", 1,
                "clave=" + claveAntes + ";modelos=" + String.join(",", antes.modelos()),
                "clave=" + claveDespues + ";modelos=" + String.join(",", modelos), ipOrigen));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<ModeloGeminiDisponible> modelosDisponibles(String apiKey) {
        return gemini.listarModelos(blancoANull(apiKey));
    }

    /** Fuera de transaccion: llama a Google (ver backend-hexagonal, llamadas externas). */
    @PreAuthorize("hasRole('ADMIN')")
    public ResultadoPruebaGemini probar(String apiKey, String modelo) {
        if (modelo == null || modelo.isBlank()) {
            throw new ValidacionDeNegocioException("Indica el modelo a probar");
        }
        return gemini.probar(blancoANull(apiKey), modelo.strip());
    }

    static List<String> normalizarModelos(List<String> modelos) {
        LinkedHashSet<String> unicos = new LinkedHashSet<>();
        if (modelos != null) {
            modelos.stream().filter(m -> m != null && !m.isBlank()).map(String::strip).forEach(unicos::add);
        }
        if (unicos.isEmpty()) {
            throw new ValidacionDeNegocioException("Indica al menos un modelo de Gemini");
        }
        return List.copyOf(unicos);
    }

    private static String enmascarar(String clave) {
        return clave.length() <= 4 ? "****" : "****" + clave.substring(clave.length() - 4);
    }

    private static String blancoANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
