package com.cine.cliente.ui.vistas;

import com.cine.cliente.ClienteRed;
import com.cine.cliente.ui.GestorVistas;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class PantallaInicio extends VBox {

    public PantallaInicio(ClienteRed cliente) {
        setSpacing(20);
        setAlignment(Pos.CENTER);

        Label lblTitulo = new Label("Sistema de Butacas");
        lblTitulo.setFont(new Font("Arial", 24));

        Button btnAdmin = new Button("Panel Administrador");
        btnAdmin.setPrefWidth(250);
        btnAdmin.setOnAction(e -> GestorVistas.navegarA(new PanelAdminVista(cliente, this)));

        Button btnCliente = new Button("Punto de Venta (Cliente)");
        btnCliente.setPrefWidth(250);
        btnCliente.setOnAction(e -> GestorVistas.navegarA(new PanelCliente(cliente, this)));

        getChildren().addAll(lblTitulo, btnAdmin, btnCliente);
    }
}
