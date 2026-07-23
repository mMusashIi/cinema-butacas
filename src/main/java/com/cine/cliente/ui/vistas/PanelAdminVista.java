package com.cine.cliente.ui.vistas;

import com.cine.cliente.ClienteRed;
import com.cine.cliente.ui.GestorVistas;
import com.cine.cliente.ui.vistas.admin.PanelEditorSala;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class PanelAdminVista extends BorderPane {

    private final ClienteRed cliente;
    private final PantallaInicio pantallaInicio;

    private Map<String, String> peliculaMap = new HashMap<>();
    private Map<String, String> salaMap = new HashMap<>();

    public PanelAdminVista(ClienteRed cliente, PantallaInicio pantallaInicio) {
        this.cliente = cliente;
        this.pantallaInicio = pantallaInicio;

        setStyle("-fx-background-color: #121212;");

        // Cabecera superior
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 20, 15, 20));
        header.setStyle("-fx-background-color: #1e1e1e; -fx-border-color: #333; -fx-border-width: 0 0 1 0;");

        Label titulo = new Label("Panel de Administración");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        Button btnVolver = new Button("← Volver al Menú");
        btnVolver.setStyle("-fx-cursor: hand;");
        btnVolver.setOnAction(e -> GestorVistas.navegarA(pantallaInicio));

        header.getChildren().addAll(btnVolver, titulo);
        setTop(header);

        // TabPane central
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tabPeliculas = new Tab("Peliculas", wrapInScrollPane(crearPanelPeliculas()));
        Tab tabSalas = new Tab("Salas", new PanelEditorSala(cliente));
        
        VBox panelFunciones = crearPanelFunciones();
        Tab tabFunciones = new Tab("Funciones", wrapInScrollPane(panelFunciones));
        Tab tabBoletas = new Tab("Boletas", new com.cine.cliente.ui.vistas.admin.PanelBoletas(cliente));
        
        tabPane.getTabs().addAll(tabPeliculas, tabSalas, tabFunciones, tabBoletas);

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

        Button btnGuardar = new Button("Crear Película");
        btnGuardar.setOnAction(e -> {
            String tit = txtTitulo.getText();
            String durStr = txtDuracion.getText();
            if (tit.isEmpty() || durStr.isEmpty()) {
                mostrarAlerta(Alert.AlertType.WARNING, "Todos los campos son requeridos");
                return;
            }
            try {
                int dur = Integer.parseInt(durStr);
                new Thread(() -> {
                    try {
                        boolean exito = cliente.crearPelicula(tit, dur);
                        Platform.runLater(() -> {
                            if (exito) {
                                mostrarAlerta(Alert.AlertType.INFORMATION, "Película creada exitosamente");
                                txtTitulo.clear();
                                txtDuracion.clear();
                            } else {
                                mostrarAlerta(Alert.AlertType.ERROR, "Error al crear película");
                            }
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> mostrarAlerta(Alert.AlertType.ERROR, "Error de red: " + ex.getMessage()));
                    }
                }).start();
            } catch (NumberFormatException ex) {
                mostrarAlerta(Alert.AlertType.WARNING, "La duración debe ser un número entero");
            }
        });

        Label lbl1 = new Label("Añadir nueva película:");
        lbl1.setStyle("-fx-text-fill: white;");
        panel.getChildren().addAll(lblTitulo, lbl1, txtTitulo, txtDuracion, btnGuardar);
        return panel;
    }

    // UI de funciones
    private FlowPane flowPeliculas;
    private ToggleGroup tgPeliculas;
    
    private VBox seccionSalaYHora;
    private FlowPane flowSalas;
    private ToggleGroup tgSalas;
    private TextField txtHora;
    
    private VBox crearPanelFunciones() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));

        Label lblTitulo = new Label("Programación de Funciones");
        lblTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Sección 1: Elegir Película
        Label lblPelicula = new Label("1. Seleccione una Película:");
        lblPelicula.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        
        flowPeliculas = new FlowPane();
        flowPeliculas.setHgap(10);
        flowPeliculas.setVgap(10);
        tgPeliculas = new ToggleGroup();
        
        tgPeliculas.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                seccionSalaYHora.setVisible(true);
            }
        });

        // Sección 2: Sala, Hora y Guardar (Oculto inicialmente)
        seccionSalaYHora = new VBox(15);
        seccionSalaYHora.setVisible(false);
        
        Label lblSala = new Label("2. Seleccione una Sala:");
        lblSala.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        
        flowSalas = new FlowPane();
        flowSalas.setHgap(10);
        flowSalas.setVgap(10);
        tgSalas = new ToggleGroup();
        
        Label lblHora = new Label("3. Ingrese la hora:");
        lblHora.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        
        txtHora = new TextField();
        txtHora.setPromptText("Hora (ej. 20:30)");
        txtHora.setMaxWidth(150);

        Button btnGuardar = new Button("+ Agregar Función");
        btnGuardar.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
        btnGuardar.setOnAction(e -> guardarFuncion());

        seccionSalaYHora.getChildren().addAll(lblSala, flowSalas, lblHora, txtHora, btnGuardar);
        
        panel.getChildren().addAll(lblTitulo, lblPelicula, flowPeliculas, seccionSalaYHora);
        return panel;
    }

    private void cargarDatosFunciones() {
        new Thread(() -> {
            try {
                String jsonPelis = cliente.listarPeliculas();
                String jsonSalas = cliente.listarSalas();

                Platform.runLater(() -> {
                    peliculaMap.clear();
                    flowPeliculas.getChildren().clear();
                    if (!jsonPelis.equals("[]") && jsonPelis.length() > 2) {
                        String[] arr = jsonPelis.replaceAll("^\\[|\\]$", "").split("\\},\\{");
                        for (String s : arr) {
                            s = s.replace("{", "").replace("}", "");
                            String id = extractField(s, "id");
                            String tit = extractField(s, "nombre"); // Depende de cómo serialice el server, puede ser nombre o titulo
                            if (tit == null) tit = extractField(s, "titulo");
                            if (tit == null) tit = "Película " + id.substring(0, 4);

                            peliculaMap.put(tit, id);
                            ToggleButton tb = new ToggleButton(tit);
                            tb.setUserData(id);
                            tb.setToggleGroup(tgPeliculas);
                            flowPeliculas.getChildren().add(tb);
                        }
                    } else {
                        Label empty = new Label("No hay películas.");
                        empty.setStyle("-fx-text-fill: gray;");
                        flowPeliculas.getChildren().add(empty);
                    }

                    salaMap.clear();
                    flowSalas.getChildren().clear();
                    if (!jsonSalas.equals("[]") && jsonSalas.length() > 2) {
                        String[] arr = jsonSalas.replaceAll("^\\[|\\]$", "").split("\\},\\{");
                        for (String s : arr) {
                            s = s.replace("{", "").replace("}", "");
                            String id = extractField(s, "id");
                            String nom = extractField(s, "nombre");
                            
                            salaMap.put(nom, id);
                            ToggleButton tb = new ToggleButton(nom);
                            tb.setUserData(id);
                            tb.setToggleGroup(tgSalas);
                            flowSalas.getChildren().add(tb);
                        }
                    } else {
                        Label empty = new Label("No hay salas.");
                        empty.setStyle("-fx-text-fill: gray;");
                        flowSalas.getChildren().add(empty);
                    }
                    
                    // Resetear estado visual
                    seccionSalaYHora.setVisible(false);
                    if (tgPeliculas.getSelectedToggle() != null) tgPeliculas.getSelectedToggle().setSelected(false);
                    if (tgSalas.getSelectedToggle() != null) tgSalas.getSelectedToggle().setSelected(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> mostrarAlerta(Alert.AlertType.ERROR, "Error al cargar datos: " + e.getMessage()));
            }
        }).start();
    }
    
    private void guardarFuncion() {
        Toggle peliSel = tgPeliculas.getSelectedToggle();
        Toggle salaSel = tgSalas.getSelectedToggle();
        
        if (peliSel == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Debe seleccionar una película.");
            return;
        }
        if (salaSel == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Debe seleccionar una sala.");
            return;
        }
        
        String peliId = (String) peliSel.getUserData();
        String salaId = (String) salaSel.getUserData();
        
        try {
            LocalTime hora = LocalTime.parse(txtHora.getText().trim());
            // Usamos la fecha de hoy ya que solo es un horario diario
            LocalDateTime fechaHora = LocalDateTime.of(LocalDate.now(), hora);

            new Thread(() -> {
                try {
                    boolean exito = cliente.crearFuncion(salaId, peliId, fechaHora);
                    Platform.runLater(() -> {
                        if (exito) {
                            mostrarAlerta(Alert.AlertType.INFORMATION, "Función creada exitosamente");
                            txtHora.clear();
                            salaSel.setSelected(false);
                        } else {
                            mostrarAlerta(Alert.AlertType.ERROR, "Error al crear función");
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> mostrarAlerta(Alert.AlertType.ERROR, "Error de red: " + ex.getMessage()));
                }
            }).start();
        } catch (DateTimeParseException ex) {
            mostrarAlerta(Alert.AlertType.ERROR, "Hora inválida. Usa formato HH:mm (ej. 20:30)");
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String mensaje) {
        Alert a = new Alert(tipo, mensaje);
        a.show();
    }

    private String extractField(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();

        if (json.charAt(start) == '"') {
            start++; 
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } else {
            int endComa = json.indexOf(",", start);
            int endLlave = json.indexOf("}", start);
            if (endComa == -1) endComa = Integer.MAX_VALUE;
            if (endLlave == -1) endLlave = Integer.MAX_VALUE;
            int end = Math.min(endComa, endLlave);
            return json.substring(start, end);
        }
    }
}
