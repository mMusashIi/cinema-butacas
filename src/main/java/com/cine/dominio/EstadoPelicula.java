package com.cine.dominio;

/**
 * Representa el estado de disponibilidad comercial de una película dentro del catálogo.
 *
 * Regla de negocio: la transición hacia RETIRADO puede ocurrir desde cualquiera
 * de los otros tres estados. La progresión PROXIMO -> PREVENTA -> VENTA es típicamente 
 * secuencial en el uso normal, pero esa validación le compete a la clase Pelicula.
 */
public enum EstadoPelicula {
    PROXIMO,
    PREVENTA,
    VENTA,
    RETIRADO;

    /**
     * Responde si, estando en este estado, se permite crear una nueva función (Funcion).
     */
    public boolean canCreateShowtime() {
        return this == PREVENTA || this == VENTA;
    }

    /**
     * Responde si la película debe mostrarse en la cartelera o listado general.
     */
    public boolean isVisibleInBillboard() {
        return this != RETIRADO;
    }

    /**
     * Responde si el estado es terminal (no se espera que cambie a otro estado
     * de forma normal dentro del flujo regular).
     */
    public boolean isTerminal() {
        return this == RETIRADO;
    }
}
