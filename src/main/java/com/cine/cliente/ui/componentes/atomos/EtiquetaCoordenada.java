package com.cine.cliente.ui.componentes.atomos;

import javafx.geometry.Pos;
import javafx.scene.control.Label;

public class EtiquetaCoordenada extends Label {
    public EtiquetaCoordenada(String texto, boolean esFila) {
        super(texto);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setMinSize(10, 10); // Responsivo
        
        if (esFila) {
            setStyle("-fx-text-fill: #5c7cfa; -fx-font-weight: bold;");
            setAlignment(Pos.CENTER_RIGHT);
            setPrefWidth(30);
        } else {
            setStyle("-fx-text-fill: #777; -fx-font-weight: bold;");
            setAlignment(Pos.CENTER);
            setPrefWidth(40);
        }
    }
}
