package com.cine.dominio;

import java.util.List;

/**
 * Lógica funcional para validar la creación de funciones.
 */
public class ValidadorFunciones {

    /**
     * Verifica si una nueva función propuesta se solapa con las funciones existentes.
     * 
     * @param existentes Lista de funciones que ya están programadas (idealmente en la misma sala).
     * @param propuesta La nueva función que se quiere crear.
     * @return true si hay choque de horarios, false si el horario está libre.
     */
    public static boolean haySolapamiento(List<Funcion> existentes, Funcion propuesta) {
        return existentes.stream()
                .filter(f -> f.getSala().getId().equals(propuesta.getSala().getId())) // Solo misma sala
                .anyMatch(f -> seSolapan(f, propuesta));
    }

    private static boolean seSolapan(Funcion f1, Funcion f2) {
        // Lógica de intervalos: [inicio1, fin1) se solapa con [inicio2, fin2) 
        // si max(inicio1, inicio2) < min(fin1, fin2)
        var inicio1 = f1.getHoraInicio();
        var fin1 = f1.getHoraFin();
        var inicio2 = f2.getHoraInicio();
        var fin2 = f2.getHoraFin();

        var maxInicio = inicio1.isAfter(inicio2) ? inicio1 : inicio2;
        var minFin = fin1.isBefore(fin2) ? fin1 : fin2;

        return maxInicio.isBefore(minFin);
    }
}
