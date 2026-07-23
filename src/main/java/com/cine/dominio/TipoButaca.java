package com.cine.dominio;

public record TipoButaca(String code, String displayName, double priceMultiplier, boolean accessible) {

    public static final TipoButaca NORMAL = new TipoButaca("NORMAL", "Butaca Normal", 1.0, false);
    public static final TipoButaca VIP = new TipoButaca("VIP", "Butaca VIP", 1.5, false);
    public static final TipoButaca SILLA_RUEDAS = new TipoButaca("SILLA_RUEDAS", "Silla de Ruedas", 1.0, true);
    public static final TipoButaca PAREJA = new TipoButaca("PAREJA", "Butaca Pareja", 2.0, false);
    public static final TipoButaca PASILLO = new TipoButaca("PASILLO", "Espacio Vacío", 1.0, false);
    public static final TipoButaca BROKEN = new TipoButaca("BROKEN", "Fuera de Servicio", 1.0, false);

    public TipoButaca {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("code no puede estar vacío ni ser nulo");
        }
        if (priceMultiplier <= 0) {
            throw new IllegalArgumentException("priceMultiplier debe ser un valor positivo");
        }
    }

    public static TipoButaca fromCode(String code) {
        return switch (code) {
            case "VIP" -> VIP;
            case "SILLA_RUEDAS" -> SILLA_RUEDAS;
            case "PAREJA" -> PAREJA;
            case "PASILLO" -> PASILLO;
            case "BROKEN" -> BROKEN;
            default -> NORMAL;
        };
    }
}
