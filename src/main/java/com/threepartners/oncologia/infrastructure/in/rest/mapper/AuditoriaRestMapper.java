package com.threepartners.oncologia.infrastructure.in.rest.mapper;

import com.threepartners.oncologia.domain.auditoria.AuditoriaAccion;
import com.threepartners.oncologia.infrastructure.in.rest.dto.auditoria.AuditoriaResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Context;

@Mapper(componentModel = "spring")
public interface AuditoriaRestMapper {

    @Mapping(target = "usuarioNombre", expression = "java(nombres.usuario(accion.getUsuarioId()))")
    @Mapping(target = "entidadDescripcion", expression = "java(nombres.entidad(accion.getEntidadAfectada(), accion.getEntidadId()))")
    AuditoriaResponseDto aResponse(AuditoriaAccion accion, @Context NombresVista nombres);
}
