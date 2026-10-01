package com.threepartners.oncologia.infrastructure.in.rest.mapper;

import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.infrastructure.in.rest.dto.cita.CitaRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.cita.CitaResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CitaRestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    Cita aDominio(CitaRequestDto dto);

    CitaResponseDto aResponse(Cita cita);
}
