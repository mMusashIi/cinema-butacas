package com.cine.dominio;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa una función programada en la cartelera.
 * Asocia una Película a una SalaCine en un horario específico.
 */
public class Funcion {
    private final String id;
    private final Pelicula pelicula;
    private final SalaCine sala;
    private final LocalDateTime horaInicio;

    public Funcion(Pelicula pelicula, SalaCine sala, LocalDateTime horaInicio) {
        this(pelicula.getNombre() + "-" + sala.getNombre() + "-" + horaInicio.toLocalTime().toString().replace(":", ""), pelicula, sala, horaInicio);
    }

    public Funcion(String id, Pelicula pelicula, SalaCine sala, LocalDateTime horaInicio) {
        if (pelicula == null) {
            throw new IllegalArgumentException("La función debe tener una película");
        }
        if (sala == null) {
            throw new IllegalArgumentException("La función debe tener una sala");
        }
        if (horaInicio == null) {
            throw new IllegalArgumentException("La función debe tener una hora de inicio");
        }

        this.id = id;
        this.pelicula = pelicula;
        this.sala = sala;
        this.horaInicio = horaInicio;
    }



    public String getId() {
        return id;
    }

    public Pelicula getPelicula() {
        return pelicula;
    }

    public SalaCine getSala() {
        return sala;
    }

    public LocalDateTime getHoraInicio() {
        return horaInicio;
    }

    public LocalDateTime getHoraFin() {
        return horaInicio.plusMinutes(pelicula.getDuracionMinutos());
    }

    /**
     * Verifica si la función ya terminó para una fecha específica (usualmente hoy).
     * @param fecha La fecha que se está consultando.
     * @return true si la función ocurre en esa fecha y la hora actual ya superó la hora de fin.
     */
    public boolean estaTerminada(java.time.LocalDate fecha) {
        java.time.LocalDate hoy = java.time.LocalDate.now();
        if (fecha.isBefore(hoy)) return true;
        if (fecha.equals(hoy)) {
            java.time.LocalTime ahora = java.time.LocalTime.now();
            java.time.LocalTime horaInicioLT = horaInicio.toLocalTime();
            
            if (horaInicioLT.isAfter(ahora)) return false;
            
            java.time.LocalTime horaFinLT = getHoraFin().toLocalTime();
            if (horaFinLT.isBefore(horaInicioLT)) {
                return false; 
            }
            return ahora.isAfter(horaFinLT);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s - %s (%s)", 
                horaInicio.toLocalTime().toString(), 
                pelicula.getNombre(), 
                sala.getNombre());
    }
}
