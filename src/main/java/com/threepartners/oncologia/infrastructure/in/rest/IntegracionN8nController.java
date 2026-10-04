package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.chatbot.ChatbotOrquestadorUseCase;
import com.threepartners.oncologia.application.chatbot.InteractuarConsultaChatbotUseCase;
import com.threepartners.oncologia.application.recordatorio.EntregarRecordatoriosUseCase;
import com.threepartners.oncologia.application.recordatorio.EntregarRecordatoriosUseCase.RecordatorioParaEnviar;
import com.threepartners.oncologia.application.recordatorio.ResponderRecordatorioUseCase;
import com.threepartners.oncologia.application.recordatorio.ResponderRecordatorioUseCase.AccionRecordatorio;
import com.threepartners.oncologia.application.telegram.VinculacionTelegramPorTelefonoUseCase;
import com.threepartners.oncologia.application.telegram.VinculacionTelegramUseCase;
import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import com.threepartners.oncologia.domain.shared.exception.DomainException;
import com.threepartners.oncologia.infrastructure.in.rest.dto.chatbot.ChatbotMensajeResponseDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Contrato con los workflows de n8n (ver n8n/README.md). Todas las rutas
 * exigen el header X-Webhook-Secret: n8n no usa JWT de usuario. El backend
 * decide; n8n solo traslada mensajes entre Telegram y estos endpoints.
 *
 * Las respuestas a Telegram siempre traen un texto listo para mostrar: si una
 * regla de negocio rechaza la accion, se devuelve el motivo en lenguaje claro
 * en vez de un error que n8n tendria que interpretar.
 */
@RestController
@RequestMapping("/api/v1/integraciones")
@RequiredArgsConstructor
public class IntegracionN8nController {

    private static final String HEADER = "X-Webhook-Secret";

    private final WebhookSecretValidator webhookSecretValidator;
    private final VinculacionTelegramUseCase vinculacionTelegramUseCase;
    private final VinculacionTelegramPorTelefonoUseCase vinculacionPorTelefonoUseCase;
    private final EntregarRecordatoriosUseCase entregarRecordatoriosUseCase;
    private final ResponderRecordatorioUseCase responderRecordatorioUseCase;
    private final ChatbotOrquestadorUseCase chatbotOrquestadorUseCase;
    private final InteractuarConsultaChatbotUseCase interactuarConsultaUseCase;

    @GetMapping("/recordatorios/pendientes")
    public List<RecordatorioParaEnviar> recordatoriosPendientes(@RequestHeader(value = HEADER, required = false) String secreto,
                                                                @RequestParam(defaultValue = "50") int limite) {
        webhookSecretValidator.validar(secreto);
        return entregarRecordatoriosUseCase.tomarPendientes(limite);
    }

