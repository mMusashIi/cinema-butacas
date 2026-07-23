package com.cine.cliente.ui.componentes.moleculas;

import atlantafx.base.theme.Styles;
import com.cine.cliente.ui.componentes.atomos.BotonButacaSvg;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class ItemLeyenda extends HBox {
    public ItemLeyenda(String texto, String estadoColor) {
        setSpacing(5);
        setAlignment(Pos.CENTER);
        
        BotonButacaSvg icono = new BotonButacaSvg(estadoColor, null);
        // Miniatura
        icono.setPrefSize(20, 20);
        icono.setMinSize(15, 15);
        icono.setCursor(javafx.scene.Cursor.DEFAULT);
        
        Label lbl = new Label(texto);
        lbl.getStyleClass().add(Styles.TEXT_MUTED);
        
        getChildren().addAll(icono, lbl);
    }
}
