package com.cine.dominio;

// [PARADIGMA POO]: Uso de un Enum para encapsular fuertemente los estados válidos
public enum EstadoButaca {
    LIBRE,      // L
    RESERVADO,  // R
    OCUPADO;    // O

    // [PARADIGMA FUNCIONAL / EXPRESIONES LAMBDA / SWITCH EXPRESSION]: 
    // Evaluaciones directas para las reglas de negocio
    public boolean canTransitionTo(EstadoButaca target) {
        if (target == null) return false;
        
        return switch (this) {
            case LIBRE -> target == RESERVADO;
            case RESERVADO -> target == LIBRE || target == OCUPADO;
            case OCUPADO -> target == LIBRE; // Para cancelación
        };
    }
}
