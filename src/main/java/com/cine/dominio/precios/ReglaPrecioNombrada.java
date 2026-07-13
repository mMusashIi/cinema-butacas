package com.cine.dominio.precios;

import com.cine.dominio.Butaca;
import com.cine.dominio.Funcion;

import java.util.UUID;

/**
 * Entidad que representa una regla de precio con identidad propia.
 * A diferencia de una ReglaPrecio anónima (lambda), una ReglaPrecioNombrada:
 *   - Tiene un id único (para referencias en historial/BD futura)
 *   - Tiene nombre y descripción legibles para la UI
 *   - Puede activarse y desactivarse sin eliminarse
 *
 * Si una regla fue aplicada a boletas ya emitidas, NO debe borrarse —
 * solo desactivarse, para preservar el historial de cómo se calculó
 * el precio en esas boletas (mismo principio que Pelicula.retire() y Cine.deactivate()).
 *
 * La lógica real de la regla vive en una instancia de ReglaPrecio
 * (puede ser una lambda, o una instancia de las clases en reglas/).
 */
public class ReglaPrecioNombrada {

    private final String id;
    private String name;
    private String description;
    private final ReglaPrecio logic; // La función de cálculo real (inmutable)
    private boolean active;

    /**
     * @param name        Nombre legible para la UI (ej: "Descuento Miércoles")
     * @param description Descripción de cuándo aplica (ej: "50% off todos los miércoles")
     * @param logic       La lógica de cálculo — puede ser lambda o clase de reglas/
     */
    public ReglaPrecioNombrada(String name, String description, ReglaPrecio logic) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la regla no puede estar vacío");
        }
        if (logic == null) {
            throw new IllegalArgumentException("La lógica (logic) no puede ser nula");
        }
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description != null ? description : "";
        this.logic = logic;
        this.active = true; // Nace activa
    }

    // --- Delegación de cálculo ---

    /**
     * Aplica la regla si está activa.
     * Si está inactiva, devuelve currentPrice sin modificar — pasa de largo.
     */
    public double aplicar(Butaca butaca, Funcion funcion, double currentPrice) {
        if (!active) return currentPrice;
        return logic.aplicar(butaca, funcion, currentPrice);
    }

    // --- Ciclo de vida ---

    /**
     * Desactiva la regla. No la elimina — preserva historial.
     * Las boletas ya emitidas con esta regla aplicada siguen siendo válidas.
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Reactiva la regla previamente desactivada.
     */
    public void activate() {
        this.active = true;
    }

    // --- Edición ---

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    // --- Getters ---

    public String getId() { return id; }
    public String getNombre() { return name; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }

    @Override
    public String toString() {
        return String.format("ReglaPrecioNombrada{name='%s', active=%b}", name, active);
    }
}
