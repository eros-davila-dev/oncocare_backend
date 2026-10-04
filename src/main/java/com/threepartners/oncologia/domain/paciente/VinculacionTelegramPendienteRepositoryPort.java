package com.threepartners.oncologia.domain.paciente;

import java.util.Optional;

public interface VinculacionTelegramPendienteRepositoryPort {

    Optional<VinculacionTelegramPendiente> buscar(Long chatId);

    void guardar(VinculacionTelegramPendiente pendiente);

    void eliminar(Long chatId);
}
