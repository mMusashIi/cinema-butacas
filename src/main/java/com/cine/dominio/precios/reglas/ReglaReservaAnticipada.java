package com.cine.dominio.precios.reglas;

import com.cine.dominio.Butaca;
import com.cine.dominio.Funcion;
import com.cine.dominio.precios.ReglaPrecio;

/**
 * Aplica un descuento configurable a funciones en horario de matiné.
 * Las funciones que comienzan antes de la hora límite configurada reciben el descuento.
 *
 * Uso:
 *   motor.addRule(new ReglaReservaAnticipada(14, 0.25)); // antes de las 14:00 → 25% off
 */
public class ReglaReservaAnticipada implements ReglaPrecio {

    private final int cutoffHour;      // Hora límite (0–23) en formato 24h
    private final double discountRate; // Porcentaje de descuento (0.0–1.0)

    /**
     * @param cutoffHour   Hora límite en formato 24h (ej: 14 = antes de las 2pm)
     * @param discountRate Porcentaje de descuento (ej: 0.25 = 25% de descuento)
     */
    public ReglaReservaAnticipada(int cutoffHour, double discountRate) {
        if (cutoffHour < 0 || cutoffHour > 23) {
            throw new IllegalArgumentException("cutoffHour debe estar entre 0 y 23");
        }
        if (discountRate < 0 || discountRate > 1) {
            throw new IllegalArgumentException("discountRate debe estar entre 0.0 y 1.0");
        }
        this.cutoffHour = cutoffHour;
        this.discountRate = discountRate;
    }

    @Override
    public double aplicar(Butaca butaca, Funcion funcion, double currentPrice) {
        int startHour = funcion.getStartTime().getHour();
        if (startHour < cutoffHour) {
            return currentPrice * (1.0 - discountRate);
        }
        return currentPrice;
    }
}
