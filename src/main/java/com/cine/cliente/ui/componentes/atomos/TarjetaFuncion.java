package com.cine.cliente.ui.componentes.atomos;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class TarjetaFuncion extends VBox {
    public TarjetaFuncion(String titulo, String subtitulo, boolean agotado, boolean cerrado, Runnable onClick) {
        setPadding(new Insets(20));
        setSpacing(10);
        
        getStyleClass().addAll(Styles.ELEVATED_1);
        
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add(Styles.TITLE_3);
        lblTitulo.setWrapText(true);
        
        Label lblSub = new Label(subtitulo);
        lblSub.getStyleClass().add(Styles.TEXT_MUTED);
        lblSub.setWrapText(true);
        
        getChildren().addAll(lblTitulo, lblSub);
        
        if (cerrado) {
            setStyle("-fx-background-color: #2c2c2c; -fx-background-radius: 8; -fx-opacity: 0.7;");
            setCursor(Cursor.DEFAULT);
            Label lblCerrado = new Label("CERRADO");
            lblCerrado.setStyle("-fx-text-fill: #aaaaaa; -fx-font-weight: bold;");
            getChildren().add(lblCerrado);
        } else if (agotado) {
            setStyle("-fx-background-color: #3a1c1c; -fx-background-radius: 8; -fx-opacity: 0.6;");
            setCursor(Cursor.DEFAULT);
            Label lblAgotado = new Label("AGOTADO");
            lblAgotado.setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: bold;");
            getChildren().add(lblAgotado);
        } else {
            setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 8;");
            setCursor(Cursor.HAND);
            setOnMouseClicked(e -> onClick.run());
            setOnMouseEntered(e -> {
                getStyleClass().remove(Styles.ELEVATED_1);
                getStyleClass().add(Styles.ELEVATED_3);
                setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 8;");
            });
            setOnMouseExited(e -> {
                getStyleClass().remove(Styles.ELEVATED_3);
                getStyleClass().add(Styles.ELEVATED_1);
                setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 8;");
            });
        }
    }
}
