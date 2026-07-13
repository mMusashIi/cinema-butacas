package com.cine.dominio.precios.reglas;

import com.cine.dominio.Butaca;
import com.cine.dominio.Funcion;
import com.cine.dominio.precios.ReglaPrecio;

/**
 * Aplica un descuento fijo configurable a butacas accesibles (SILLA_RUEDAS).
 * Las butacas marcadas como accesibles (TipoButaca.accessible() == true)
 * reciben el porcentaje de descuento definido.
 *
 * Justificación de negocio: política de inclusión — precio preferencial
 * para butacas de accesibilidad para no incentivar su uso por personas
 * que no las necesitan y al mismo tiempo reducir la barrera económica.
 *
 * Uso:
 *   motor.addRule(new ReglaDescuentoAccesible(0.30)); // 30% off en SILLA_RUEDAS
 */
public class ReglaDescuentoAccesible implements ReglaPrecio {

    private final double discountRate;

    /**
     * @param discountRate Porcentaje de descuento (0.0–1.0).
     *                     Ej: 1.0 = gratis, 0.5 = mitad de precio.
     */
    public ReglaDescuentoAccesible(double discountRate) {
        if (discountRate < 0 || discountRate > 1) {
            throw new IllegalArgumentException("discountRate debe estar entre 0.0 y 1.0");
        }
        this.discountRate = discountRate;
    }

    @Override
    public double aplicar(Butaca butaca, Funcion funcion, double currentPrice) {
        if (butaca.getTipo().accessible()) {
            return currentPrice * (1.0 - discountRate);
        }
        return currentPrice;
    }
}