    @PostMapping("/recordatorios/{id}/resultado")
    public ResponseEntity<Void> resultadoRecordatorio(@RequestHeader(value = HEADER, required = false) String secreto,
                                                      @PathVariable Long id, @Valid @RequestBody ResultadoEnvioDto dto) {
        webhookSecretValidator.validar(secreto);
        entregarRecordatoriosUseCase.registrarResultado(id, dto.enviado(), dto.mensajeExternoId(), dto.error());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/telegram/vincular")
    public RespuestaTelegramDto vincular(@RequestHeader(value = HEADER, required = false) String secreto,
                                         @Valid @RequestBody VincularDto dto) {
        webhookSecretValidator.validar(secreto);
        try {
            var paciente = vinculacionTelegramUseCase.vincular(dto.token(), dto.chatId());
            return new RespuestaTelegramDto(true, "¡Listo, %s! Desde ahora te recordaremos tus citas por aqui. "
                    .formatted(paciente.nombrePila()) + "Tambien puedes escribirme si tienes alguna consulta.");
        } catch (DomainException e) {
            return new RespuestaTelegramDto(false, e.getMessage()
                    + ". Pide un nuevo enlace en el portal (Mi perfil) o en recepcion.");
        }
    }

    /**
     * El usuario toco "Compartir mi numero" en el bot (vinculacion sin enlace
     * personal). fromId y contactoUsuarioId deben coincidir: solo se acepta el
     * numero propio.
     */
    @PostMapping("/telegram/vincular-telefono")
    public RespuestaTelegramDto vincularPorTelefono(@RequestHeader(value = HEADER, required = false) String secreto,
                                                    @Valid @RequestBody VincularTelefonoDto dto) {
        webhookSecretValidator.validar(secreto);
        return new RespuestaTelegramDto(true, vinculacionPorTelefonoUseCase.compartioNumero(
                dto.chatId(), dto.fromId(), dto.contactoUsuarioId(), dto.telefono()));
    }

    @PostMapping("/telegram/desvincular")
    public RespuestaTelegramDto desvincular(@RequestHeader(value = HEADER, required = false) String secreto,
                                            @Valid @RequestBody ChatDto dto) {
        webhookSecretValidator.validar(secreto);
        boolean desvinculado = vinculacionTelegramUseCase.desvincularPorChat(dto.chatId());
        return new RespuestaTelegramDto(desvinculado, desvinculado
                ? "Listo, ya no te enviaremos recordatorios por Telegram. Puedes volver a vincularte cuando quieras."
                : "Este chat no estaba vinculado a ningun paciente.");
    }

    @PostMapping("/telegram/accion-cita")
    public RespuestaTelegramDto accionCita(@RequestHeader(value = HEADER, required = false) String secreto,
                                           @Valid @RequestBody AccionCitaDto dto) {
        webhookSecretValidator.validar(secreto);
        try {
            return new RespuestaTelegramDto(true, responderRecordatorioUseCase.responder(dto.chatId(), dto.recordatorioId(), dto.accion()));
        } catch (DomainException e) {
            return new RespuestaTelegramDto(false, "No pude procesar tu respuesta: " + e.getMessage());
        }
    }

    @PostMapping("/telegram/mensaje")
    public ChatbotMensajeResponseDto mensaje(@RequestHeader(value = HEADER, required = false) String secreto,
                                             @Valid @RequestBody MensajeTelegramDto dto) {
        webhookSecretValidator.validar(secreto);
        // Si el chat esta confirmando una vinculacion (3 digitos del DNI), ese texto no va al chatbot.
        var confirmacion = vinculacionPorTelefonoUseCase.confirmar(dto.chatId(), dto.texto());
        if (confirmacion.isPresent()) {
            return new ChatbotMensajeResponseDto(confirmacion.get(), null, null);
        }
        var respuesta = chatbotOrquestadorUseCase.procesarDesdeTelegram(dto.chatId(), dto.texto());
        return new ChatbotMensajeResponseDto(respuesta.texto(), respuesta.consultaId(), respuesta.estadoConsulta());
    }

    @PostMapping("/telegram/valoracion")
    public RespuestaTelegramDto valoracion(@RequestHeader(value = HEADER, required = false) String secreto,
                                           @Valid @RequestBody ValoracionTelegramDto dto) {
        webhookSecretValidator.validar(secreto);
        try {
            interactuarConsultaUseCase.valorar(dto.consultaId(), dto.valor(), "tg-" + dto.chatId());
            return new RespuestaTelegramDto(true, dto.valor() > 0
                    ? "¡Gracias! Me alegra haberte ayudado."
                    : "Gracias por avisarme. Le pedi a una persona del equipo que revise tu consulta.");
        } catch (DomainException e) {
            return new RespuestaTelegramDto(false, "No pude registrar tu valoracion.");
        }
    }

    @PostMapping("/telegram/escalar")
    public RespuestaTelegramDto escalar(@RequestHeader(value = HEADER, required = false) String secreto,
                                        @Valid @RequestBody ChatDto dto) {
        webhookSecretValidator.validar(secreto);
        interactuarConsultaUseCase.hablarConUnaPersonaPorTelegram(dto.chatId());
        return new RespuestaTelegramDto(true, "Listo, una persona del equipo de la fundacion te escribira pronto.");
    }

    public record ResultadoEnvioDto(@NotNull Boolean enviado, @Size(max = 50) String mensajeExternoId, String error) {
    }

    public record VincularDto(@NotBlank @Size(max = 64) String token, @NotNull Long chatId) {
    }

    public record ChatDto(@NotNull Long chatId) {
    }

    /** Datos del update de Telegram con message.contact: from.id, contact.user_id y contact.phone_number. */
    public record VincularTelefonoDto(@NotNull Long chatId, @NotNull Long fromId, Long contactoUsuarioId,
                                      @NotBlank @Size(max = 30) String telefono) {
    }

    public record AccionCitaDto(@NotNull Long chatId, @NotNull Long recordatorioId, @NotNull AccionRecordatorio accion) {
    }

    public record MensajeTelegramDto(@NotNull Long chatId, @NotBlank @Size(max = 2000) String texto) {
    }

    public record ValoracionTelegramDto(@NotNull Long chatId, @NotNull Long consultaId,
                                        @NotNull @Min(-1) @Max(1) Integer valor) {
    }

    /** Texto listo para enviar al chat; ok=false si la accion no se pudo realizar. */
    public record RespuestaTelegramDto(boolean ok, String mensaje) {
    }
}
