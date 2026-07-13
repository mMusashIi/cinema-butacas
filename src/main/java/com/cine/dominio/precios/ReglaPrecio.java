package com.cine.dominio.precios;

import com.cine.dominio.Butaca;
import com.cine.dominio.Funcion;

/**
 * Interfaz funcional que representa una regla de precio o promoción.
 * Cada implementación recibe el precio acumulado hasta ese momento
 * y puede modificarlo (aplicando descuento, cargo extra, etc.).
 *
 * Al ser una interfaz funcional (@FunctionalInterface), las reglas simples
 * pueden expresarse como lambdas o method references, sin necesidad de
 * crear una clase completa para cada promoción pequeña.
 *
 * Principio de diseño (Abierto/Cerrado):
 * Para agregar una promoción nueva, se crea una clase nueva que implemente
 * esta interfaz — nunca se modifica MotorPrecios ni las reglas existentes.
 * Las reglas concretas viven en el subpaquete reglas/.
 *
 * Fórmula de precio de referencia (aplicada antes de las reglas):
 *   precio_base = funcion.getPrecioBase()
 *               × funcion.getFormat().getMultiplicadorPrecio()
 *               × butaca.getTipo().getMultiplicadorPrecio()
 *
 * Las reglas de este contrato reciben ese precio ya calculado como
 * 'currentPrice' y pueden ajustarlo a la baja (descuento) o al alza.
 *
 * Contrato que toda implementación debe respetar:
 *   - Nunca devolver un precio negativo — si el descuento supera el precio,
 *     retornar 0.0 como mínimo.
 *   - No mutar Butaca ni Funcion — esta interfaz es de solo consulta.
 */
@FunctionalInterface
public interface ReglaPrecio {

    /**
     * Aplica esta regla al precio actual de una butaca en una función.
     *
     * @param butaca         La butaca para la que se calcula el precio
     * @param funcion     La función en la que se encuentra esa butaca
     * @param currentPrice El precio acumulado hasta este punto en la cadena de reglas
     * @return             El precio modificado por esta regla (puede ser igual si no aplica)
     */
    double aplicar(Butaca butaca, Funcion funcion, double currentPrice);
}
