package com.cine.dominio.precios;

import com.cine.dominio.EstadoPelicula;
import com.cine.dominio.Butaca;
import com.cine.dominio.Funcion;
import com.cine.dominio.FormatoFuncion;
import com.cine.dominio.TipoButaca;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Constructor de reglas de precio desde parámetros configurables.
 * Permite crear reglas temporales (ReglaPrecio) o permanentes (ReglaPrecioNombrada)
 * sin escribir ninguna clase Java nueva.
 *
 * Condiciones disponibles (se combinan con lógica AND — todas deben cumplirse):
 *   - Días de la semana
 *   - Rango horario (antes de / después de)
 *   - Tipos de butaca
 *   - Formatos de proyección
 *   - Estado de la película
 *
 * Acción:
 *   - Porcentaje de descuento (descuento, no recargo)
 *
 * Uso para regla temporal (sesión de venta única):
 *   ReglaPrecio r = new ConstructorReglaPrecio()
 *       .onDays(DayOfWeek.FRIDAY)
 *       .beforeHour(18)
 *       .discount(0.15)
 *       .build();
 *
 * Uso para regla permanente (catálogo gestionado):
 *   ReglaPrecioNombrada r = new ConstructorReglaPrecio()
 *       .onDays(DayOfWeek.WEDNESDAY)
 *       .discount(0.50)
 *       .buildNamed("Día del espectador", "50% off todos los miércoles");
 */
public class ConstructorReglaPrecio {

    // --- Condiciones (null = sin restricción / aplica siempre) ---
    private Set<DayOfWeek> days = null;
    private Integer beforeHour = null;    // Aplica si la función empieza ANTES de esta hora
    private Integer afterHour = null;     // Aplica si la función empieza DESDE esta hora
    private Set<String> seatTypeCodes = null;  // Códigos de TipoButaca (ej. "VIP", "SILLA_RUEDAS")
    private Set<FormatoFuncion> formats = null;
    private Set<EstadoPelicula> movieStatuses = null;

    // --- Acción ---
    private double discountRate = 0.0; // 0.0 = sin descuento, 1.0 = gratis

    // --- Condiciones de día ---

    public ConstructorReglaPrecio onDays(DayOfWeek... days) {
        this.days = EnumSet.copyOf(Arrays.asList(days));
        return this;
    }

    // --- Condiciones de horario ---

    /**
     * La regla aplica solo si la función comienza ANTES de la hora indicada (exclusivo).
     * Ejemplo: beforeHour(14) → funciones que empiezan antes de las 14:00.
     */
    public ConstructorReglaPrecio beforeHour(int hour) {
        if (hour < 0 || hour > 23) throw new IllegalArgumentException("hour debe estar entre 0 y 23");
        this.beforeHour = hour;
        return this;
    }

    /**
     * La regla aplica solo si la función comienza DESDE la hora indicada (inclusivo).
     * Ejemplo: fromHour(20) → funciones nocturnas desde las 20:00.
     */
    public ConstructorReglaPrecio fromHour(int hour) {
        if (hour < 0 || hour > 23) throw new IllegalArgumentException("hour debe estar entre 0 y 23");
        this.afterHour = hour;
        return this;
    }

    // --- Condiciones de tipo de butaca ---

    /**
     * La regla aplica solo a butacas de los tipos indicados.
     * Ejemplo: forSeatTypes(TipoButaca.VIP, TipoButaca.PAREJA)
     */
    public ConstructorReglaPrecio forSeatTypes(TipoButaca... types) {
        this.seatTypeCodes = new java.util.HashSet<>();
        for (TipoButaca t : types) seatTypeCodes.add(t.code());
        return this;
    }

    // --- Condiciones de formato ---

    public ConstructorReglaPrecio forFormats(FormatoFuncion... formats) {
        this.formats = EnumSet.copyOf(Arrays.asList(formats));
        return this;
    }

    // --- Condiciones de estado de película ---

    public ConstructorReglaPrecio forMovieStatuses(EstadoPelicula... statuses) {
        this.movieStatuses = EnumSet.copyOf(Arrays.asList(statuses));
        return this;
    }

    // --- Acción ---

    /**
     * Define el porcentaje de descuento que aplica cuando se cumplen todas las condiciones.
     * @param rate Decimal entre 0.0 y 1.0 (ej: 0.20 = 20% de descuento).
     */
    public ConstructorReglaPrecio discount(double rate) {
        if (rate < 0 || rate > 1) throw new IllegalArgumentException("discount debe estar entre 0.0 y 1.0");
        this.discountRate = rate;
        return this;
    }

    // --- Construcción ---

    /**
     * Construye una ReglaPrecio anónima (temporal).
     * Útil para aplicarla en una sola sesión de venta sin guardarla en el catálogo.
     */
    public ReglaPrecio build() {
        final Set<DayOfWeek> finalDays = days;
        final Integer finalBeforeHour = beforeHour;
        final Integer finalAfterHour = afterHour;
        final Set<String> finalSeatCodes = seatTypeCodes;
        final Set<FormatoFuncion> finalFormats = formats;
        final Set<EstadoPelicula> finalStatuses = movieStatuses;
        final double rate = discountRate;

        return (Butaca butaca, Funcion funcion, double currentPrice) -> {
            if (!matchesConditions(butaca, funcion,
                    finalDays, finalBeforeHour, finalAfterHour,
                    finalSeatCodes, finalFormats, finalStatuses)) {
                return currentPrice;
            }
            return Math.max(0.0, currentPrice * (1.0 - rate));
        };
    }

    /**
     * Construye una ReglaPrecioNombrada permanente con nombre y descripción.
     * La regla queda registrada con un id único y puede activarse/desactivarse.
     *
     * @param name        Nombre visible en la UI (ej: "Promoción Verano 2025")
     * @param description Descripción de las condiciones (ej: "20% off los viernes antes de las 6pm")
     */
    public ReglaPrecioNombrada buildNamed(String name, String description) {
        return new ReglaPrecioNombrada(name, description, build());
    }

    // --- Evaluación de condiciones (lógica AND) ---

    private boolean matchesConditions(Butaca butaca, Funcion funcion,
                                      Set<DayOfWeek> days, Integer beforeHour, Integer afterHour,
                                      Set<String> seatCodes, Set<FormatoFuncion> formats,
                                      Set<EstadoPelicula> statuses) {
        // Día de la semana
        if (days != null && !days.contains(funcion.getStartTime().getDayOfWeek())) return false;

        // Rango horario
        int hour = funcion.getStartTime().getHour();
        if (beforeHour != null && hour >= beforeHour) return false;
        if (afterHour != null && hour < afterHour) return false;

        // Tipo de butaca
        if (seatCodes != null && !seatCodes.contains(butaca.getTipo().code())) return false;

        // Formato de proyección
        if (formats != null && !formats.contains(funcion.getFormat())) return false;

        // Estado de la película
        if (statuses != null && !statuses.contains(funcion.getPelicula().getEstado())) return false;

        return true; // todas las condiciones se cumplieron
    }
}
