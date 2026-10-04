package com.threepartners.oncologia.application.telegram;

import com.threepartners.oncologia.domain.auditoria.event.OperacionAuditadaEvent;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.paciente.Telefono;
import com.threepartners.oncologia.domain.paciente.VinculacionTelegramPendiente;
import com.threepartners.oncologia.domain.paciente.VinculacionTelegramPendienteRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Vinculacion de Telegram sin enlace personal: el paciente (o su referido)
 * abre el bot desde un QR comun, toca "Compartir mi numero" y confirma con
 * los 3 ultimos digitos del DNI del paciente. Asi no hace falta enviar a una
 * persona mayor un enlace que puede parecerle una estafa.
 *
 * Reglas, pensadas para casos reales:
 * - Solo se acepta el numero propio de la cuenta (Telegram informa el
 *   user_id del contacto): nadie puede vincular a otra persona compartiendo
 *   su tarjeta de contacto.
 * - Solo en chat privado (los ids de grupo son negativos).
 * - El numero solo no basta: un numero reasignado por la operadora o un
 *   celular compartido por dos pacientes se resuelven con los digitos del DNI.
 *   Tres intentos fallidos bloquean el chat 30 minutos.
 * - El referido solo se vincula si el paciente autorizo que reciba sus
 *   recordatorios (Ley 29733).
 */
@Service
@RequiredArgsConstructor
public class VinculacionTelegramPorTelefonoUseCase {

    static final String MENSAJE_SOLO_PROPIO = "Por seguridad solo aceptamos tu propio numero. "
            + "Toca el boton «📱 Compartir mi numero» que aparece debajo del chat.";
    static final String MENSAJE_SOLO_PRIVADO = "La vinculacion solo funciona en un chat privado con el bot.";
    static final String MENSAJE_NO_ENCONTRADO = "No encontramos este numero en nuestros registros. "
            + "Acercate a recepcion para que lo verifiquen en tu ficha.";
    static final String MENSAJE_SIN_AUTORIZACION = "Tu numero figura como contacto de un paciente, pero el paciente aun "
            + "no autorizo que recibas sus recordatorios. Puede autorizarlo en el portal (Mi perfil) o en recepcion.";
    static final String MENSAJE_PEDIR_DIGITOS = "Encontramos tu numero 👍 Para confirmar, escribe los 3 ultimos digitos "
            + "del DNI del paciente (si acompanas a alguien, los de su DNI). Escribe «cancelar» para salir.";
    static final String MENSAJE_BLOQUEADO = "Por seguridad, espera 30 minutos antes de volver a intentarlo "
            + "o acercate a recepcion.";

    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final VinculacionTelegramPendienteRepositoryPort pendienteRepositoryPort;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /** Paso 1: el usuario compartio un contacto en el bot. */
    @Transactional
    public String compartioNumero(Long chatId, Long remitenteId, Long contactoUsuarioId, String telefono) {
        Instant ahora = clock.instant();
        if (chatId == null || chatId <= 0) {
            return MENSAJE_SOLO_PRIVADO;
        }
        if (contactoUsuarioId == null || !contactoUsuarioId.equals(remitenteId)) {
            return MENSAJE_SOLO_PROPIO;
        }
        Optional<VinculacionTelegramPendiente> previa = pendienteRepositoryPort.buscar(chatId);
        if (previa.map(p -> p.bloqueado(ahora)).orElse(false)) {
            return MENSAJE_BLOQUEADO;
        }

        String numero = Telefono.normalizar(telefono);
        List<Long> titulares = numero.isEmpty() ? List.of()
                : pacienteRepositoryPort.listarPorTelefono(numero).stream().map(Paciente::getId).toList();
        List<Paciente> comoReferido = numero.isEmpty() ? List.of() : pacienteRepositoryPort.listarPorTelefonoReferido(numero);
        List<Long> referidos = comoReferido.stream().filter(Paciente::isContactoRecibeRecordatorios).map(Paciente::getId).toList();

        if (titulares.isEmpty() && referidos.isEmpty()) {
            return comoReferido.isEmpty() ? MENSAJE_NO_ENCONTRADO : MENSAJE_SIN_AUTORIZACION;
        }
        pendienteRepositoryPort.guardar(VinculacionTelegramPendiente.nueva(chatId, titulares, referidos, ahora));
        return MENSAJE_PEDIR_DIGITOS;
    }

