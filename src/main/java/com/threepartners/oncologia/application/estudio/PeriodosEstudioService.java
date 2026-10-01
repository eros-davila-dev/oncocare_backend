package com.threepartners.oncologia.application.estudio;

import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.FaseEstudio;
import com.threepartners.oncologia.domain.estudio.FaseEstudioRepositoryPort;
import com.threepartners.oncologia.domain.estudio.PeriodoMedicion;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Traduce "fase" a "rango de fechas", la unica forma en que el sistema decide
 * si un evento pertenece al pretest o al postest.
 */
@Service
@RequiredArgsConstructor
public class PeriodosEstudioService {

    private final FaseEstudioRepositoryPort faseEstudioRepositoryPort;

    public PeriodoMedicion periodoDe(Fase fase) {
        return faseEstudioRepositoryPort.buscarPorFase(fase)
                .map(FaseEstudio::periodo)
                .orElseThrow(() -> new ValidacionDeNegocioException(
                        "La fase " + fase + " aun no tiene fechas configuradas (Estudio > Fases)"));
    }

    /**
     * Si se pide una fase, su periodo; si no, el rango explicito; si tampoco
     * hay rango, los ultimos 30 dias (vista de gestion diaria).
     */
    public PeriodoMedicion resolver(Fase fase, LocalDate desde, LocalDate hasta, LocalDate hoy) {
        if (fase != null) {
            return periodoDe(fase);
        }
        LocalDate hastaEfectivo = hasta != null ? hasta : hoy;
        LocalDate desdeEfectivo = desde != null ? desde : hastaEfectivo.minusDays(30);
        return new PeriodoMedicion(desdeEfectivo, hastaEfectivo);
    }

    /**
     * Las fichas manuales solo se aceptan con fecha dentro de una fase
     * configurada y abierta: un dato fuera de toda fase no serviria para la
     * comparacion y uno en una fase cerrada alteraria resultados ya congelados.
     */
    public FaseEstudio exigirFaseAbiertaQueContiene(LocalDate fecha) {
        var fases = faseEstudioRepositoryPort.listar();
        if (fases.isEmpty()) {
            throw new ValidacionDeNegocioException(
                    "Configure las fechas de las fases del estudio antes de capturar fichas");
        }
        FaseEstudio fase = fases.stream()
                .filter(f -> f.contiene(fecha))
                .findFirst()
                .orElseThrow(() -> new ValidacionDeNegocioException(
                        "La fecha " + fecha + " no pertenece a ninguna fase del estudio"));
        if (fase.estaCerrada()) {
            throw new ValidacionDeNegocioException(
                    "La fase " + fase.getFase() + " esta cerrada: ya no admite nuevas capturas");
        }
        return fase;
    }
}
