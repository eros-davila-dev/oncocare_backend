package com.threepartners.oncologia.domain.shared;

public record CriterioPaginacion(int numeroPagina, int tamanoPagina, String ordenarPor, boolean ascendente) {

    public CriterioPaginacion {
        if (numeroPagina < 0) {
            numeroPagina = 0;
        }
        if (tamanoPagina <= 0 || tamanoPagina > 100) {
            tamanoPagina = 20;
        }
    }

    public static CriterioPaginacion de(int numeroPagina, int tamanoPagina) {
        return new CriterioPaginacion(numeroPagina, tamanoPagina, null, true);
    }
}
