package com.threepartners.oncologia.infrastructure.in.rest.mapper;

import com.threepartners.oncologia.domain.tratamiento.CicloTratamiento;
import com.threepartners.oncologia.infrastructure.in.rest.dto.tratamiento.CicloTratamientoRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.tratamiento.CicloTratamientoResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Context;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CicloTratamientoRestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    CicloTratamiento aDominio(CicloTratamientoRequestDto dto);

    @Mapping(target = "porcentajeCumplimiento", expression = "java(ciclo.porcentajeCumplimiento())")
    @Mapping(target = "pacienteNombre", ignore = true)
    @Mapping(target = "medicoResponsableNombre", ignore = true)
    CicloTratamientoResponseDto aResponse(CicloTratamiento ciclo);

    @Mapping(target = "porcentajeCumplimiento", expression = "java(ciclo.porcentajeCumplimiento())")
    @Mapping(target = "pacienteNombre", expression = "java(nombres.paciente(ciclo.getPacienteId()))")
    @Mapping(target = "medicoResponsableNombre", expression = "java(nombres.usuario(ciclo.getMedicoResponsableId()))")
    CicloTratamientoResponseDto aResponseConNombres(CicloTratamiento ciclo, @Context NombresVista nombres);
}
