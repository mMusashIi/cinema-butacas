package com.cine.cliente.ui;

import atlantafx.base.theme.PrimerDark;
import com.cine.cliente.ClienteRed;
import com.cine.cliente.ui.vistas.PantallaInicio;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class AplicacionCine extends Application {

    private ClienteRed cliente;

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        cliente = new ClienteRed();
        try {
            cliente.connect(
                (id, status) -> {},
                error -> Platform.runLater(() -> mostrarAlerta("Error de Red", error))
            );
        } catch (Exception e) {
            mostrarAlerta("Error de Conexión", "Asegúrate de que el servidor esté corriendo en el puerto 9090.\n" + e.getMessage());
            return;
        }

        PantallaInicio inicio = new PantallaInicio(cliente);
        Scene scene = new Scene(inicio, 1120, 720);
        
        GestorVistas.setMainScene(scene);

        stage.setTitle("Sistema de Gestión de Cines (Simplificado)");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            if (cliente != null) cliente.disconnect();
            Platform.exit();
            System.exit(0);
        });
        stage.show();
    }

    private void mostrarAlerta(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
