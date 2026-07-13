package com.cine.cliente.ui;

import atlantafx.base.theme.PrimerDark;
import com.cine.cliente.ui.vistas.PantallaInicio;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AplicacionCine extends Application {

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        com.cine.cliente.ui.vistas.PantallaSeleccionCine inicio = new com.cine.cliente.ui.vistas.PantallaSeleccionCine();
        Scene scene = new Scene(inicio, 1120, 720);
        
        GestorVistas.setMainScene(scene);

        stage.setTitle("Sistema de Gestión de Cines");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
