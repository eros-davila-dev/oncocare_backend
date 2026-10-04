package com.threepartners.oncologia.domain.paciente;

/**
 * Comparacion de telefonos escritos de formas distintas: "+51 904 049 494",
 * "51904049494" y "904-049-494" son el mismo numero. Se usan los ultimos 9
 * digitos (numero movil peruano sin codigo de pais). La misma regla vive en
 * el indice de la migracion V13.
 */
public final class Telefono {

    public static final int DIGITOS = 9;

    private Telefono() {
    }

    /** Ultimos 9 digitos, o "" si el numero tiene menos. */
    public static String normalizar(String telefono) {
        if (telefono == null) {
            return "";
        }
        String digitos = telefono.replaceAll("[^0-9]", "");
        return digitos.length() < DIGITOS ? "" : digitos.substring(digitos.length() - DIGITOS);
    }

    public static boolean mismos(String a, String b) {
        String x = normalizar(a);
        return !x.isEmpty() && x.equals(normalizar(b));
    }
}
