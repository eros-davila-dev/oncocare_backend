package com.threepartners.oncologia.infrastructure.in.rest.mapper;

import com.threepartners.oncologia.domain.paciente.EstadisticasPacientes;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteResumen;
import com.threepartners.oncologia.infrastructure.in.rest.dto.paciente.EstadisticasPacientesResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.paciente.PacienteRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.paciente.PacienteResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.paciente.PacienteResumenResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Context;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PacienteRestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuarioId", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaRegistro", ignore = true)
    Paciente aDominio(PacienteRequestDto dto);

    @Mapping(target = "edad", expression = "java(paciente.edad())")
    @Mapping(target = "medicoTratanteNombre", ignore = true)
    PacienteResponseDto aResponse(Paciente paciente);

    @Mapping(target = "edad", expression = "java(paciente.edad())")
    @Mapping(target = "medicoTratanteNombre", expression = "java(nombres.usuario(paciente.getMedicoTratanteId()))")
    PacienteResponseDto aResponseConNombres(Paciente paciente, @Context NombresVista nombres);

    default PacienteResumenResponseDto aResumenResponse(PacienteResumen resumen) {
        Paciente p = resumen.paciente();
        return new PacienteResumenResponseDto(
                p.getId(), p.getNombres(), p.getApellidos(), p.getDocumentoIdentidad(), p.edad(),
                p.getTipoCancer(), p.getConvenioSeguro(), p.isActivo(),
                resumen.estadoTratamiento(), resumen.medicoTratanteNombre(), resumen.medicoTratanteEspecialidad(),
                resumen.ultimaCita(), resumen.proximaCita(), resumen.tieneTelegram(), resumen.referidoTieneTelegram());
    }

    default EstadisticasPacientesResponseDto aEstadisticasResponse(EstadisticasPacientes e) {
        return new EstadisticasPacientesResponseDto(
                e.pacientesRegistrados(), e.variacionPacientesRegistradosPorcentaje(),
                e.pacientesEnTratamiento(), e.variacionPacientesEnTratamientoPorcentaje(),
                e.citasEstaSemana(), e.variacionCitasPorcentaje(),
                e.asistenciaCitasPorcentaje(), e.variacionAsistenciaPorcentaje());
    }
}
