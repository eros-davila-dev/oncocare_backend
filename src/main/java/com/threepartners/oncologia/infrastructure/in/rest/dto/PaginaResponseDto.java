package com.threepartners.oncologia.infrastructure.in.rest.dto;

import com.threepartners.oncologia.domain.shared.Pagina;

import java.util.List;
import java.util.function.Function;

/**
 * Contrato de paginacion consumido por el frontend (seccion 7): content,
 * totalElements, totalPages, pageNumber, pageSize. Todos los listados
 * paginados de la API responden con esta misma forma.
 */
public record PaginaResponseDto<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int pageNumber,
        int pageSize
) {

    public static <D, T> PaginaResponseDto<T> de(Pagina<D> pagina, Function<D, T> mapper) {
        return new PaginaResponseDto<>(
                pagina.contenido().stream().map(mapper).toList(),
                pagina.totalElementos(),
                pagina.totalPaginas(),
                pagina.numeroPagina(),
                pagina.tamanoPagina());
    }
}
