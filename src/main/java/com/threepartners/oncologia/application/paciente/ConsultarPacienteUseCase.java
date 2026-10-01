package com.threepartners.oncologia.application.paciente;

import com.threepartners.oncologia.domain.paciente.EstadoTratamientoPaciente;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.paciente.PacienteResumen;
import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsultarPacienteUseCase {

    private final PacienteRepositoryPort pacienteRepositoryPort;

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public Paciente porId(Long id) {
        return pacienteRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente", id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public Pagina<Paciente> buscar(String textoBusqueda, CriterioPaginacion criterio) {
        return pacienteRepositoryPort.buscar(textoBusqueda, criterio);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public Pagina<PacienteResumen> buscarResumen(String textoBusqueda, EstadoTratamientoPaciente estado, CriterioPaginacion criterio) {
        return pacienteRepositoryPort.buscarResumen(textoBusqueda, estado, criterio);
    }

    /**
     * Perfil propio del paciente autenticado (seccion 31: un paciente solo
     * puede ver su propia informacion, nunca la de otro por id).
     */
    @PreAuthorize("hasRole('PACIENTE')")
    @Transactional(readOnly = true)
    public Paciente miPerfil(Long usuarioAutenticadoId) {
        return pacienteRepositoryPort.buscarPorUsuarioId(usuarioAutenticadoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente", usuarioAutenticadoId));
    }

    /**
     * Soporta el validador asincrono de unicidad de documento en el frontend
     * (seccion 7): se consulta antes de enviar el formulario, sin esperar al
     * conflicto 409 que igualmente aplica RegistrarPacienteUseCase como
     * garantia final a nivel de backend. Abierto a cualquier rol autenticado
     * (incluido PACIENTE durante el autoservicio): solo revela un booleano.
     */
    @Transactional(readOnly = true)
    public boolean existeDocumento(String documentoIdentidad, Long idExcluido) {
        return idExcluido == null
                ? pacienteRepositoryPort.existePorDocumento(documentoIdentidad)
                : pacienteRepositoryPort.existePorDocumentoYNoId(documentoIdentidad, idExcluido);
    }
}
