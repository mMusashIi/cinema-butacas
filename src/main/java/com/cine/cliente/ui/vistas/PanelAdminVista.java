package com.cine.cliente.ui.vistas;

import com.cine.cliente.ClienteRed;
import com.cine.cliente.ui.GestorVistas;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

//import java.io.IOException;
import java.time.LocalDateTime;

public class PanelAdminVista extends BorderPane {

    private ClienteRed cliente;

    // Para almacenar IDs asociados a los nombres en los ComboBox
    private java.util.Map<String, String> peliculaMap = new java.util.HashMap<>();
    private java.util.Map<String, String> salaMap = new java.util.HashMap<>();

    public PanelAdminVista() {
        setStyle("-fx-background-color: #121212;");
        cliente = new ClienteRed();

        // Cabecera superior
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 20, 15, 20));
        header.setStyle("-fx-background-color: #1e1e1e; -fx-border-color: #333; -fx-border-width: 0 0 1 0;");

        Label titulo = new Label("Panel de Administración");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        Button btnVolver = new Button("← Volver al Menú");
        btnVolver.setStyle("-fx-cursor: hand;");
        btnVolver.setOnAction(e -> GestorVistas.navegarA(new PantallaInicio()));

        header.getChildren().addAll(btnVolver, titulo);
        setTop(header);

        // TabPane central para los diferentes módulos
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tabPeliculas = new Tab("Películas", wrapInScrollPane(crearPanelPeliculas()));
        Tab tabCines = new Tab("Cines", wrapInScrollPane(crearPanelCines()));
        Tab tabSalas = new Tab("Salas", crearPanelSalas()); // PanelEditorSala ya tiene su propio ScrollPane

        VBox panelFunciones = crearPanelFunciones();
        Tab tabFunciones = new Tab("Funciones", wrapInScrollPane(panelFunciones));

        tabPane.getTabs().addAll(tabPeliculas, tabCines, tabSalas, tabFunciones);

        // Cargar datos cuando se seleccione la pestaña de funciones
        tabFunciones.setOnSelectionChanged(e -> {
            if (tabFunciones.isSelected()) {
                cargarDatosFunciones();
            }
        });

        setCenter(tabPane);
    }

    private ScrollPane wrapInScrollPane(javafx.scene.Node content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return scrollPane;
    }

    private VBox crearPanelPeliculas() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.setAlignment(Pos.TOP_LEFT);

        Label lblTitulo = new Label("Gestión de Películas");
        lblTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        TextField txtTitulo = new TextField();
        txtTitulo.setPromptText("Título de la película");

        TextField txtDuracion = new TextField();
        txtDuracion.setPromptText("Duración (minutos)");

        TextField txtClasificacion = new TextField();
        txtClasificacion.setPromptText("Clasificación (ej. PG-13)");

        Button btnGuardar = new Button("Crear Película");
        btnGuardar.setOnAction(e -> {
            String tit = txtTitulo.getText();
            String durStr = txtDuracion.getText();
            String clas = txtClasificacion.getText();
            if (tit.isEmpty() || durStr.isEmpty() || clas.isEmpty()) {
                mostrarAlerta(Alert.AlertType.WARNING, "Todos los campos son requeridos");
                return;
            }
            try {
                int dur = Integer.parseInt(durStr);
                new Thread(() -> {
                    try {
                        cliente.connect(null, null, null, null);
                        boolean exito = cliente.crearPelicula(tit, dur, clas);
                        Platform.runLater(() -> {
                            if (exito) {
                                mostrarAlerta(Alert.AlertType.INFORMATION, "Película creada exitosamente");
                                txtTitulo.clear();
                                txtDuracion.clear();
                                txtClasificacion.clear();
                            } else {
                                mostrarAlerta(Alert.AlertType.ERROR, "Error al crear película");
                            }
                        });
                    } catch (Exception ex) {
                        Platform.runLater(
                                () -> mostrarAlerta(Alert.AlertType.ERROR, "Error de red: " + ex.getMessage()));
                    } finally {
                        cliente.disconnect();
                    }
                }).start();
            } catch (NumberFormatException ex) {
                mostrarAlerta(Alert.AlertType.WARNING, "La duración debe ser un número entero");
            }
        });

        Label lbl1 = new Label("Añadir nueva película:");
        lbl1.setStyle("-fx-text-fill: white;");
        panel.getChildren().addAll(lblTitulo, lbl1, txtTitulo, txtDuracion, txtClasificacion, btnGuardar);
        return panel;
    }

    private VBox crearPanelCines() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));

        Label lblTitulo = new Label("Gestión de Cines");
        lblTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Nombre del Cine");

        TextField txtDireccion = new TextField();
        txtDireccion.setPromptText("Dirección");

        TextField txtCiudad = new TextField();
        txtCiudad.setPromptText("Ciudad");

        Button btnGuardar = new Button("Crear Cine");
        btnGuardar.setOnAction(e -> {
            String nom = txtNombre.getText();
            String dir = txtDireccion.getText();
            String ciu = txtCiudad.getText();
            if (nom.isEmpty() || dir.isEmpty() || ciu.isEmpty()) {
                mostrarAlerta(Alert.AlertType.WARNING, "Todos los campos son requeridos");
                return;
            }
            new Thread(() -> {
                try {
                    cliente.connect(null, null, null, null);
                    boolean exito = cliente.crearCine(nom, dir, ciu);
                    Platform.runLater(() -> {
                        if (exito) {
                            mostrarAlerta(Alert.AlertType.INFORMATION, "Cine creado exitosamente");
                            txtNombre.clear();
                            txtDireccion.clear();
                            txtCiudad.clear();
                        } else {
                            mostrarAlerta(Alert.AlertType.ERROR, "Error al crear cine");
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> mostrarAlerta(Alert.AlertType.ERROR, "Error de red: " + ex.getMessage()));
                } finally {
                    cliente.disconnect();
                }
            }).start();
        });

        Label lbl1 = new Label("Añadir nuevo cine:");
        lbl1.setStyle("-fx-text-fill: white;");
        panel.getChildren().addAll(lblTitulo, lbl1, txtNombre, txtDireccion, txtCiudad, btnGuardar);
        return panel;
    }

    private BorderPane crearPanelSalas() {
        return new com.cine.cliente.ui.vistas.admin.PanelEditorSala();
    }

    private ComboBox<String> comboPelicula;
    private ComboBox<String> comboSala;

    private VBox crearPanelFunciones() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));

        Label lblTitulo = new Label("Programación de Funciones");
        lblTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        comboPelicula = new ComboBox<>();
        comboPelicula.setPromptText("Seleccione Película");

        comboSala = new ComboBox<>();
        comboSala.setPromptText("Seleccione Sala");

        TextField txtFechaHora = new TextField();
        txtFechaHora.setPromptText("Fecha y Hora (ej. 2026-07-11T20:30)");

        TextField txtFormato = new TextField();
        txtFormato.setPromptText("Formato (ej. _2D, _3D, IMAX)");

        TextField txtPrecio = new TextField();
        txtPrecio.setPromptText("Precio Base (ej. 15.0)");

        Button btnGuardar = new Button("Programar Función");
        btnGuardar.setOnAction(e -> {
            String peliSel = comboPelicula.getValue();
            String salaSel = comboSala.getValue();
            if (peliSel == null || salaSel == null) {
                mostrarAlerta(Alert.AlertType.WARNING, "Debe seleccionar una película y una sala");
                return;
            }
            String peliId = peliculaMap.get(peliSel);
            String salaId = salaMap.get(salaSel);

            try {
                LocalDateTime hora = LocalDateTime.parse(txtFechaHora.getText());
                com.cine.dominio.FormatoFuncion formato = com.cine.dominio.FormatoFuncion.valueOf(txtFormato.getText());
                double precio = Double.parseDouble(txtPrecio.getText());

                new Thread(() -> {
                    try {
                        cliente.connect(null, null, null, null);
                        boolean exito = cliente.crearFuncion(salaId, peliId, hora, formato, precio);
                        Platform.runLater(() -> {
                            if (exito) {
                                mostrarAlerta(Alert.AlertType.INFORMATION, "Función creada exitosamente");
                                txtFechaHora.clear();
                                txtFormato.clear();
                                txtPrecio.clear();
                            } else {
                                mostrarAlerta(Alert.AlertType.ERROR, "Error al crear función");
                            }
                        });
                    } catch (Exception ex) {
                        Platform.runLater(
                                () -> mostrarAlerta(Alert.AlertType.ERROR, "Error de red: " + ex.getMessage()));
                    } finally {
                        cliente.disconnect();
                    }
                }).start();
            } catch (Exception ex) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error en formato de datos (Fecha, Formato o Precio)");
            }
        });

        Label lbl1 = new Label("Nueva Función:");
        lbl1.setStyle("-fx-text-fill: white;");
        panel.getChildren().addAll(lblTitulo, lbl1, comboPelicula, comboSala, txtFechaHora, txtFormato, txtPrecio,
                btnGuardar);
        return panel;
    }

    private void cargarDatosFunciones() {
        new Thread(() -> {
            try {
                cliente.connect(null, null, null, null);
                String jsonPelis = cliente.listarPeliculas();
                String jsonSalas = cliente.listarSalas();

                Platform.runLater(() -> {
                    peliculaMap.clear();
                    comboPelicula.getItems().clear();
                    if (!jsonPelis.equals("[]")) {
                        String[] arr = jsonPelis.replaceAll("^\\[|\\]$", "").split("\\},\\{");
                        for (String s : arr) {
                            s = s.replace("{", "").replace("}", "");
                            String id = extractField(s, "id");
                            String tit = extractField(s, "titulo");
                            peliculaMap.put(tit, id);
                            comboPelicula.getItems().add(tit);
                        }
                    }

                    salaMap.clear();
                    comboSala.getItems().clear();
                    if (!jsonSalas.equals("[]")) {
                        String[] arr = jsonSalas.replaceAll("^\\[|\\]$", "").split("\\},\\{");
                        for (String s : arr) {
                            s = s.replace("{", "").replace("}", "");
                            String id = extractField(s, "id");
                            String nom = extractField(s, "nombre");
                            String cine = extractField(s, "cineNombre");
                            String display = nom + " (" + cine + ")";
                            salaMap.put(display, id);
                            comboSala.getItems().add(display);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                cliente.disconnect();
            }
        }).start();
    }

    private void mostrarAlerta(Alert.AlertType tipo, String mensaje) {
        Alert a = new Alert(tipo, mensaje);
        a.show();
    }

    private String extractField(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1)
            return null;
        start += search.length();

        if (json.charAt(start) == '"') {
            start++; // saltar comilla
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } else {
            int endComa = json.indexOf(",", start);
            int endLlave = json.indexOf("}", start);
            if (endComa == -1)
                endComa = Integer.MAX_VALUE;
            if (endLlave == -1)
                endLlave = Integer.MAX_VALUE;
            int end = Math.min(endComa, endLlave);
            return json.substring(start, end);
        }
    }
}
