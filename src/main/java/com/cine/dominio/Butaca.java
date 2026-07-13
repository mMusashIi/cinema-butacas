package com.cine.dominio;

import java.util.UUID;

/**
 * Representa una butaca concreta dentro de una sala específica.
 * Es la entidad central con estado mutable y reglas de negocio activas.
 */
public class Butaca {
    
    private final String id;
    private final String row;
    private final int number;
    private final TipoButaca tipo;
    private EstadoButaca estado;

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
        this.id = UUID.randomUUID().toString();
        this.tipo = tipo;
        this.estado = EstadoButaca.FREE; // Estado inicial por defecto
    }

    /**
     * Constructor para restaurar desde la base de datos (con ID existente).
     */
    public Butaca(String id, String row, int number, TipoButaca tipo) {
        this.id = id;
        this.row = row;
        this.number = number;
        this.tipo = tipo;
        this.estado = EstadoButaca.FREE; // El estado real se carga en la tabla de reservas de la función
    }

    // --- Métodos de Negocio (Transiciones de Estado) ---

    public void seleccionar() {
        changeStatus(EstadoButaca.SELECTED);
    }

    public void reservarButaca() {
        changeStatus(EstadoButaca.BOOKED);
    }

    public void liberar() {
        changeStatus(EstadoButaca.FREE);
    }

    public void markBroken() {
        changeStatus(EstadoButaca.BROKEN);
    }

    public void repair() {
        changeStatus(EstadoButaca.FREE);
    }

    /**
     * Método interno centralizado para realizar el cambio de estado,
     * apoyándose en la lógica de validación de EstadoButaca.
     */
    private void changeStatus(EstadoButaca targetStatus) {
        if (!this.estado.canTransitionTo(targetStatus)) {
            throw new IllegalStateException(
                String.format("Transición inválida: No se puede cambiar la butaca %s de %s a %s.", 
                              id, this.estado, targetStatus)
            );
        }
        this.estado = targetStatus;
    }

    // --- Getters (Ningún setter público genérico) ---

    public String getId() {
        return id;
    }

    public String getFila() {
        return row;
    }

    public int getNumero() {
        return number;
    }

    public TipoButaca getTipo() {
        return tipo;
    }

    public EstadoButaca getEstado() {
        return estado;
    }
}
