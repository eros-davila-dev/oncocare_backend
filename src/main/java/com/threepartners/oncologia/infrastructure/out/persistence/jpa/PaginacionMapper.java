package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.function.Function;

/**
 * Unico punto de conversion entre las abstracciones de paginacion propias del
 * dominio (CriterioPaginacion / Pagina) y las de Spring Data (Pageable / Page).
 * Vive en infrastructure para que domain y application no dependan de Spring Data.
 */
public final class PaginacionMapper {

    private PaginacionMapper() {
    }

    public static Pageable aPageable(CriterioPaginacion criterio, String ordenPorDefecto) {
        String campoOrden = criterio.ordenarPor() != null ? criterio.ordenarPor() : ordenPorDefecto;
        Sort sort = Sort.by(criterio.ascendente() ? Sort.Direction.ASC : Sort.Direction.DESC, campoOrden);
        return PageRequest.of(criterio.numeroPagina(), criterio.tamanoPagina(), sort);
    }

    public static <J, D> Pagina<D> aPagina(Page<J> pagina, Function<J, D> mapper) {
        List<D> contenido = pagina.getContent().stream().map(mapper).toList();
        return Pagina.de(contenido, pagina.getTotalElements(), pagina.getNumber(), pagina.getSize());
    }
}
