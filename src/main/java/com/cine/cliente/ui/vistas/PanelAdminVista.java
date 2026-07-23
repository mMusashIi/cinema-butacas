package com.cine.cliente.ui.vistas;

import com.cine.cliente.ClienteRed;
import com.cine.cliente.ui.GestorVistas;
import com.cine.cliente.ui.vistas.admin.PanelEditorSala;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.time.LocalTime;

public class PanelAdminVista extends BorderPane {

    private final ClienteRed cliente;
    private final PantallaInicio pantallaInicio;

    private ObservableList<PeliculaModel> peliculasList = FXCollections.observableArrayList();
    private ObservableList<SalaModel> salasList = FXCollections.observableArrayList();
    private ObservableList<FuncionModel> funcionesList = FXCollections.observableArrayList();

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

        Tab tabPeliculas = new Tab("Películas", crearPanelPeliculas());
        Tab tabSalasLista = new Tab("Salas (Lista)", crearPanelSalas());
        Tab tabSalasEditor = new Tab("Editor de Sala", new PanelEditorSala(cliente));
        Tab tabFunciones = new Tab("Funciones", crearPanelFunciones());
        Tab tabBoletas = new Tab("Boletas", new com.cine.cliente.ui.vistas.admin.PanelBoletas(cliente));
        
