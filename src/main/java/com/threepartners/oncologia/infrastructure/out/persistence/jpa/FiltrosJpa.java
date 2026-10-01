package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

/**
 * Filtros opcionales para listados: cada criterio solo se agrega si tiene
 * valor. Evita las consultas "(:p IS NULL OR campo = :p)", con las que
 * PostgreSQL no puede inferir el tipo de un parametro nulo.
 */
final class FiltrosJpa {

    private FiltrosJpa() {
    }

    static <T> Specification<T> igual(String campo, Object valor) {
        return valor == null ? null : (raiz, consulta, cb) -> cb.equal(raiz.get(campo), valor);
    }

    static <T> Specification<T> desde(String campo, Instant inicio) {
        return inicio == null ? null : (raiz, consulta, cb) -> cb.greaterThanOrEqualTo(raiz.get(campo), inicio);
    }

    static <T> Specification<T> antesDe(String campo, Instant finExclusivo) {
        return finExclusivo == null ? null : (raiz, consulta, cb) -> cb.lessThan(raiz.get(campo), finExclusivo);
    }

    @SafeVarargs
    static <T> Specification<T> todas(Specification<T>... criterios) {
        Specification<T> resultado = (raiz, consulta, cb) -> cb.conjunction();
        for (Specification<T> criterio : criterios) {
            if (criterio != null) {
                resultado = resultado.and(criterio);
            }
        }
        return resultado;
    }
}
