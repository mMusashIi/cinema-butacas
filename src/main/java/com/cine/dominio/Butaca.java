package com.cine.dominio;

/**
 * Representa una butaca concreta dentro de una sala específica.
 * Es la entidad central con estado mutable y reglas de negocio activas.
 */
public class Butaca {

    private final String row;
    private final int number;
    private final TipoButaca tipo;

    // // [RUTINA]: Constructor principal.
    // [PARADIGMA POO]: Inicializa el objeto validando su estado inicial.
    // Garantiza que ninguna butaca se cree con datos inválidos (fail-fast).
    public Butaca(String row, int number, TipoButaca tipo) {
        if (row == null || row.trim().isEmpty()) {
            throw new IllegalArgumentException("La fila (row) no puede estar vacía");
        }
        if (number <= 0) {
            throw new IllegalArgumentException("El número (number) debe ser positivo");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de butaca (tipo) no puede ser nulo");
        }

        this.row = row;
        this.number = number;
        this.tipo = tipo;
    }

    // --- Getters (Ningún setter público genérico) ---
    // [PARADIGMA POO]: Inmutabilidad. Una vez creada, la butaca física no cambia de
    // lugar.

    // // [RUTINA]: Obtener identificador único (Fila + Número).
    public String getId() {
        return row + number;
    }

    // // [RUTINA]: Obtener la letra de la fila (ej. 'A').
    public String getFila() {
        return row;
    }

    // // [RUTINA]: Obtener el número de butaca en la fila.
    public int getNumero() {
        return number;
    }

    // // [RUTINA]: Obtener el tipo físico de butaca (VIP, Normal, Pasillo).
    public TipoButaca getTipo() {
        return tipo;
    }
}
