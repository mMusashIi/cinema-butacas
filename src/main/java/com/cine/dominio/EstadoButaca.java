package com.cine.dominio;

public enum EstadoButaca {
    FREE,
    SELECTED,
    BOOKED,
    BROKEN;

    public boolean isAvailable() {
        return this == FREE;
    }

    public boolean canTransitionTo(EstadoButaca target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case FREE -> target == SELECTED || target == BROKEN;
            case SELECTED -> target == FREE || target == BOOKED;
            case BOOKED -> target == FREE;
            case BROKEN -> target == FREE;
        };
    }
}
