package com.threepartners.oncologia.infrastructure.in.rest.dto.dispositivo;

import com.threepartners.oncologia.domain.dispositivo.EstadoConexion;
import com.threepartners.oncologia.domain.dispositivo.ProtocoloDispositivo;
import com.threepartners.oncologia.domain.dispositivo.TipoDispositivo;

public record DispositivoResponseDto(Long id, String nombre, TipoDispositivo tipo, ProtocoloDispositivo protocolo, EstadoConexion estadoConexion) {
}
