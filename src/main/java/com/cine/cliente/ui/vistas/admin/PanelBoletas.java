package com.cine.cliente.ui.vistas.admin;

import com.cine.cliente.ClienteRed;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.Callback;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PanelBoletas extends VBox {

    private final ClienteRed cliente;
    private TableView<BoletaViewModel> tabla;
    private ObservableList<BoletaViewModel> todasLasBoletas = FXCollections.observableArrayList();
    private ObservableList<BoletaViewModel> boletasFiltradas = FXCollections.observableArrayList();

    // Filtros
    private ComboBox<String> cmbPelicula;
    private ComboBox<String> cmbSala;
    private DatePicker dpFecha;
    private ComboBox<String> cmbHora;

    private static final String OPCION_TODAS = "Todas";

    public PanelBoletas(ClienteRed cliente) {
        this.cliente = cliente;
        setSpacing(15);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: transparent;");

        Label lblTitulo = new Label("Gestión de Boletas");
        lblTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        // --- Configuración de Filtros ---
        FlowPane filtrosPane = new FlowPane();
        filtrosPane.setHgap(15);
        filtrosPane.setVgap(10);
        filtrosPane.setAlignment(Pos.CENTER_LEFT);

        cmbPelicula = new ComboBox<>();
        cmbSala = new ComboBox<>();
        dpFecha = new DatePicker();
        cmbHora = new ComboBox<>();

        cmbPelicula.setPrefWidth(180);
        cmbSala.setPrefWidth(120);
        dpFecha.setPrefWidth(140);
        cmbHora.setPrefWidth(100);

        Button btnRefrescar = new Button("Refrescar Datos");
        btnRefrescar.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnRefrescar.setOnAction(e -> cargarTodasLasBoletas());

        Button btnLimpiar = new Button("Limpiar Filtros");
        btnLimpiar.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnLimpiar.setOnAction(e -> limpiarFiltros());
        
        filtrosPane.getChildren().addAll(
                crearFiltroControl("Película:", cmbPelicula),
                crearFiltroControl("Sala:", cmbSala),
                crearFiltroControl("Fecha:", dpFecha),
                crearFiltroControl("Hora:", cmbHora),
                btnRefrescar,
                btnLimpiar
        );

        // Listeners para filtros
        cmbPelicula.setOnAction(e -> actualizarFiltrosYTabla());
        cmbSala.setOnAction(e -> actualizarFiltrosYTabla());
        dpFecha.setOnAction(e -> actualizarFiltrosYTabla());
        cmbHora.setOnAction(e -> aplicarFiltroTabla()); // La hora no afecta a los otros combos, solo filtra la tabla

        crearTabla();

        getChildren().addAll(lblTitulo, filtrosPane, tabla);
        
        // Carga inicial
        cargarTodasLasBoletas();
    }

    private VBox crearFiltroControl(String labelText, Control control) {
        Label label = new Label(labelText);
        label.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        VBox box = new VBox(5, label, control);
        return box;
    }

    private void limpiarFiltros() {
        cmbPelicula.getSelectionModel().select(OPCION_TODAS);
        cmbSala.getSelectionModel().select(OPCION_TODAS);
        dpFecha.setValue(null);
        cmbHora.getSelectionModel().select(OPCION_TODAS);
        actualizarFiltrosYTabla();
    }


    @SuppressWarnings("unchecked")
    private void crearTabla() {
        tabla = new TableView<>();
        tabla.setItems(boletasFiltradas);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        TableColumn<BoletaViewModel, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().id != null ? data.getValue().id.substring(0, Math.min(8, data.getValue().id.length())) : ""));
        colId.setPrefWidth(80);
        colId.setMaxWidth(80);

        TableColumn<BoletaViewModel, String> colPeli = new TableColumn<>("Película");
        colPeli.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().pelicula));

        TableColumn<BoletaViewModel, String> colSala = new TableColumn<>("Sala");
        colSala.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().sala));

        TableColumn<BoletaViewModel, String> colFecha = new TableColumn<>("Fecha Reserva");
        colFecha.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().fechaReservada));

        TableColumn<BoletaViewModel, String> colHora = new TableColumn<>("Hora");
        colHora.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().horaFormateada));

        TableColumn<BoletaViewModel, String> colPago = new TableColumn<>("Pago");
        colPago.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().metodoPago));

        TableColumn<BoletaViewModel, String> colAsientos = new TableColumn<>("Asientos");
        colAsientos.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().asientosLimpios));

        TableColumn<BoletaViewModel, String> colDni = new TableColumn<>("DNI");
        colDni.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().dni));

        TableColumn<BoletaViewModel, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().estado));

        TableColumn<BoletaViewModel, Void> colAccion = new TableColumn<>("Acciones");
        colAccion.setPrefWidth(140);
        colAccion.setMaxWidth(140);
        colAccion.setCellFactory(param -> new TableCell<>() {
            private final Button btnVer = new Button("Ver");
            private final Button btnCancelar = new Button("Cancelar");
            private final HBox btnBox = new HBox(5, btnVer, btnCancelar);
            {
                btnVer.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-cursor: hand;");
                btnVer.setOnAction(event -> {
                    BoletaViewModel b = getTableView().getItems().get(getIndex());
                    if (b.jsonOriginal != null) {
                        com.cine.cliente.ui.componentes.moleculas.DialogBoleta.mostrar(b.jsonOriginal);
                    }
                });

                btnCancelar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");
                btnCancelar.setOnAction(event -> {
                    BoletaViewModel b = getTableView().getItems().get(getIndex());
                    if ("ACTIVA".equals(b.estado)) {
                        cancelarBoleta(b);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    BoletaViewModel b = getTableView().getItems().get(getIndex());
                    btnCancelar.setDisable(!"ACTIVA".equals(b.estado));
                    setGraphic(btnBox);
                }
            }
        });

        tabla.getColumns().addAll(colId, colPeli, colSala, colFecha, colHora, colPago, colAsientos, colDni, colEstado, colAccion);
    }

    private void cargarTodasLasBoletas() {
        new Thread(() -> {
            try {
                String jsonStr = cliente.listarTodasLasBoletas();
                Platform.runLater(() -> procesarBoletasJSON(jsonStr));
            } catch (Exception e) {
                Platform.runLater(() -> mostrarAlerta("Error al cargar boletas: " + e.getMessage()));
            }
        }).start();
    }

    private void procesarBoletasJSON(String jsonStr) {
        todasLasBoletas.clear();
        if (jsonStr == null || jsonStr.equals("[]")) {
            actualizarFiltrosYTabla();
            return;
        }

        String[] arr = jsonStr.replaceAll("^\\[|\\]$", "").split("\\},\\{");
        for (String s : arr) {
            s = s.replace("{", "").replace("}", "");
            BoletaViewModel b = new BoletaViewModel();
            b.id = extractField(s, "id");
            b.pelicula = nvl(extractField(s, "pelicula"), "N/A");
            b.sala = nvl(extractField(s, "sala"), "N/A");
            String rawHora = extractField(s, "hora");
            b.horaRaw = rawHora;
            if (rawHora != null && rawHora.contains("T")) {
                b.horaFormateada = rawHora.split("T")[1].substring(0, 5);
            } else {
                b.horaFormateada = rawHora != null ? rawHora : "";
            }
            b.fechaReservada = nvl(extractField(s, "fechaReservada"), "");
            b.metodoPago = nvl(extractField(s, "metodoPago"), "N/A");
            b.dni = nvl(extractField(s, "dni"), "N/A");
            
            String as = nvl(extractField(s, "asientos"), "");
            b.asientosRaw = as;
            b.asientosLimpios = java.util.Arrays.stream(as.split(","))
                    .filter(str -> !str.isBlank())
                    .map(str -> str.contains("_") ? str.substring(str.lastIndexOf('_') + 1) : str)
                    .collect(Collectors.joining("  "));
            
            b.estado = nvl(extractField(s, "estado"), "N/A");
            b.fechaEmision = extractField(s, "fechaEmision");
            b.jsonOriginal = "{" + s + "}";
            todasLasBoletas.add(b);
        }
        
        inicializarOpcionesDeFiltros();
    }

    private void inicializarOpcionesDeFiltros() {
        String selPeli = cmbPelicula.getValue();
        String selSala = cmbSala.getValue();
        
        Set<String> peliculas = new HashSet<>();
        Set<String> salas = new HashSet<>();
        Set<LocalDate> fechasConBoletas = new HashSet<>();

        for (BoletaViewModel b : todasLasBoletas) {
            peliculas.add(b.pelicula);
            salas.add(b.sala);
            if (b.fechaReservada != null && !b.fechaReservada.isEmpty()) {
                try {
                    fechasConBoletas.add(LocalDate.parse(b.fechaReservada));
                } catch (Exception ignored) {}
            }
        }

        cmbPelicula.getItems().setAll(OPCION_TODAS);
        cmbPelicula.getItems().addAll(peliculas.stream().sorted().collect(Collectors.toList()));
        cmbPelicula.getSelectionModel().select(selPeli != null && peliculas.contains(selPeli) ? selPeli : OPCION_TODAS);

        cmbSala.getItems().setAll(OPCION_TODAS);
        cmbSala.getItems().addAll(salas.stream().sorted().collect(Collectors.toList()));
        cmbSala.getSelectionModel().select(selSala != null && salas.contains(selSala) ? selSala : OPCION_TODAS);

        dpFecha.setDayCellFactory(param -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    if (fechasConBoletas.contains(item)) {
                        setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");
                        setTooltip(new Tooltip("Hay boletas registradas"));
                    } else {
                        setDisable(true);
                        setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #bdc3c7;");
                    }
                }
            }
        });

        actualizarFiltrosYTabla();
    }

    private void actualizarFiltrosYTabla() {
        String peli = cmbPelicula.getValue();
        String sala = cmbSala.getValue();
        LocalDate fecha = dpFecha.getValue();
        
        String selHora = cmbHora.getValue();

        Set<String> horasPosibles = new HashSet<>();
        for (BoletaViewModel b : todasLasBoletas) {
            boolean matchPeli = OPCION_TODAS.equals(peli) || b.pelicula.equals(peli);
            boolean matchSala = OPCION_TODAS.equals(sala) || b.sala.equals(sala);
            boolean matchFecha = (fecha == null) || (b.fechaReservada != null && b.fechaReservada.equals(fecha.toString()));
            
            if (matchPeli && matchSala && matchFecha) {
                horasPosibles.add(b.horaFormateada);
            }
        }

        cmbHora.setOnAction(null);
        cmbHora.getItems().setAll(OPCION_TODAS);
        cmbHora.getItems().addAll(horasPosibles.stream().sorted().collect(Collectors.toList()));
        cmbHora.getSelectionModel().select(selHora != null && horasPosibles.contains(selHora) ? selHora : OPCION_TODAS);
        cmbHora.setOnAction(e -> aplicarFiltroTabla());

        aplicarFiltroTabla();
    }

    private void aplicarFiltroTabla() {
        String peli = cmbPelicula.getValue();
        String sala = cmbSala.getValue();
        LocalDate fecha = dpFecha.getValue();
        String hora = cmbHora.getValue();

        List<BoletaViewModel> filtradas = todasLasBoletas.stream().filter(b -> {
            boolean matchPeli = OPCION_TODAS.equals(peli) || peli == null || b.pelicula.equals(peli);
            boolean matchSala = OPCION_TODAS.equals(sala) || sala == null || b.sala.equals(sala);
            boolean matchFecha = (fecha == null) || (b.fechaReservada != null && b.fechaReservada.equals(fecha.toString()));
            boolean matchHora = OPCION_TODAS.equals(hora) || hora == null || b.horaFormateada.equals(hora);
            return matchPeli && matchSala && matchFecha && matchHora;
        }).collect(Collectors.toList());

        boletasFiltradas.setAll(filtradas);
        tabla.refresh();
    }

    private void cancelarBoleta(BoletaViewModel b) {
        new Thread(() -> {
            try {
                boolean exito = cliente.cancelarBoleta(b.id);
                Platform.runLater(() -> {
                    if (exito) {
                        b.estado = "CANCELADA";
                        if (b.jsonOriginal != null) {
                            b.jsonOriginal = b.jsonOriginal.replace("\"estado\":\"ACTIVA\"", "\"estado\":\"CANCELADA\"");
                        }
                        tabla.refresh();
                    } else {
                        mostrarAlerta("No se pudo cancelar la boleta.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> mostrarAlerta("Error de red: " + e.getMessage()));
            }
        }).start();
    }

    private void mostrarAlerta(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg);
        alert.show();
    }

    private String extractField(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();

        if (start < json.length() && json.charAt(start) == '"') {
            start++; 
            int end = json.indexOf("\"", start);
            return end >= 0 ? json.substring(start, end) : json.substring(start);
        } else {
            int endComa = json.indexOf(",", start);
            int endLlave = json.indexOf("}", start);
            if (endComa == -1) endComa = Integer.MAX_VALUE;
            if (endLlave == -1) endLlave = Integer.MAX_VALUE;
            int end = Math.min(endComa, endLlave);
            return end != Integer.MAX_VALUE ? json.substring(start, end).trim() : json.substring(start).trim();
        }
    }

    private String nvl(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    public static class BoletaViewModel {
        public String id;
        public String pelicula;
        public String sala;
        public String horaRaw;
        public String horaFormateada;
        public String fechaReservada;
        public String metodoPago;
        public String dni;
        public String asientosRaw;
        public String asientosLimpios;
        public String estado;
        public String fechaEmision;
        public String jsonOriginal;
    }
}
