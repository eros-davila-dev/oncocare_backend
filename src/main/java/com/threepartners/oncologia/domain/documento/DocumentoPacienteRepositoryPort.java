package com.threepartners.oncologia.domain.documento;

import java.util.List;

public interface DocumentoPacienteRepositoryPort {

    DocumentoPaciente guardar(DocumentoPaciente documento);

    List<DocumentoPaciente> listarPorPaciente(Long pacienteId);
}
