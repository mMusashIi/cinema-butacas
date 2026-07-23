package com.cine.cliente.ui.componentes.moleculas;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class IndicadorPantalla extends VBox {
    public IndicadorPantalla() {
        setAlignment(Pos.CENTER);
        setStyle("-fx-border-color: #333; -fx-border-width: 0 0 1 0; -fx-padding: 0 0 10 0;");
        
        Label lblPantalla = new Label("P A N T A L L A");
        lblPantalla.setStyle("-fx-text-fill: #555555; -fx-font-size: 12px; -fx-letter-spacing: 5px;");
        lblPantalla.setAlignment(Pos.CENTER);
        
        getChildren().add(lblPantalla);
    }
}
