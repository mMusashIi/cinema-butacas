package com.cine.cliente.ui.vistas;

import com.cine.cliente.ui.GestorVistas;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class PantallaInicio extends VBox {

    public PantallaInicio() {
        setAlignment(Pos.CENTER);
        setSpacing(30);
        setStyle("-fx-background-color: #121212;");

        Label titulo = new Label("Sistema de Gestión de Cines");
        titulo.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white;");

        Button btnPuntoVenta = new Button("Punto de Venta");
        btnPuntoVenta.setStyle("-fx-font-size: 18px; -fx-padding: 15px 30px; -fx-cursor: hand;");
        btnPuntoVenta.setOnAction(e -> GestorVistas.navegarA(new SeleccionFuncionVista()));

        Button btnAdmin = new Button("Administración");
        btnAdmin.setStyle("-fx-font-size: 18px; -fx-padding: 15px 30px; -fx-cursor: hand;");
        btnAdmin.setOnAction(e -> {
            GestorVistas.navegarA(new PanelAdminVista());
        });

        getChildren().addAll(titulo, btnPuntoVenta, btnAdmin);
    }
}
