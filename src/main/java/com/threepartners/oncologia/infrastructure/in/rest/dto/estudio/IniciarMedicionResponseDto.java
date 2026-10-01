package com.threepartners.oncologia.infrastructure.in.rest.dto.estudio;

import com.threepartners.oncologia.domain.estudio.CanalMedicion;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;

import java.time.Instant;

/**
 * Identificador de la sesion de medicion que el formulario debe reenviar al
 * guardar (campo medicionId). El inicio se informa solo como referencia: el
 * servidor nunca acepta un tiempo enviado por el cliente.
 */
public record IniciarMedicionResponseDto(Long medicionId, TipoMedicion tipo, CanalMedicion canal, Instant inicio) {
}
