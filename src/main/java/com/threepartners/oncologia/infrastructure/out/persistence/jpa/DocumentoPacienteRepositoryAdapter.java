package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.documento.DocumentoPaciente;
import com.threepartners.oncologia.domain.documento.DocumentoPacienteRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DocumentoPacienteRepositoryAdapter implements DocumentoPacienteRepositoryPort {

    private final DocumentoPacienteJpaRepository jpaRepository;

    @Override
    public DocumentoPaciente guardar(DocumentoPaciente documento) {
        return aDominio(jpaRepository.save(aEntidad(documento)));
    }

    @Override
    public List<DocumentoPaciente> listarPorPaciente(Long pacienteId) {
        return jpaRepository.findByPacienteIdOrderByFechaCargaDesc(pacienteId).stream()
                .map(DocumentoPacienteRepositoryAdapter::aDominio)
                .toList();
    }

    private static DocumentoPacienteJpaEntity aEntidad(DocumentoPaciente documento) {
        return DocumentoPacienteJpaEntity.builder()
                .id(documento.getId())
                .pacienteId(documento.getPacienteId())
                .tipoDocumento(documento.getTipoDocumento())
                .urlArchivo(documento.getUrlArchivo())
                .fechaCarga(documento.getFechaCarga())
                .build();
    }

    private static DocumentoPaciente aDominio(DocumentoPacienteJpaEntity entidad) {
        return DocumentoPaciente.builder()
                .id(entidad.getId())
                .pacienteId(entidad.getPacienteId())
                .tipoDocumento(entidad.getTipoDocumento())
                .urlArchivo(entidad.getUrlArchivo())
                .fechaCarga(entidad.getFechaCarga())
                .build();
    }
}
