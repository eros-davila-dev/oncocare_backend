package com.threepartners.oncologia.domain.estudio;

import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import org.apache.commons.math3.special.Erf;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Prueba de rangos con signo de Wilcoxon para muestras relacionadas, con las
 * mismas convenciones que SPSS (la herramienta oficial de la tesis), para que
 * la vista previa del sistema y la salida de SPSS coincidan:
 *
 * - diferencia = postest - pretest; las diferencias cero se descartan (empates).
 * - rangos promedio para |d| repetidos y correccion de la varianza por empates.
 * - Z con el menor de los dos totales de rangos y sin correccion por
 *   continuidad (por eso Z sale siempre <= 0, como en SPSS).
 * - p asintotica bilateral y, para n <= 50, p exacta bilateral condicionada a
 *   los rangos observados (equivale a "Exacta" de SPSS con muestras pequenas).
 *
 * Es una vista preliminar: el analisis que se reporta en la tesis se hace en
 * SPSS con los datos exportados.
 */
public final class PruebaWilcoxon {

    static final int N_MAXIMO_EXACTO = 50;

    private PruebaWilcoxon() {
    }

    /** pretest[i] y postest[i] son del mismo participante; ambos sin nulos. */
    public static ResultadoWilcoxon calcular(double[] pretest, double[] postest) {
        if (pretest.length != postest.length) {
            throw new ValidacionDeNegocioException("La prueba de Wilcoxon requiere pares completos");
        }
        int pares = pretest.length;
        if (pares == 0) {
            return ResultadoWilcoxon.sinDatos();
        }

        double[] diferencias = new double[pares];
        int n = 0;
        for (int i = 0; i < pares; i++) {
            double d = postest[i] - pretest[i];
            if (d != 0) {
                diferencias[n++] = d;
            }
        }
        diferencias = Arrays.copyOf(diferencias, n);
        Double medianaPre = mediana(pretest);
        Double medianaPost = mediana(postest);

        if (n == 0) {
            // Sin ninguna diferencia no hay evidencia de cambio: SPSS informa Z = 0 y p = 1.
            return new ResultadoWilcoxon(pares, pares, 0, 0, 0, 0.0, 0.0, medianaPre, medianaPost,
                    0.0, 1.0, 1.0, 0.0);
        }

        Rangos rangos = rangosPromedio(diferencias);
        double sumaPositivos = 0;
        double sumaNegativos = 0;
        int positivos = 0;
        for (int i = 0; i < n; i++) {
            if (diferencias[i] > 0) {
                sumaPositivos += rangos.valores[i];
                positivos++;
            } else {
                sumaNegativos += rangos.valores[i];
            }
        }

        double media = n * (n + 1) / 4.0;
        double varianza = n * (n + 1) * (2.0 * n + 1) / 24.0 - rangos.correccionEmpates / 48.0;
        double z = varianza > 0 ? (Math.min(sumaPositivos, sumaNegativos) - media) / Math.sqrt(varianza) : 0.0;
        double pAsintotica = Math.min(1.0, Erf.erfc(Math.abs(z) / Math.sqrt(2)));
        Double pExacta = n <= N_MAXIMO_EXACTO ? pExacta(rangos.valores, sumaPositivos) : null;

        return new ResultadoWilcoxon(pares, pares - n, n, n - positivos, positivos, sumaNegativos, sumaPositivos,
                medianaPre, medianaPost, z, pAsintotica, pExacta, Math.abs(z) / Math.sqrt(pares));
    }

    /**
     * Distribucion exacta de W+ bajo H0 (cada signo con probabilidad 1/2),
     * condicionada a los rangos observados, asi que tambien vale con empates.
     * Los rangos promedio son multiplos de 0,5: se duplican para trabajar con
     * enteros en una programacion dinamica sobre las sumas posibles.
     */
    private static double pExacta(double[] rangos, double sumaPositivos) {
        int[] dobles = new int[rangos.length];
        int total = 0;
        for (int i = 0; i < rangos.length; i++) {
            dobles[i] = (int) Math.round(rangos[i] * 2);
            total += dobles[i];
        }
        // Frecuencias como double: 2^50 combinaciones caben sin perder precision.
        double[] frecuencias = new double[total + 1];
        frecuencias[0] = 1;
        int alcanzado = 0;
        for (int r : dobles) {
            for (int s = alcanzado; s >= 0; s--) {
                if (frecuencias[s] != 0) {
                    frecuencias[s + r] += frecuencias[s];
                }
            }
            alcanzado += r;
        }
        int observado = (int) Math.round(sumaPositivos * 2);
        double combinaciones = Math.pow(2, rangos.length);
        double hastaObservado = 0;
        for (int s = 0; s <= observado; s++) {
            hastaObservado += frecuencias[s];
        }
        double desdeObservado = combinaciones - hastaObservado + frecuencias[observado];
        return Math.min(1.0, 2 * Math.min(hastaObservado, desdeObservado) / combinaciones);
    }

    private static Rangos rangosPromedio(double[] diferencias) {
        int n = diferencias.length;
        Integer[] orden = new Integer[n];
        for (int i = 0; i < n; i++) {
            orden[i] = i;
        }
        Arrays.sort(orden, Comparator.comparingDouble(i -> Math.abs(diferencias[i])));

        double[] rangos = new double[n];
        double correccion = 0;
        int i = 0;
        while (i < n) {
            int j = i;
            while (j + 1 < n && Math.abs(diferencias[orden[j + 1]]) == Math.abs(diferencias[orden[i]])) {
                j++;
            }
            double rangoPromedio = (i + j + 2) / 2.0;
            for (int k = i; k <= j; k++) {
                rangos[orden[k]] = rangoPromedio;
            }
            int empatados = j - i + 1;
            correccion += (double) empatados * empatados * empatados - empatados;
            i = j + 1;
        }
        return new Rangos(rangos, correccion);
    }

    private static Double mediana(double[] valores) {
        double[] ordenados = valores.clone();
        Arrays.sort(ordenados);
        int m = ordenados.length / 2;
        return ordenados.length % 2 == 1 ? ordenados[m] : (ordenados[m - 1] + ordenados[m]) / 2;
    }

    private record Rangos(double[] valores, double correccionEmpates) {
    }
}
