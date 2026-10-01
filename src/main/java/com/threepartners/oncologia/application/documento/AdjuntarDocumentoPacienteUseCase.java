package com.threepartners.oncologia.application.documento;

import com.threepartners.oncologia.domain.documento.DocumentoPaciente;
import com.threepartners.oncologia.domain.documento.DocumentoPacienteRepositoryPort;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdjuntarDocumentoPacienteUseCase {

    private final DocumentoPacienteRepositoryPort documentoPacienteRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'MEDICO')")
    @Transactional
    public DocumentoPaciente ejecutar(DocumentoPaciente documento) {
        pacienteRepositoryPort.buscarPorId(documento.getPacienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente", documento.getPacienteId()));

        documento.setFechaCarga(Instant.now());
        return documentoPacienteRepositoryPort.guardar(documento);
    }

    @Transactional(readOnly = true)
    public List<DocumentoPaciente> listarPorPaciente(Long pacienteId) {
        return documentoPacienteRepositoryPort.listarPorPaciente(pacienteId);
    }
}
