package com.threepartners.oncologia.domain.paciente;

import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;

import java.util.List;
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

    /** Pacientes cuyo referido tiene ese chat de Telegram vinculado. */
    List<Paciente> listarPorTelegramReferido(Long chatId);

    /** Pacientes activos con ese telefono (ultimos 9 digitos, ver {@link Telefono}). */
    List<Paciente> listarPorTelefono(String telefonoNormalizado);

    /** Pacientes activos cuyo referido tiene ese telefono. */
    List<Paciente> listarPorTelefonoReferido(String telefonoNormalizado);

    Pagina<Paciente> buscar(String textoBusqueda, CriterioPaginacion criterio);

    Pagina<PacienteResumen> buscarResumen(String textoBusqueda, EstadoTratamientoPaciente estado, CriterioPaginacion criterio);
}
