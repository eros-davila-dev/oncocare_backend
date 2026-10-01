package com.threepartners.oncologia.domain.paciente;

import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;

import java.util.Optional;

public interface PacienteRepositoryPort {

    Paciente guardar(Paciente paciente);

    Optional<Paciente> buscarPorId(Long id);

    Optional<Paciente> buscarPorDocumento(String documentoIdentidad);

    boolean existePorDocumento(String documentoIdentidad);

    boolean existePorDocumentoYNoId(String documentoIdentidad, Long idExcluido);

    boolean existePorEmail(String email);

    boolean existePorEmailYNoId(String email, Long idExcluido);

    Optional<Paciente> buscarPorUsuarioId(Long usuarioId);

    Optional<Paciente> buscarPorTelegramChatId(Long chatId);

    Pagina<Paciente> buscar(String textoBusqueda, CriterioPaginacion criterio);

    Pagina<PacienteResumen> buscarResumen(String textoBusqueda, EstadoTratamientoPaciente estado, CriterioPaginacion criterio);
}
