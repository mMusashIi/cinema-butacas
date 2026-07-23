package com.cine.cliente.ui.componentes.atomos;

import atlantafx.base.theme.Styles;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.Cursor;

public class BotonButacaSvg extends Button {

    public static final String SVG_ARMCHAIR = 
        "M6,3 C4.89,3 4,3.89 4,5 V10 H3 C1.89,10 1,10.89 1,12 V17 C1,18.1 1.89,19 3,19 H4 V21 H6 V19 H18 V21 H20 V19 H21 C22.1,19 23,18.1 23,17 V12 C23,10.89 22.11,10 21,10 H20 V5 C20,3.89 19.1,3 18,3 H6 Z";

    public BotonButacaSvg(String estadoColor, String tooltipText) {
        setCursor(Cursor.HAND);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setPrefSize(40, 40); 
        setMinSize(10, 10); // Diseño responsivo para pantallas chicas
        
        getStyleClass().add(Styles.BUTTON_ICON);

        aplicarColorPorEstado(estadoColor);
        if (tooltipText != null && !tooltipText.isEmpty()) {
            setTooltip(new Tooltip(tooltipText));
        }
    }
    
    public void aplicarColorPorEstado(String estado) {
        String colorBase;
        double opacidad = 1.0;
        switch (estado) {
            case "LIBRE":
            case "NORMAL":
                colorBase = "#2ecc71"; // Verde
                break;
            case "OCUPADO":
                colorBase = "#2c2c2c"; // Oscuro
                break;
            case "BROKEN":
            case "FUERA_DE_SERVICIO":
                colorBase = "#e74c3c"; // Rojo
                break;
            case "SELECCIONADO":
                colorBase = "#3498db"; // Celeste
                break;
            case "LOCKED":
                colorBase = "#f1c40f"; // Amarillo
                break;
            case "PASILLO":
            case "NULO":
                colorBase = "#222222"; // Gris muy oscuro
                opacidad = 0.3;
                break;
            default:
                colorBase = "#7f8c8d";
        }

        setStyle("-fx-shape: \"" + SVG_ARMCHAIR + "\"; "
               + "-fx-scale-shape: true; "
               + "-fx-padding: 0; "
               + "-fx-background-color: " + colorBase + "; "
               + "-fx-opacity: " + opacidad + ";");
    }
}
