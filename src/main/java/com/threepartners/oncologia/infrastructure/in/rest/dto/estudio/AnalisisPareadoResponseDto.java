package com.threepartners.oncologia.infrastructure.in.rest.dto.estudio;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.threepartners.oncologia.domain.estudio.AnalisisPareado;
import com.threepartners.oncologia.domain.estudio.ResultadoWilcoxon;

/**
 * Wilcoxon preliminar por indicador. Los nulos se serializan: "sin pares"
 * debe llegar como null explicito, no como un campo ausente.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record AnalisisPareadoResponseDto(
        WilcoxonDto tiempoPromedioRegistro,
        WilcoxonDto tasaAusentismo,
        WilcoxonDto nivelConsultasAtendidas
) {

    public static AnalisisPareadoResponseDto de(AnalisisPareado a) {
        return new AnalisisPareadoResponseDto(
                WilcoxonDto.de(a.tiempoPromedioRegistro()),
                WilcoxonDto.de(a.tasaAusentismo()),
                WilcoxonDto.de(a.nivelConsultasAtendidas()));
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record WilcoxonDto(
            int pares,
            int empates,
            int n,
            int rangosNegativos,
            int rangosPositivos,
            Double sumaRangosNegativos,
            Double sumaRangosPositivos,
            Double medianaPretest,
            Double medianaPostest,
            Double z,
            Double pAsintotica,
            Double pExacta,
            Double tamanoEfecto
    ) {

        static WilcoxonDto de(ResultadoWilcoxon r) {
            return new WilcoxonDto(r.pares(), r.empates(), r.n(), r.rangosNegativos(), r.rangosPositivos(),
                    r.sumaRangosNegativos(), r.sumaRangosPositivos(), r.medianaPretest(), r.medianaPostest(),
                    r.z(), r.pAsintotica(), r.pExacta(), r.tamanoEfecto());
        }
    }
}
