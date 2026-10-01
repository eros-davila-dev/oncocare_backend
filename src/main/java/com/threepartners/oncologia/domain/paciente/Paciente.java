package com.threepartners.oncologia.domain.paciente;

import com.threepartners.oncologia.domain.shared.ZonaHoraria;
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
    private boolean activo;
    private Instant fechaRegistro;
    /** Chat de Telegram vinculado (recordatorios y chatbot). Null si no lo vinculo. */
    private Long telegramChatId;
    private Instant telegramVinculadoEn;
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

    public String nombreCompleto() {
        return "%s %s".formatted(nombres, apellidos);
    }
}
