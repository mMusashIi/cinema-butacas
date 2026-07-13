package com.cine.dominio;

/**
 * Representa el formato de proyección de una función específica.
 * Es un atributo de Funcion, no de la sala ni de la película.
 *
 * Nota de diseño: el multiplicador de precio definido aquí (getMultiplicadorPrecio)
 * es independiente del multiplicador del tipo de butaca (TipoButaca). 
 * Ambos se combinarán en el MotorPrecios multiplicándolos por el precio base de la función.
 */
public enum FormatoFuncion {
    STANDARD(1.0, "Función Estándar"),
    _3D(1.3, "Función 3D"),   // Java no permite identificadores que comiencen con números (3D)
    VIP(1.5, "Función VIP");

    private final double priceMultiplier;
    private final String displayName;

    FormatoFuncion(double priceMultiplier, String displayName) {
        this.priceMultiplier = priceMultiplier;
        this.displayName = displayName;
    }

    public double getMultiplicadorPrecio() {
        return priceMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }
}
