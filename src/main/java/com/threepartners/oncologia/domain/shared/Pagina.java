package com.threepartners.oncologia.domain.shared;

import java.util.List;

public record Pagina<T>(
        List<T> contenido,
        long totalElementos,
        int totalPaginas,
        int numeroPagina,
        int tamanoPagina
) {

    public static <T> Pagina<T> de(List<T> contenido, long totalElementos, int numeroPagina, int tamanoPagina) {
        int totalPaginas = tamanoPagina == 0 ? 0 : (int) Math.ceil((double) totalElementos / tamanoPagina);
        return new Pagina<>(contenido, totalElementos, totalPaginas, numeroPagina, tamanoPagina);
    }

    public <R> Pagina<R> map(java.util.function.Function<T, R> mapper) {
        return new Pagina<>(contenido.stream().map(mapper).toList(), totalElementos, totalPaginas, numeroPagina, tamanoPagina);
    }
}
