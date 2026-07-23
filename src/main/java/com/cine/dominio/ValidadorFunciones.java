package com.cine.dominio;

import java.util.List;

/**
 * Lógica funcional para validar la creación de funciones.
 */
public class ValidadorFunciones {

    /**
     * Verifica si una nueva función propuesta se solapa con las existentes (misma sala).
     * Compara solo horas — las funciones son plantillas sin fecha.
     */
    public static boolean haySolapamiento(List<Funcion> existentes, Funcion propuesta) {
        return existentes.stream()
                .filter(f -> f.isActivo() && !f.isEliminada())
                .filter(f -> f.getSala().getId().equals(propuesta.getSala().getId()))
                .anyMatch(f -> seSolapan(f, propuesta));
    }

    private static boolean seSolapan(Funcion f1, Funcion f2) {
        java.time.LocalTime inicio1 = f1.getHoraInicio();
        java.time.LocalTime fin1    = f1.getHoraFin();
        java.time.LocalTime inicio2 = f2.getHoraInicio();
        java.time.LocalTime fin2    = f2.getHoraFin();

        java.time.LocalTime maxInicio = inicio1.isAfter(inicio2) ? inicio1 : inicio2;
        java.time.LocalTime minFin    = fin1.isBefore(fin2) ? fin1 : fin2;

        return maxInicio.isBefore(minFin);
    }
}
