package com.cine.cliente.ui.vistas;

import com.cine.cliente.ClienteRed;
import com.cine.cliente.ContextoGlobal;
import com.cine.cliente.ui.GestorVistas;
import com.cine.dominio.Cine;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;

public class PantallaSeleccionCine extends BorderPane {

    private final VBox listaCinesBox;
    private final ClienteRed cliente;

    public PantallaSeleccionCine() {
        setStyle("-fx-background-color: #121212;");
        cliente = new ClienteRed();

        Label lblTitulo = new Label("Seleccione su Cine");
        lblTitulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");

        listaCinesBox = new VBox(15);
        listaCinesBox.setAlignment(Pos.CENTER);

        ScrollPane scroll = new ScrollPane(listaCinesBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #1e1e1e; -fx-border-color: #333;");

        VBox centerContent = new VBox(20, lblTitulo, scroll);
        centerContent.setAlignment(Pos.CENTER);
        centerContent.setPadding(new Insets(40));
        setCenter(centerContent);

        // Formulario inferior para crear nuevo cine
        HBox bottomControls = new HBox(15);
        bottomControls.setAlignment(Pos.CENTER);
        bottomControls.setPadding(new Insets(20));
        bottomControls.setStyle("-fx-background-color: #1e1e1e; -fx-border-color: #333; -fx-border-width: 1 0 0 0;");

        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Nombre del Cine");
        TextField txtDireccion = new TextField();
        txtDireccion.setPromptText("Dirección");
        TextField txtCiudad = new TextField();
        txtCiudad.setPromptText("Ciudad");

        Button btnCrear = new Button("Crear Nuevo Cine");
        btnCrear.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-cursor: hand;");
        btnCrear.setOnAction(e -> crearCine(txtNombre.getText(), txtDireccion.getText(), txtCiudad.getText()));

        bottomControls.getChildren().addAll(new Label("O crear uno nuevo:"), txtNombre, txtDireccion, txtCiudad,
                btnCrear);
        setBottom(bottomControls);

        cargarCines();
    }

    private void cargarCines() {
        new Thread(() -> {
            try {
                cliente.connect((a, b) -> {
                }, t -> {
                }, () -> {
                }, reason -> {
                });
                String json = cliente.listarCines();
                Platform.runLater(() -> procesarCines(json));
            } catch (IOException e) {
                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.ERROR, "No se pudo conectar al servidor:\n" + e.getMessage());
                    a.show();
                });
            }
        }).start();
    }

    private void procesarCines(String json) {
        listaCinesBox.getChildren().clear();
        if (json.equals("[]")) {
            listaCinesBox.getChildren().add(new Label("No hay cines disponibles. Por favor, crea uno."));
            return;
        }

        // Parsear JSON manual básico [{"id":"...", "nombre":"...", ...}]
        String content = json.replace("[", "").replace("]", "").trim();
        if (content.isEmpty())
            return;

        String[] cineObjects = content.split("},\\{");
        for (String obj : cineObjects) {
            String cleanObj = obj.replace("{", "").replace("}", "").replace("\"", "");
            String[] props = cleanObj.split(",");

            String id = "";
            String nombre = "";
            String direccion = "";
            String ciudad = "";

            for (String p : props) {
                String[] kv = p.split(":");
                if (kv.length == 2) {
                    if (kv[0].trim().equals("id"))
                        id = kv[1].trim();
                    if (kv[0].trim().equals("nombre"))
                        nombre = kv[1].trim();
                    if (kv[0].trim().equals("direccion"))
                        direccion = kv[1].trim();
                    if (kv[0].trim().equals("ciudad"))
                        ciudad = kv[1].trim();
                }
            }

            Cine cine = new Cine(nombre, direccion, ciudad);
            // Necesitamos forzar el ID si lo necesitamos, pero en este caso solo
            // pasaremos la instancia. Para hacerlo 100% real, Cine debe tener un setId o
            // usar reflexión.
            // Para UI, solo lo guardamos en el botón.

            Button btnCine = new Button(nombre + " - " + ciudad + "\n" + direccion);
            btnCine.setStyle(
                    "-fx-font-size: 16px; -fx-padding: 15px; -fx-cursor: hand; -fx-background-color: #2b2b2b; -fx-text-fill: white;");
            btnCine.setMaxWidth(Double.MAX_VALUE);

            final Cine selectedCine = cine;
            btnCine.setOnAction(e -> {
                ContextoGlobal.setCineActual(selectedCine);
                cliente.disconnect();
                GestorVistas.navegarA(new com.cine.cliente.ui.vistas.PantallaInicio());
            });

            listaCinesBox.getChildren().add(btnCine);
        }
    }

    private void crearCine(String nombre, String direccion, String ciudad) {
        if (nombre.isEmpty() || direccion.isEmpty() || ciudad.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Todos los campos son obligatorios").show();
            return;
        }
        new Thread(() -> {
            try {
                boolean exito = cliente.crearCine(nombre, direccion, ciudad);
                Platform.runLater(() -> {
                    if (exito) {
                        new Alert(Alert.AlertType.INFORMATION, "Cine creado exitosamente").show();
                        cargarCines();
                    } else {
                        new Alert(Alert.AlertType.ERROR, "Error al crear cine").show();
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, e.getMessage()).show());
            }
        }).start();
    }
}
