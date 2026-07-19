package com.cine.cliente.ui.vistas;

import com.cine.cliente.ClienteRed;
import com.cine.cliente.ui.GestorVistas;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public class PanelAdminVista extends BorderPane {

    // Bug 4: sin estado compartido de red — cada operación crea
    // su propio ClienteRed para evitar condiciones de carrera en el socket.
    // Para almacenar IDs asociados a los nombres en los ComboBox
    private java.util.Map<String, String> peliculaMap = new java.util.HashMap<>();
    private java.util.Map<String, String> salaMap = new java.util.HashMap<>();

    public PanelAdminVista() {
        setStyle("-fx-background-color: #121212;");
        // Bug 4: ya no instanciamos un ClienteRed compartido aquí.

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

        Tab tabCompras = new Tab("Compras", crearPanelCompras());

        tabPane.getTabs().addAll(tabPeliculas, tabCines, tabSalas, tabFunciones, tabCompras);

        // Cargar datos cuando se seleccione la pestaña de funciones
        tabFunciones.setOnSelectionChanged(e -> {
            if (tabFunciones.isSelected()) {
                cargarDatosFunciones();
            }
        });

        // Cargar compras al seleccionar la pestaña
        tabCompras.setOnSelectionChanged(e -> {
            if (tabCompras.isSelected()) {
                VBox panel = (VBox) ((ScrollPane) tabCompras.getContent()).getContent();
                cargarCompras((TableView<CompraItem>) panel.getChildren().get(2)); // El índice 2 es la tabla (lbl, btn, table)
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
                    ClienteRed c = new ClienteRed();
                    try {
                        c.connect(null, null, null, null);
                        boolean exito = c.crearPelicula(tit, dur, clas);
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
                        c.disconnect();
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
                ClienteRed c = new ClienteRed();
                try {
                    c.connect(null, null, null, null);
                    boolean exito = c.crearCine(nom, dir, ciu);
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
                    c.disconnect();
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

        // Separar fecha y hora en campos distintos
        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setPromptText("Fecha");
        datePicker.setStyle("-fx-background-color: #1e1e2e; -fx-text-fill: white;");

        TextField txtHora = new TextField();
        txtHora.setPromptText("Hora (ej. 20:30)");

        HBox fechaHoraBox = new HBox(10, datePicker, txtHora);

        ComboBox<com.cine.dominio.FormatoFuncion> comboFormato = new ComboBox<>();
        comboFormato.getItems().addAll(com.cine.dominio.FormatoFuncion.values());
        comboFormato.setPromptText("Seleccione Formato");

        TextField txtPrecio = new TextField();
        txtPrecio.setPromptText("Precio Base (ej. 15.0)");

        Button btnGuardar = new Button("Programar Función");
        btnGuardar.setOnAction(e -> {
            String peliSel = comboPelicula.getValue();
            String salaSel = comboSala.getValue();
            com.cine.dominio.FormatoFuncion formatoSel = comboFormato.getValue();
            if (peliSel == null || salaSel == null || formatoSel == null) {
                mostrarAlerta(Alert.AlertType.WARNING, "Debe seleccionar una película, una sala y un formato");
                return;
            }
            String peliId = peliculaMap.get(peliSel);
            String salaId = salaMap.get(salaSel);

            try {
                LocalDate fecha = datePicker.getValue();
                LocalTime hora  = LocalTime.parse(txtHora.getText().trim());
                LocalDateTime fechaHora = LocalDateTime.of(fecha, hora);
                com.cine.dominio.FormatoFuncion formato = formatoSel;
                double precio = Double.parseDouble(txtPrecio.getText());

                new Thread(() -> {
                    ClienteRed c = new ClienteRed();
                    try {
                        c.connect(null, null, null, null);
                        boolean exito = c.crearFuncion(salaId, peliId, fechaHora, formato, precio);
                        Platform.runLater(() -> {
                            if (exito) {
                                mostrarAlerta(Alert.AlertType.INFORMATION, "Función creada exitosamente");
                                txtHora.clear();
                                comboFormato.setValue(null);
                                txtPrecio.clear();
                            } else {
                                mostrarAlerta(Alert.AlertType.ERROR, "Error al crear función");
                            }
                        });
                    } catch (Exception ex) {
                        Platform.runLater(
                                () -> mostrarAlerta(Alert.AlertType.ERROR, "Error de red: " + ex.getMessage()));
                    } finally {
                        c.disconnect();
                    }
                }).start();
            } catch (DateTimeParseException ex) {
                mostrarAlerta(Alert.AlertType.ERROR, "Hora inválida. Usa formato HH:mm (ej. 20:30)");
            } catch (Exception ex) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error en datos: " + ex.getMessage());
            }
        });

        Label lbl1 = new Label("Nueva Función:");
        lbl1.setStyle("-fx-text-fill: white;");
        panel.getChildren().addAll(lblTitulo, lbl1, comboPelicula, comboSala, fechaHoraBox, comboFormato, txtPrecio,
                btnGuardar);
        return panel;
    }

    public static class CompraItem {
        private final String ref;
        private final String dni;
        private final String pelicula;
        private final String sala;
        private final String cine;
        private final String horario;
        private final String butacas;
        private final String fecha;

        public CompraItem(String ref, String dni, String pelicula, String sala, String cine, String horario, String butacas, String fecha) {
            this.ref = ref; this.dni = dni; this.pelicula = pelicula; this.sala = sala;
            this.cine = cine; this.horario = horario; this.butacas = butacas; this.fecha = fecha;
        }

        public String getRef() { return ref; }
        public String getDni() { return dni; }
        public String getPelicula() { return pelicula; }
        public String getSala() { return sala; }
        public String getCine() { return cine; }
        public String getHorario() { return horario; }
        public String getButacas() { return butacas; }
        public String getFecha() { return fecha; }
    }

    private ScrollPane crearPanelCompras() {
        TableView<CompraItem> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: #1e1e1e; -fx-text-fill: white;");
        table.setPlaceholder(new Label("Sin compras registradas."));

        String[][] cols = {
            {"Referencia", "ref"}, {"DNI", "dni"}, {"Película", "pelicula"},
            {"Sala", "sala"}, {"Cine", "cine"}, {"Horario", "horario"},
            {"Butacas", "butacas"}, {"Fecha", "fecha"}
        };
        for (String[] col : cols) {
            TableColumn<CompraItem, String> tc = new TableColumn<>(col[0]);
            tc.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>(col[1]));
            tc.setStyle("-fx-text-fill: white;");
            table.getColumns().add(tc);
        }

        Button btnRefrescar = new Button("↻ Actualizar");
        btnRefrescar.setStyle("-fx-cursor: hand; -fx-background-color: #2563eb; -fx-text-fill: white;");
        btnRefrescar.setOnAction(e -> cargarCompras(table));

        Label lblTitulo = new Label("Historial de Compras");
        lblTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        VBox panel = new VBox(15, lblTitulo, btnRefrescar, table);
        panel.setPadding(new Insets(20));
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        panel.setStyle("-fx-background-color: #121212;");

        ScrollPane sp = new ScrollPane(panel);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        return sp;
    }

    private void cargarCompras(TableView<CompraItem> table) {
        new Thread(() -> {
            ClienteRed c = new ClienteRed();
            try {
                c.connect(null, null, null, null);
                String json = c.listarReservas();
                java.util.List<CompraItem> rows = parseReservasJson(json);
                Platform.runLater(() -> {
                    table.getItems().setAll(rows);
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                c.disconnect();
            }
        }).start();
    }

    /** Parsea el JSON de reservas devuelto por el servidor en una lista de objetos CompraItem. */
    private java.util.List<CompraItem> parseReservasJson(String json) {
        java.util.List<CompraItem> result = new java.util.ArrayList<>();
        if (json == null || json.equals("[]")) return result;
        String[] objs = json.replaceAll("^\\[|\\]$", "").split("\\},\\{");
        for (String o : objs) {
            o = o.replace("{", "").replace("}", "");
            result.add(new CompraItem(
                nvl(extractField(o, "ref")),
                nvl(extractField(o, "dni")),
                nvl(extractField(o, "pelicula")),
                nvl(extractField(o, "sala")),
                nvl(extractField(o, "cine")),
                nvl(extractField(o, "horario")),
                nvl(extractField(o, "butacas")),
                nvl(extractField(o, "fecha"))
            ));
        }
        return result;
    }

    private static String nvl(String s) { return s != null ? s : ""; }


    private void cargarDatosFunciones() {
        new Thread(() -> {
            ClienteRed c = new ClienteRed();
            try {
                c.connect(null, null, null, null);
                String jsonPelis = c.listarPeliculas();
                String jsonSalas = c.listarSalas();

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
                c.disconnect();
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