    /**
     * Paso 2: cada mensaje de texto del chat pasa primero por aqui. Si el chat
     * esta esperando los digitos del DNI, se responde; si no, Optional.empty()
     * y el mensaje sigue al chatbot.
     */
    @Transactional
    public Optional<String> confirmar(Long chatId, String texto) {
        Instant ahora = clock.instant();
        Optional<VinculacionTelegramPendiente> encontrada = pendienteRepositoryPort.buscar(chatId);
        if (encontrada.isEmpty()) {
            return Optional.empty();
        }
        VinculacionTelegramPendiente pendiente = encontrada.get();
        if (!pendiente.esperandoDigitos(ahora)) {
            if (!pendiente.bloqueado(ahora)) {
                pendienteRepositoryPort.eliminar(chatId);
            }
            return Optional.empty();
        }

        String limpio = texto == null ? "" : texto.strip().toLowerCase(Locale.ROOT);
        if (limpio.equals("cancelar") || limpio.equals("/cancelar")) {
            pendienteRepositoryPort.eliminar(chatId);
            return Optional.of("Listo, cancelamos la vinculacion. Puedes volver a intentarlo cuando quieras.");
        }
        if (!limpio.matches("\\d{3}")) {
            return Optional.of("Escribe solo los 3 ultimos digitos del DNI del paciente (por ejemplo: 123), "
                    + "o «cancelar» para salir.");
        }

        List<Paciente> titulares = coinciden(pendiente.getTitulares(), limpio);
        List<Paciente> referidos = coinciden(pendiente.getReferidos(), limpio);
        if (titulares.isEmpty() && referidos.isEmpty()) {
            pendiente.fallo(ahora);
            pendienteRepositoryPort.guardar(pendiente);
            return Optional.of(pendiente.bloqueado(ahora)
                    ? "Los digitos no coinciden. " + MENSAJE_BLOQUEADO
                    : "Los digitos no coinciden. Te quedan " + pendiente.intentosRestantes() + " intento(s).");
        }

        titulares.forEach(p -> vincularTitular(p, chatId, ahora));
        referidos.forEach(p -> vincularReferido(p, chatId, ahora));
        pendienteRepositoryPort.eliminar(chatId);
        return Optional.of(confirmacion(titulares, referidos));
    }

    private List<Paciente> coinciden(List<Long> ids, String digitos) {
        List<Paciente> resultado = new ArrayList<>();
        for (Long id : ids) {
            pacienteRepositoryPort.buscarPorId(id)
                    .filter(p -> p.getDocumentoIdentidad() != null && p.getDocumentoIdentidad().endsWith(digitos))
                    .ifPresent(resultado::add);
        }
        return resultado;
    }

    /** Un chat es titular de un solo paciente: si ya lo era de otro, se mueve. */
    private void vincularTitular(Paciente paciente, Long chatId, Instant ahora) {
        pacienteRepositoryPort.buscarPorTelegramChatId(chatId)
                .filter(otro -> !otro.getId().equals(paciente.getId()))
                .ifPresent(otro -> {
                    otro.desvincularTelegram();
                    pacienteRepositoryPort.guardar(otro);
                });
        paciente.vincularTelegram(chatId, ahora);
        pacienteRepositoryPort.guardar(paciente);
        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(null, "TELEGRAM_VINCULADO", "PACIENTE",
                paciente.getId(), null, "vinculado=true;metodo=telefono", "telegram"));
    }

    private void vincularReferido(Paciente paciente, Long chatId, Instant ahora) {
        paciente.vincularTelegramReferido(chatId, ahora);
        pacienteRepositoryPort.guardar(paciente);
        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(null, "TELEGRAM_REFERIDO_VINCULADO", "PACIENTE",
                paciente.getId(), null, "referido_vinculado=true;metodo=telefono", "telegram"));
    }

    /** Solo nombres de pila (minimizacion de datos en Telegram). */
    private static String confirmacion(List<Paciente> titulares, List<Paciente> referidos) {
        StringBuilder texto = new StringBuilder("¡Listo");
        if (!titulares.isEmpty()) {
            texto.append(", ").append(titulares.getFirst().nombrePila()).append("! Desde ahora te recordaremos tus citas por aqui.");
        } else {
            texto.append("!");
        }
        if (!referidos.isEmpty()) {
            String nombres = String.join(" y ", referidos.stream().map(Paciente::nombrePila).toList());
            texto.append(" Tambien te enviaremos los recordatorios de las citas de ").append(nombres).append('.');
        }
        texto.append(" Solo te enviaremos la fecha y hora de las citas; nunca te pediremos contrasenas ni pagos.");
        return texto.toString();
    }
}
