package com.cine.dominio;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa una función de cine: la proyección de una película específica
 * en una sala específica, en un horario y formato determinados.
 *
 * Precio final de cada butaca (calculado por MotorPrecios, no por Funcion):
 *   basePrice × FormatoFuncion.priceMultiplier × TipoButaca.priceMultiplier
 *
 * Funcion NO administra el estado de las butacas — eso es responsabilidad
 * de Butaca. Tampoco calcula precios finales — eso le compete a MotorPrecios.
 * Su rol es ser la fuente de datos de una función para que otros la consulten.
 */
public class Funcion {

    private final String id;
    private final SalaCine sala;
    private final Pelicula pelicula;
    private final LocalDateTime startTime;
    private final FormatoFuncion format;
    private double basePrice;      // Precio base antes de multiplicadores
    private boolean cancelled;     // Permite cancelar sin borrar el registro

    /**
     * @param sala      Sala donde se proyecta la función
     * @param pelicula     Película a proyectar
     * @param startTime Hora de inicio de la función
     * @param format    Formato de proyección (STANDARD, 3D, VIP)
     * @param basePrice Precio base de la función (debe ser positivo)
     */
    public Funcion(SalaCine sala, Pelicula pelicula, LocalDateTime startTime,
                    FormatoFuncion format, double basePrice) {

        if (sala == null) {
            throw new IllegalArgumentException("La sala (sala) no puede ser nula");
        }
        if (pelicula == null) {
            throw new IllegalArgumentException("La película (pelicula) no puede ser nula");
        }
        if (!pelicula.canHaveShowtime()) {
            throw new IllegalStateException(
                String.format("No se puede crear una función para '%s': su estado es %s. " +
                              "La película debe estar en PREVENTA o VENTA.",
                              pelicula.getTitulo(), pelicula.getEstado())
            );
        }
        if (startTime == null) {
            throw new IllegalArgumentException("La hora de inicio (startTime) no puede ser nula");
        }
        if (format == null) {
            throw new IllegalArgumentException("El formato (format) no puede ser nulo");
        }
        if (basePrice <= 0) {
            throw new IllegalArgumentException("El precio base (basePrice) debe ser positivo");
        }

        this.id = UUID.randomUUID().toString();
        this.sala = sala;
        this.pelicula = pelicula;
        this.startTime = startTime;
        this.format = format;
        this.basePrice = basePrice;
        this.cancelled = false;
    }

    /**
     * Constructor para restaurar desde la base de datos.
     */
    public Funcion(String id, SalaCine sala, Pelicula pelicula, LocalDateTime startTime,
                   FormatoFuncion format, double basePrice, boolean cancelled) {
        this.id = id;
        this.sala = sala;
        this.pelicula = pelicula;
        this.startTime = startTime;
        this.format = format;
        this.basePrice = basePrice;
        this.cancelled = cancelled;
    }

    // --- Consultas de tiempo ---

    /**
     * Calcula la hora de fin de la función a partir de la duración de la película.
     * Se recalcula en cada llamada — si la duración de la película cambia,
     * este método refleja el valor actualizado automáticamente.
     */
    public LocalDateTime getEndTime() {
        return startTime.plusMinutes(pelicula.getDurationMinutes());
    }

    /**
     * Responde si esta función se solapa en tiempo con otra en la misma sala.
     * Dos funciones se solapan si una empieza antes de que la otra termine.
     * Quien gestione la colección de funciones (repositorio o servicio)
     * debe llamar este método antes de persistir una nueva función.
     *
     * @param other La otra función a comparar
     * @return true si hay solapamiento de horario
     */
    public boolean overlapsWith(Funcion other) {
        if (other == null) return false;
        // Solapamiento: this empieza antes de que other termine,
        // Y other empieza antes de que this termine
        return this.startTime.isBefore(other.getEndTime())
                && other.startTime.isBefore(this.getEndTime());
    }

    /**
     * Responde si esta función está en la misma sala que otra.
     * Útil para filtrar antes de verificar solapamientos.
     */
    public boolean isInSameRoom(Funcion other) {
        if (other == null) return false;
        return this.sala.getId().equals(other.sala.getId());
    }

    /**
     * Responde si esta función colisiona con otra (misma sala + solapamiento de horario).
     * Método de conveniencia que combina isInSameRoom() + overlapsWith().
     */
    public boolean conflictsWith(Funcion other) {
        return isInSameRoom(other) && overlapsWith(other);
    }

    // --- Consultas de estado ---

    /**
     * Responde si la función todavía no ha comenzado (útil para validar compras).
     */
    public boolean isUpcoming() {
        return !cancelled && LocalDateTime.now().isBefore(startTime);
    }

    /**
     * Responde si la función está activa (no cancelada).
     */
    public boolean isActive() {
        return !cancelled;
    }

    /**
     * Cancela la función. No borra el registro — preserva historial de ventas.
     */
    public void cancel() {
        if (this.cancelled) {
            throw new IllegalStateException(
                String.format("La función del %s ya está cancelada", startTime)
            );
        }
        this.cancelled = true;
    }

    // --- Getters ---

    public String getId() {
        return id;
    }

    public SalaCine getSala() {
        return sala;
    }

    public Pelicula getPelicula() {
        return pelicula;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public FormatoFuncion getFormat() {
        return format;
    }

    public double getPrecioBase() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        if (basePrice <= 0) {
            throw new IllegalArgumentException("El precio base (basePrice) debe ser positivo");
        }
        this.basePrice = basePrice;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public String toString() {
        return String.format("Funcion{id='%s', pelicula='%s', sala='%s', start=%s, format=%s, price=%.2f}",
                id, pelicula.getTitulo(), sala.getNombre(), startTime, format.getDisplayName(), basePrice);
    }
}