        tabPane.getTabs().addAll(tabPeliculas, tabSalasLista, tabSalasEditor, tabFunciones, tabBoletas);

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            cargarDatos();
        });

        setCenter(tabPane);
        cargarDatos();
    }

    private void cargarDatos() {
        new Thread(() -> {
            try {
                String jsonPelis = cliente.listarPeliculas();
                String jsonSalas = cliente.listarSalas();
                String jsonFunc = cliente.listarFunciones(LocalDate.now().toString()); // Hoy (como base, backend no filtra igual)

                Platform.runLater(() -> {
                    peliculasList.clear();
                    if (!jsonPelis.equals("[]") && jsonPelis.length() > 2) {
                        String[] arr = jsonPelis.replaceAll("^\\[|\\]$", "").split("\\},\\{");
                        for (String s : arr) {
                            s = s.replace("{", "").replace("}", "");
                            String id = extractField(s, "id");
                            String tit = extractField(s, "nombre");
                            int dur = Integer.parseInt(extractField(s, "duracion"));
                            boolean act = "true".equals(extractField(s, "activo"));
                            peliculasList.add(new PeliculaModel(id, tit, dur, act));
                        }
                    }

                    salasList.clear();
                    if (!jsonSalas.equals("[]") && jsonSalas.length() > 2) {
                        String[] arr = jsonSalas.replaceAll("^\\[|\\]$", "").split("\\},\\{");
                        for (String s : arr) {
                            s = s.replace("{", "").replace("}", "");
                            String id = extractField(s, "id");
                            String nom = extractField(s, "nombre");
                            int filas = Integer.parseInt(extractField(s, "filas"));
                            int cols = Integer.parseInt(extractField(s, "columnas"));
                            boolean act = "true".equals(extractField(s, "activo"));
                            salasList.add(new SalaModel(id, nom, filas, cols, act));
                        }
                    }

                    funcionesList.clear();
                    if (!jsonFunc.equals("[]") && jsonFunc.length() > 2) {
                        String[] arr = jsonFunc.replaceAll("^\\[|\\]$", "").split("\\},\\{");
                        for (String s : arr) {
                            s = s.replace("{", "").replace("}", "");
                            String id = extractField(s, "id");
                            String pel = extractField(s, "pelicula");
                            String sal = extractField(s, "sala");
                            String horStr = extractField(s, "hora");
                            boolean act = "true".equals(extractField(s, "activo"));
                            boolean eli = "true".equals(extractField(s, "eliminada"));
                            
                            if (eli) continue; // Skip soft deleted

                            try {
                                LocalTime horaDT = LocalTime.parse(horStr);
                                funcionesList.add(new FuncionModel(id, pel, sal, horaDT.toString(), act));
                            } catch (Exception x) {
                                // Fallback por si la db tiene datos viejos mal parseados en el cache del server
                            }
                        }
                    }
                    
                    actualizarCombosFuncion();
                });
            } catch (Exception e) {
                // Ignore load errors silently for smooth UI
            }
        }).start();
    }

    // --- Panel Peliculas ---
    private VBox crearPanelPeliculas() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));

        HBox form = new HBox(10);
        form.setAlignment(Pos.CENTER_LEFT);
        TextField txtTitulo = new TextField();
        txtTitulo.setPromptText("Título");
        TextField txtDuracion = new TextField();
        txtDuracion.setPromptText("Duración (min)");
        Button btnGuardar = new Button("Crear Película");
        btnGuardar.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        
        Label lblForm = new Label("Nueva Película:");
        lblForm.setStyle("-fx-text-fill: white;");
        form.getChildren().addAll(lblForm, txtTitulo, txtDuracion, btnGuardar);

        TableView<PeliculaModel> tabla = new TableView<>();
        tabla.setItems(peliculasList);
        
        TableColumn<PeliculaModel, String> colNom = new TableColumn<>("Nombre");
        colNom.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        TableColumn<PeliculaModel, Integer> colDur = new TableColumn<>("Duración (m)");
        colDur.setCellValueFactory(new PropertyValueFactory<>("duracion"));
        TableColumn<PeliculaModel, Boolean> colAct = new TableColumn<>("Estado");
        colAct.setCellValueFactory(new PropertyValueFactory<>("activo"));
        
        TableColumn<PeliculaModel, Void> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button();
            {
                btn.setOnAction(event -> {
                    PeliculaModel p = getTableView().getItems().get(getIndex());
                    togglePelicula(p);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    PeliculaModel p = getTableView().getItems().get(getIndex());
                    btn.setText(p.isActivo() ? "Desactivar" : "Activar");
                    btn.setStyle(p.isActivo() ? "-fx-background-color: #f39c12; -fx-text-fill: white;" : "-fx-background-color: #2ecc71; -fx-text-fill: white;");
                    setGraphic(btn);
                }
            }
        });

        tabla.getColumns().addAll(colNom, colDur, colAct, colAcciones);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        btnGuardar.setOnAction(e -> {
            try {
                int dur = Integer.parseInt(txtDuracion.getText());
                new Thread(() -> {
                    try {
                        if (cliente.crearPelicula(txtTitulo.getText(), dur)) {
                            Platform.runLater(() -> {
                                txtTitulo.clear(); txtDuracion.clear();
                                cargarDatos();
                            });
                        }
                    } catch (Exception ex) {}
                }).start();
            } catch (Exception ex) {}
        });

        panel.getChildren().addAll(form, tabla);
        return panel;
    }

    // --- Panel Salas ---
    private VBox crearPanelSalas() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        
        Label info = new Label("Las salas se crean y editan desde la pestaña 'Editor de Sala'.");
        info.setStyle("-fx-text-fill: #aaa; -fx-font-style: italic;");

        TableView<SalaModel> tabla = new TableView<>();
        tabla.setItems(salasList);
        
        TableColumn<SalaModel, String> colNom = new TableColumn<>("Nombre");
        colNom.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        TableColumn<SalaModel, Integer> colFilas = new TableColumn<>("Filas");
        colFilas.setCellValueFactory(new PropertyValueFactory<>("filas"));
        TableColumn<SalaModel, Integer> colCols = new TableColumn<>("Columnas");
        colCols.setCellValueFactory(new PropertyValueFactory<>("columnas"));
        TableColumn<SalaModel, Boolean> colAct = new TableColumn<>("Estado");
        colAct.setCellValueFactory(new PropertyValueFactory<>("activo"));
        
        TableColumn<SalaModel, Void> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button();
            {
                btn.setOnAction(event -> {
                    SalaModel s = getTableView().getItems().get(getIndex());
                    toggleSala(s);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    SalaModel s = getTableView().getItems().get(getIndex());
                    btn.setText(s.isActivo() ? "Desactivar" : "Activar");
                    btn.setStyle(s.isActivo() ? "-fx-background-color: #f39c12; -fx-text-fill: white;" : "-fx-background-color: #2ecc71; -fx-text-fill: white;");
                    setGraphic(btn);
                }
            }
        });

        tabla.getColumns().addAll(colNom, colFilas, colCols, colAct, colAcciones);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        panel.getChildren().addAll(info, tabla);
        return panel;
    }

    // --- Panel Funciones ---
    private ComboBox<PeliculaModel> cmbPelicula = new ComboBox<>();
    private ComboBox<SalaModel> cmbSala = new ComboBox<>();
    
    private void actualizarCombosFuncion() {
        cmbPelicula.setItems(peliculasList);
        cmbSala.setItems(salasList);
    }

    private VBox crearPanelFunciones() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));

        HBox form = new HBox(10);
        form.setAlignment(Pos.CENTER_LEFT);
        
        cmbPelicula.setPromptText("Película");
        cmbSala.setPromptText("Sala");
        TextField txtHora = new TextField();
        txtHora.setPromptText("Hora (HH:mm)");
        
        Button btnGuardar = new Button("Crear Función");
        btnGuardar.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");

        Label lbl1 = new Label("Película:"); lbl1.setStyle("-fx-text-fill: white;");
        Label lbl2 = new Label("Sala:"); lbl2.setStyle("-fx-text-fill: white;");
        Label lbl4 = new Label("Hora:"); lbl4.setStyle("-fx-text-fill: white;");

        form.getChildren().addAll(
            new VBox(5, lbl1, cmbPelicula),
            new VBox(5, lbl2, cmbSala),
            new VBox(5, lbl4, txtHora),
            new VBox(5, new Label(" "), btnGuardar)
        );

        TableView<FuncionModel> tabla = new TableView<>();
        tabla.setItems(funcionesList);
        
        TableColumn<FuncionModel, String> colPel = new TableColumn<>("Película");
        colPel.setCellValueFactory(new PropertyValueFactory<>("pelicula"));
        TableColumn<FuncionModel, String> colSal = new TableColumn<>("Sala");
        colSal.setCellValueFactory(new PropertyValueFactory<>("sala"));
        TableColumn<FuncionModel, String> colHor = new TableColumn<>("Hora");
        colHor.setCellValueFactory(new PropertyValueFactory<>("hora"));
        TableColumn<FuncionModel, Boolean> colAct = new TableColumn<>("Estado");
        colAct.setCellValueFactory(new PropertyValueFactory<>("activo"));
        
        TableColumn<FuncionModel, Void> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnAct = new Button();
            private final Button btnDel = new Button("Eliminar");
            private final HBox bx = new HBox(5, btnAct, btnDel);
            {
                btnAct.setOnAction(event -> {
                    FuncionModel f = getTableView().getItems().get(getIndex());
                    toggleFuncion(f);
                });
                btnDel.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                btnDel.setOnAction(event -> {
                    FuncionModel f = getTableView().getItems().get(getIndex());
                    eliminarFuncion(f);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    FuncionModel f = getTableView().getItems().get(getIndex());
                    btnAct.setText(f.isActivo() ? "Desactivar" : "Activar");
                    btnAct.setStyle(f.isActivo() ? "-fx-background-color: #f39c12; -fx-text-fill: white;" : "-fx-background-color: #2ecc71; -fx-text-fill: white;");
                    setGraphic(bx);
                }
            }
        });

        tabla.getColumns().addAll(colPel, colSal, colHor, colAct, colAcciones);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        btnGuardar.setOnAction(e -> {
            PeliculaModel p = cmbPelicula.getValue();
            SalaModel s = cmbSala.getValue();
            String hor = txtHora.getText();
            if (p == null || s == null || hor.isEmpty()) {
                mostrarAlerta(Alert.AlertType.WARNING, "Llene todos los campos.");
                return;
            }
            try {
                LocalTime ltime = LocalTime.parse(hor);
                new Thread(() -> {
                    try {
                        if (cliente.crearFuncion(s.getId(), p.getId(), ltime)) {
                            Platform.runLater(() -> {
                                txtHora.clear();
                                cargarDatos();
                            });
                        } else {
                            Platform.runLater(() -> mostrarAlerta(Alert.AlertType.ERROR, "Error o cruce de horarios."));
                        }
                    } catch (Exception ex) {}
                }).start();
            } catch (Exception ex) {
                mostrarAlerta(Alert.AlertType.ERROR, "Hora inválida (use HH:mm).");
            }
        });

        panel.getChildren().addAll(form, tabla);
        return panel;
    }

    // --- Acciones ---
    private void togglePelicula(PeliculaModel p) {
        new Thread(() -> {
            try {
                if (p.isActivo()) cliente.desactivarPelicula(p.getId());
                else cliente.activarPelicula(p.getId());
                Platform.runLater(this::cargarDatos);
            } catch(Exception e){}
        }).start();
    }
    private void toggleSala(SalaModel s) {
        new Thread(() -> {
            try {
                if (s.isActivo()) cliente.desactivarSala(s.getId());
                else cliente.activarSala(s.getId());
                Platform.runLater(this::cargarDatos);
            } catch(Exception e){}
        }).start();
    }
    private void toggleFuncion(FuncionModel f) {
        new Thread(() -> {
            try {
                if (f.isActivo()) cliente.desactivarFuncion(f.getId());
                else cliente.activarFuncion(f.getId());
                Platform.runLater(this::cargarDatos);
            } catch(Exception e){}
        }).start();
    }
    private void eliminarFuncion(FuncionModel f) {
        new Thread(() -> {
            try {
                if (cliente.eliminarFuncion(f.getId())) {
                    Platform.runLater(this::cargarDatos);
                }
            } catch(Exception e){}
        }).start();
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

    // --- Modelos Internos para TableView ---
    public static class PeliculaModel {
        private final StringProperty id;
        private final StringProperty nombre;
        private final IntegerProperty duracion;
        private final BooleanProperty activo;
        public PeliculaModel(String i, String n, int d, boolean a) {
            id = new SimpleStringProperty(i);
            nombre = new SimpleStringProperty(n);
            duracion = new SimpleIntegerProperty(d);
            activo = new SimpleBooleanProperty(a);
        }
        public String getId() { return id.get(); }
        public String getNombre() { return nombre.get(); }
        public int getDuracion() { return duracion.get(); }
        public boolean isActivo() { return activo.get(); }
        @Override public String toString() { return getNombre(); }
    }

    public static class SalaModel {
        private final StringProperty id;
        private final StringProperty nombre;
        private final IntegerProperty filas;
        private final IntegerProperty columnas;
        private final BooleanProperty activo;
        public SalaModel(String i, String n, int f, int c, boolean a) {
            id = new SimpleStringProperty(i);
            nombre = new SimpleStringProperty(n);
            filas = new SimpleIntegerProperty(f);
            columnas = new SimpleIntegerProperty(c);
            activo = new SimpleBooleanProperty(a);
        }
        public String getId() { return id.get(); }
        public String getNombre() { return nombre.get(); }
        public int getFilas() { return filas.get(); }
        public int getColumnas() { return columnas.get(); }
        public boolean isActivo() { return activo.get(); }
        @Override public String toString() { return getNombre(); }
    }

    public static class FuncionModel {
        private final StringProperty id;
        private final StringProperty pelicula;
        private final StringProperty sala;
        private final StringProperty hora;
        private final BooleanProperty activo;
        public FuncionModel(String i, String p, String s, String h, boolean a) {
            id = new SimpleStringProperty(i);
            pelicula = new SimpleStringProperty(p);
            sala = new SimpleStringProperty(s);
            hora = new SimpleStringProperty(h);
            activo = new SimpleBooleanProperty(a);
        }
        public String getId() { return id.get(); }
        public String getPelicula() { return pelicula.get(); }
        public String getSala() { return sala.get(); }
        public String getHora() { return hora.get(); }
        public boolean isActivo() { return activo.get(); }
    }
}
