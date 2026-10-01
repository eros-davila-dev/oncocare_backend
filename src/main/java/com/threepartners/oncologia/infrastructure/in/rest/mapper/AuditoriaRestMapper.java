package com.threepartners.oncologia.infrastructure.in.rest.mapper;

import com.threepartners.oncologia.domain.auditoria.AuditoriaAccion;
import com.threepartners.oncologia.infrastructure.in.rest.dto.auditoria.AuditoriaResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditoriaRestMapper {

    AuditoriaResponseDto aResponse(AuditoriaAccion accion);
}
