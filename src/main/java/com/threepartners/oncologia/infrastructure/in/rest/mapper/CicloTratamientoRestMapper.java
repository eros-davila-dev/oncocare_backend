package com.threepartners.oncologia.infrastructure.in.rest.mapper;

import com.threepartners.oncologia.domain.tratamiento.CicloTratamiento;
import com.threepartners.oncologia.infrastructure.in.rest.dto.tratamiento.CicloTratamientoRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.tratamiento.CicloTratamientoResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CicloTratamientoRestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    CicloTratamiento aDominio(CicloTratamientoRequestDto dto);

    @Mapping(target = "porcentajeCumplimiento", expression = "java(ciclo.porcentajeCumplimiento())")
    CicloTratamientoResponseDto aResponse(CicloTratamiento ciclo);
}
