package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros opcionales para listados: cada criterio solo se agrega si tiene
 * valor. Evita las consultas "(:p IS NULL OR campo = :p)", con las que
 * PostgreSQL falla ("no se pudo determinar el tipo del parametro") cuando el
 * parametro llega nulo, segun su tipo y la combinacion de filtros.
 */
final class FiltrosJpa {

    private FiltrosJpa() {
    }

    static <T> Specification<T> igual(String campo, Object valor) {
        return valor == null ? null : (raiz, consulta, cb) -> cb.equal(raiz.get(campo), valor);
    }

    static <T, V extends Comparable<? super V>> Specification<T> desde(String campo, V inicio) {
        return inicio == null ? null : (raiz, consulta, cb) -> cb.greaterThanOrEqualTo(raiz.<V>get(campo), inicio);
    }

    static <T, V extends Comparable<? super V>> Specification<T> hasta(String campo, V fin) {
        return fin == null ? null : (raiz, consulta, cb) -> cb.lessThanOrEqualTo(raiz.<V>get(campo), fin);
    }

    static <T, V extends Comparable<? super V>> Specification<T> antesDe(String campo, V finExclusivo) {
        return finExclusivo == null ? null : (raiz, consulta, cb) -> cb.lessThan(raiz.<V>get(campo), finExclusivo);
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
