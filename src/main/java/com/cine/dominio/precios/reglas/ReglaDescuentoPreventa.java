package com.cine.dominio.precios.reglas;

import com.cine.dominio.Butaca;
import com.cine.dominio.Funcion;
import com.cine.dominio.EstadoPelicula;
import com.cine.dominio.precios.ReglaPrecio;

/**
 * Aplica un descuento configurable a funciones en período de preventa.
 * Si la película está en estado PREVENTA, se aplica el porcentaje de descuento
 * definido al construir la regla.
 *
 * Justificación de negocio: incentivar la compra anticipada antes del estreno.
 *
 * Uso:
 *   motor.addRule(new ReglaDescuentoPreventa(0.20)); // 20% de descuento en preventa
 */
public class ReglaDescuentoPreventa implements ReglaPrecio {

    private final double discountRate; // Ej: 0.20 = 20% de descuento

    /**
     * @param discountRate Porcentaje de descuento como decimal (0.0 a 1.0).
     *                     Ej: 0.15 = 15% de descuento.
     */
    public ReglaDescuentoPreventa(double discountRate) {
        if (discountRate < 0 || discountRate > 1) {
            throw new IllegalArgumentException(
                "discountRate debe estar entre 0.0 y 1.0 (ej: 0.20 para 20%)"
            );
        }
        this.discountRate = discountRate;
    }

    @Override
    public double aplicar(Butaca butaca, Funcion funcion, double currentPrice) {
        if (funcion.getPelicula().getEstado() == EstadoPelicula.PREVENTA) {
            return currentPrice * (1.0 - discountRate);
        }
        return currentPrice;
    }
}
