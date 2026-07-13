package com.cine.cliente;

import com.cine.dominio.Cine;

public class ContextoGlobal {
    private static Cine cineActual;

    public static void setCineActual(Cine cine) {
        cineActual = cine;
    }

    public static Cine getCineActual() {
        return cineActual;
    }
}
