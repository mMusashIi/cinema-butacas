package com.cine.dominio.precios;

import com.cine.dominio.Butaca;
import com.cine.dominio.Funcion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Motor de cálculo de precios. Maneja dos capas de reglas:
 *
 * 1. PERMANENTES (ReglaPrecioNombrada): catálogo gestionado. Tienen nombre, id
 *    y ciclo de vida (activa/inactiva). Se aplican solo si están activas.
 *    Ejemplo: "Día del espectador", "Descuento accesibilidad".
 *
 * 2. TEMPORALES (ReglaPrecio): anónimas, sin identidad persistente.
 *    Se aplican siempre que estén en el motor. Útiles para descuentos
 *    de sesión única (ej. cupón ingresado en caja que aplica a la venta actual).
 *
 * Orden de aplicación:
 *   precio_base (formula) → reglas permanentes ACTIVAS → reglas temporales → piso 0
 *
 * Fórmula base:
 *   funcion.basePrice × format.priceMultiplier × seatType.priceMultiplier
 */
public class MotorPrecios {

    // Catálogo permanente: gestionado por el operador
    private final List<ReglaPrecioNombrada> permanentRules;

    // Reglas temporales: solo para la sesión actual
    private final List<ReglaPrecio> temporaryRules;

    public MotorPrecios() {
        this.permanentRules = new ArrayList<>();
        this.temporaryRules = new ArrayList<>();
    }

    // =====================================================================
    // GESTIÓN DE REGLAS PERMANENTES
    // =====================================================================

    /**
     * Agrega una regla permanente al catálogo.
     */
    public MotorPrecios addPermanentRule(ReglaPrecioNombrada rule) {
        if (rule == null) throw new IllegalArgumentException("La regla no puede ser nula");
        permanentRules.add(rule);
        return this;
    }

    /**
     * Elimina una regla permanente del catálogo.
     * Preferir desactivarla (rule.deactivate()) para preservar historial.
     */
    public MotorPrecios removePermanentRule(ReglaPrecioNombrada rule) {
        permanentRules.remove(rule);
        return this;
    }

    /**
     * Busca una regla permanente por su id.
     */
    public ReglaPrecioNombrada findPermanentRuleById(String id) {
        return permanentRules.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Devuelve todas las reglas permanentes (activas e inactivas) — para la UI de gestión.
     */
    public List<ReglaPrecioNombrada> getAllPermanentRules() {
        return Collections.unmodifiableList(permanentRules);
    }

    /**
     * Devuelve solo las reglas permanentes activas — para previsualización de precio.
     */
    public List<ReglaPrecioNombrada> getActivePermanentRules() {
        return permanentRules.stream()
                .filter(ReglaPrecioNombrada::isActive)
                .toList();
    }

    // =====================================================================
    // GESTIÓN DE REGLAS TEMPORALES (sesión actual)
    // =====================================================================

    /**
     * Agrega una regla temporal para la sesión actual.
     * No tiene identidad ni se guarda entre sesiones.
     */
    public MotorPrecios addTemporaryRule(ReglaPrecio rule) {
        if (rule == null) throw new IllegalArgumentException("La regla no puede ser nula");
        temporaryRules.add(rule);
        return this;
    }

    /**
     * Elimina una regla temporal (ej. cupón que ya fue validado y aplicado).
     */
    public MotorPrecios removeTemporaryRule(ReglaPrecio rule) {
        temporaryRules.remove(rule);
        return this;
    }

    /**
     * Limpia todas las reglas temporales (al cerrar una sesión de venta).
     */
    public MotorPrecios clearTemporaryRules() {
        temporaryRules.clear();
        return this;
    }

    // =====================================================================
    // CÁLCULO DE PRECIO
    // =====================================================================

    /**
     * Calcula el precio final de una butaca en una función.
     *
     * Orden:
     *   1. Precio base con multiplicadores de formato y tipo de butaca
     *   2. Reglas permanentes ACTIVAS (en orden de registro)
     *   3. Reglas temporales (en orden de registro)
     *   4. Piso en 0.0
     */
    public double calcularPrecio(Butaca butaca, Funcion funcion) {
        if (butaca == null) throw new IllegalArgumentException("La butaca no puede ser nula");
        if (funcion == null) throw new IllegalArgumentException("La función no puede ser nula");

        // Paso 1: fórmula base
        double price = funcion.getPrecioBase()
                     * funcion.getFormat().getMultiplicadorPrecio()
                     * butaca.getTipo().priceMultiplier();

        // Paso 2: reglas permanentes activas
        for (ReglaPrecioNombrada rule : permanentRules) {
            price = rule.aplicar(butaca, funcion, price); // ReglaPrecioNombrada ya filtra si está inactiva
            price = Math.max(0.0, price);
        }

        // Paso 3: reglas temporales
        for (ReglaPrecio rule : temporaryRules) {
            price = rule.aplicar(butaca, funcion, price);
            price = Math.max(0.0, price);
        }

        return Math.max(0.0, price);
    }

    /**
     * Calcula el precio total de una lista de butacas en la misma función.
     */
    public double calculateTotal(List<Butaca> butacas, Funcion funcion) {
        if (butacas == null || butacas.isEmpty()) return 0.0;
        return butacas.stream()
                .mapToDouble(butaca -> calcularPrecio(butaca, funcion))
                .sum();
    }
}
