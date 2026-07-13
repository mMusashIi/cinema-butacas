package com.cine.dominio;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Representa la disponibilidad de una película en un local de cine específico.
 * Es el vínculo entre Pelicula y Cine — responde la pregunta:
 * "¿Desde cuándo está disponible esta película en este local?"
 *
 * Es una entidad de asociación, no un value object puro, porque:
 *   - Tiene identidad propia (dos registros distintos para misma película
 *     en dos locales son entidades separadas).
 *   - Puede tener ciclo de vida propio (se puede deshabilitar sin borrar).
 *
 * Pelicula no conoce esta clase — quien necesita saber en qué locales
 * está disponible una película consulta la colección de DisponibilidadPelicula,
 * no le pregunta a Pelicula.
 */
public class DisponibilidadPelicula {

    private final String id;
    private final Pelicula pelicula;
    private final Cine cinema;
    private final LocalDate availableFrom; // Fecha desde la que está disponible en este local
    private boolean active;               // Permite deshabilitar sin borrar el registro

    /**
     * @param pelicula         La película que se habilita en el local
     * @param cinema        El local donde estará disponible
     * @param availableFrom Fecha desde la que puede programarse en este local
     */
    public DisponibilidadPelicula(Pelicula pelicula, Cine cinema, LocalDate availableFrom) {
        if (pelicula == null) {
            throw new IllegalArgumentException("La película (pelicula) no puede ser nula");
        }
        if (cinema == null) {
            throw new IllegalArgumentException("El local (cinema) no puede ser nulo");
        }
        if (!cinema.isActive()) {
            throw new IllegalStateException(
                String.format("No se puede asignar película al local '%s' porque está inactivo", cinema.getNombre())
            );
        }
        if (pelicula.getEstado().isTerminal()) {
            throw new IllegalStateException(
                String.format("No se puede asignar la película '%s' porque está retirada", pelicula.getTitulo())
            );
        }
        if (availableFrom == null) {
            throw new IllegalArgumentException("La fecha de disponibilidad (availableFrom) no puede ser nula");
        }

        this.id = UUID.randomUUID().toString();
        this.pelicula = pelicula;
        this.cinema = cinema;
        this.availableFrom = availableFrom;
        this.active = true;
    }

    // --- Consultas de negocio ---

    /**
     * Responde si la película está actualmente disponible para programar
     * funciones en este local (combinando la fecha, el estado del local
     * y el estado de la película).
     */
    public boolean isAvailableToday() {
        return active
                && cinema.isActive()
                && !pelicula.getEstado().isTerminal()
                && !LocalDate.now().isBefore(availableFrom);
    }

    /**
     * Responde si la disponibilidad aplica para una fecha concreta.
     * Útil al programar funciones (Funcion) con fecha futura.
     */
    public boolean isAvailableOn(LocalDate date) {
        if (date == null) return false;
        return active
                && cinema.isActive()
                && !pelicula.getEstado().isTerminal()
                && !date.isBefore(availableFrom);
    }

    /**
     * Deshabilita la disponibilidad de la película en este local.
     * No borra el registro — preserva el historial.
     */
    public void disable() {
        this.active = false;
    }

    /**
     * Reactiva la disponibilidad si fue deshabilitada previamente.
     * Solo funciona si el local y la película siguen operativos.
     */
    public void enable() {
        if (!cinema.isActive()) {
            throw new IllegalStateException(
                String.format("No se puede reactivar: el local '%s' está inactivo", cinema.getNombre())
            );
        }
        if (pelicula.getEstado().isTerminal()) {
            throw new IllegalStateException(
                String.format("No se puede reactivar: la película '%s' está retirada", pelicula.getTitulo())
            );
        }
        this.active = true;
    }

    // --- Getters ---

    public String getId() {
        return id;
    }

    public Pelicula getPelicula() {
        return pelicula;
    }

    public Cine getCinema() {
        return cinema;
    }

    public LocalDate getAvailableFrom() {
        return availableFrom;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return String.format("DisponibilidadPelicula{pelicula='%s', cinema='%s', from=%s, active=%b}",
                pelicula.getTitulo(), cinema.getNombre(), availableFrom, active);
    }
}
