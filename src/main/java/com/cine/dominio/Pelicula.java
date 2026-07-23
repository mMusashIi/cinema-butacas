package com.cine.dominio;

import java.util.UUID;

/**
 * Representa una película que será proyectada en una función.
 */
public class Pelicula {
    private final String id;
    private final String nombre;
    private final int duracionMinutos;

    public Pelicula(String nombre, int duracionMinutos) {
        this(nombre, nombre, duracionMinutos);
    }

    public Pelicula(String id, String nombre, int duracionMinutos) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (duracionMinutos <= 0) {
            throw new IllegalArgumentException("La duración debe ser mayor a 0");
        }
        this.id = id;
        this.nombre = nombre;
        this.duracionMinutos = duracionMinutos;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    @Override
    public String toString() {
        return nombre + " (" + duracionMinutos + " min)";
    }
}
