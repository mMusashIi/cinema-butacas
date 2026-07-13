package com.cine.dominio.precios.reglas;

import com.cine.dominio.Butaca;
import com.cine.dominio.Funcion;
import com.cine.dominio.precios.ReglaPrecio;

import java.time.DayOfWeek;

/**
 * Aplica 50% de descuento todos los miércoles (día del espectador).
 * Si la función comienza un miércoles, el precio se reduce a la mitad.
 * No distingue por tipo de butaca ni formato — aplica a todo.
 */
public class ReglaDescuentoMiercoles implements ReglaPrecio {

    @Override
    public double aplicar(Butaca butaca, Funcion funcion, double currentPrice) {
        if (funcion.getStartTime().getDayOfWeek() == DayOfWeek.WEDNESDAY) {
            return currentPrice * 0.50;
        }
        return currentPrice;
    }
}
