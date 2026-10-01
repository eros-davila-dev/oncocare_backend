package com.threepartners.oncologia.domain.dispositivo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturaDispositivo {

    private Long id;
    private Long dispositivoId;
    private Long pacienteId;
    private Long cicloTratamientoId;
    private String tipoDato;
    private String valor;
    private Instant fechaLectura;
}
