package com.threepartners.oncologia.domain.paciente;

import com.threepartners.oncologia.domain.shared.ZonaHoraria;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Paciente {

    private Long id;
    private Long usuarioId;
    private String nombres;
    private String apellidos;
    private String documentoIdentidad;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String email;
    private String direccion;
    private String tipoCancer;
    private String estadioClinico;
    private LocalDate fechaDiagnostico;
    private Long medicoTratanteId;
    private ConvenioSeguro convenioSeguro;
    private String contactoEmergenciaNombre;
    private String contactoEmergenciaTelefono;
    /** Correo del referido (el contacto de emergencia): recibe copia de los recordatorios si el paciente lo autoriza. */
    private String contactoEmergenciaEmail;
    /** Consentimiento del paciente para enviar a su referido los recordatorios (Ley 29733). */
    private boolean contactoRecibeRecordatorios;
    private boolean activo;
    private Instant fechaRegistro;
    /** Chat de Telegram vinculado (recordatorios y chatbot). Null si no lo vinculo. */
    private Long telegramChatId;
    private Instant telegramVinculadoEn;
    /**
     * Telegram del referido (acompanante): recibe los recordatorios del
     * paciente si este lo autorizo. Un mismo chat puede acompanar a varios
     * pacientes (p. ej. una hija que cuida a sus dos padres).
     */
    private Long contactoTelegramChatId;
    private Instant contactoTelegramVinculadoEn;
    /** null = si (valor por defecto); solo un false explicito apaga los recordatorios. */
    private Boolean aceptaRecordatorios;

    public int edad() {
        if (fechaNacimiento == null) {
            return 0;
        }
        return Period.between(fechaNacimiento, LocalDate.now(ZonaHoraria.LIMA)).getYears();
    }

    /** Primer nombre: lo unico que se muestra en un mensaje de Telegram (minimizacion de datos). */
    public String nombrePila() {
        if (nombres == null || nombres.isBlank()) {
            return "";
        }
        return nombres.strip().split("\\s+")[0];
    }

    public boolean aceptaRecordatorios() {
        return !Boolean.FALSE.equals(aceptaRecordatorios);
    }

    public boolean tieneTelegram() {
        return telegramChatId != null;
    }

    public void vincularTelegram(Long chatId, Instant cuando) {
        this.telegramChatId = chatId;
        this.telegramVinculadoEn = cuando;
        this.aceptaRecordatorios = true;
    }

    public void desvincularTelegram() {
        this.telegramChatId = null;
        this.telegramVinculadoEn = null;
    }

    public void vincularTelegramReferido(Long chatId, Instant cuando) {
        this.contactoTelegramChatId = chatId;
        this.contactoTelegramVinculadoEn = cuando;
    }

    public void desvincularTelegramReferido() {
        this.contactoTelegramChatId = null;
        this.contactoTelegramVinculadoEn = null;
    }

    /** El referido recibe los recordatorios por Telegram: tiene chat vinculado y el paciente lo autorizo. */
    public boolean referidoRecibeTelegram() {
        return contactoRecibeRecordatorios && contactoTelegramChatId != null;
    }

    /** Primer nombre del referido, para saludarlo en sus mensajes. */
    public String nombrePilaReferido() {
        if (contactoEmergenciaNombre == null || contactoEmergenciaNombre.isBlank()) {
            return "";
        }
        return contactoEmergenciaNombre.strip().split("\\s+")[0];
    }

    /**
     * Datos de contacto obligatorios para registrar o editar un paciente: su
     * correo (recordatorios de cita) y el de su referido. Se valida en el
     * dominio y no en la base para no invalidar a los pacientes ya cargados:
     * a esos se les pide al editarlos.
     */
    public void validarDatosDeContacto() {
        // El telefono es la llave para vincular Telegram con "Compartir mi numero".
        if (Telefono.normalizar(telefono).isEmpty()) {
            throw new ValidacionDeNegocioException("El telefono del paciente es obligatorio (al menos 9 digitos)");
        }
        if (Telefono.mismos(telefono, contactoEmergenciaTelefono)) {
            throw new ValidacionDeNegocioException("El telefono del referido debe ser distinto al del paciente");
        }
        if (email == null || email.isBlank()) {
            throw new ValidacionDeNegocioException("El correo electronico del paciente es obligatorio");
        }
        if (contactoEmergenciaEmail == null || contactoEmergenciaEmail.isBlank()) {
            throw new ValidacionDeNegocioException("El correo del referido es obligatorio");
        }
        if (contactoEmergenciaEmail.strip().equalsIgnoreCase(email.strip())) {
            throw new ValidacionDeNegocioException("El correo del referido debe ser distinto al del paciente");
        }
    }

    public boolean tieneEmail() {
        return email != null && !email.isBlank();
    }

    /** El referido solo recibe copia con el consentimiento del paciente y si tiene correo. */
    public boolean referidoRecibeRecordatorios() {
        return contactoRecibeRecordatorios && contactoEmergenciaEmail != null && !contactoEmergenciaEmail.isBlank();
    }

    public String nombreCompleto() {
        return "%s %s".formatted(nombres, apellidos);
    }
}
