package com.threepartners.oncologia.domain.estudio;

import java.util.List;
import java.util.Optional;

public interface FaseEstudioRepositoryPort {

    List<FaseEstudio> listar();

    Optional<FaseEstudio> buscarPorFase(Fase fase);

    FaseEstudio guardar(FaseEstudio fase);
}
