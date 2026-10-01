package com.threepartners.oncologia.domain.dispositivo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispositivoExterno {

    private Long id;
    private String nombre;
    private TipoDispositivo tipo;
    private ProtocoloDispositivo protocolo;
    private EstadoConexion estadoConexion;
    private String credencialHash;
}
