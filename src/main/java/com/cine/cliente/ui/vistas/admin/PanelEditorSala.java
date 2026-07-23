package com.cine.cliente.ui.vistas.admin;

import atlantafx.base.theme.Styles;
import com.cine.cliente.ClienteRed;
import com.cine.dominio.TipoButaca;
import com.cine.cliente.ui.componentes.organismos.CuadriculaSala;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class PanelEditorSala extends BorderPane {

    private int filasActuales = 0;
    private int columnasActuales = 0;
    private TipoButaca[][] matrizTipos;

    // Componentes nativos e importados
    private final CuadriculaSala cuadriculaSala = new CuadriculaSala();
    private final GridPane gridCelda = new GridPane();
    
    // Herramientas
    private ToggleGroup toolGroup;
    private ToggleGroup colorGroup;
    private ToggleButton btnModoVista;

    // Controles de configuración
    private TextField txtNombre;
    private TextField txtFilas;
    private TextField txtColumnas;
    private ComboBox<String> cmbSalas;
    private String currentSalaId = null;

    private ScrollPane scrollCentral;
    private final ClienteRed cliente;

    private java.util.Map<String, String> salaNameToId = new java.util.HashMap<>();

    public PanelEditorSala(ClienteRed cliente) {
        this.cliente = cliente;
        setPadding(new Insets(20));

        // CONTROLES SUPERIORES (A x B)
        FlowPane topControls = new FlowPane();
        topControls.setHgap(15);
        topControls.setVgap(10);
        topControls.setAlignment(Pos.CENTER_LEFT);
        topControls.setPadding(new Insets(0, 0, 15, 0));

        cmbSalas = new ComboBox<>();
        cmbSalas.setPromptText("Seleccione una sala");
        cmbSalas.getItems().add("--- CREAR NUEVA SALA ---");
        cmbSalas.getSelectionModel().selectFirst();
        cmbSalas.setOnAction(e -> manejarSeleccionSala());

        txtNombre = new TextField();
        txtNombre.setPromptText("Número (Ej: 1)");
        txtNombre.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getText().matches("[0-9]*")) return change;
            return null;
        }));

        txtFilas = new TextField();
        txtFilas.setPromptText("Filas (Ej: 10)");
        txtFilas.setPrefWidth(100);

        txtColumnas = new TextField();
        txtColumnas.setPromptText("Columnas (Ej: 15)");
        txtColumnas.setPrefWidth(100);

        Button btnGenerar = new Button("Generar Lienzo");
        btnGenerar.setOnAction(e -> generarLienzo(
            parseInt(txtFilas.getText(), 10),
            parseInt(txtColumnas.getText(), 10)
        ));

        topControls.getChildren().addAll(cmbSalas, new Label("Sala:"), txtNombre, new Label("Tamaño:"), txtFilas, txtColumnas, btnGenerar);
        setTop(topControls);
        
        cargarListaSalas();

        // CONTROLES LATERALES (PALETA)
        VBox palette = new VBox(15);
        palette.setPadding(new Insets(10));

        btnModoVista = new ToggleButton("Cambiar a Modo Vista");
        btnModoVista.setMaxWidth(Double.MAX_VALUE);
        btnModoVista.setOnAction(e -> {
            if (btnModoVista.isSelected()) {
                btnModoVista.setText("Cambiar a Modo Celda");
            } else {
                btnModoVista.setText("Cambiar a Modo Vista");
            }
            dibujarLienzo();
        });

        Label lblHerramientas = new Label("Herramienta:");
        lblHerramientas.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
        
        toolGroup = new ToggleGroup();
        RadioButton rbCelda = new RadioButton("Pintar Celda");
        rbCelda.setStyle("-fx-text-fill: white;");
        rbCelda.setToggleGroup(toolGroup);
        rbCelda.setSelected(true);
        RadioButton rbFila = new RadioButton("Pintar Fila");
        rbFila.setStyle("-fx-text-fill: white;");
        rbFila.setToggleGroup(toolGroup);
        RadioButton rbColumna = new RadioButton("Pintar Columna");
        rbColumna.setStyle("-fx-text-fill: white;");
        rbColumna.setToggleGroup(toolGroup);

        Label lblColores = new Label("Tipo de Espacio:");
        lblColores.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");

        colorGroup = new ToggleGroup();
        RadioButton rbNormal = new RadioButton("NORMAL (Verde)");
        rbNormal.setStyle("-fx-text-fill: white;");
        rbNormal.setUserData(TipoButaca.NORMAL);
        rbNormal.setToggleGroup(colorGroup);
        rbNormal.setSelected(true);

        RadioButton rbNulo = new RadioButton("NULO (Transparente)");
        rbNulo.setStyle("-fx-text-fill: white;");
        rbNulo.setUserData(TipoButaca.PASILLO);
        rbNulo.setToggleGroup(colorGroup);

        RadioButton rbBroken = new RadioButton("FUERA DE SERVICIO (Rojo)");
        rbBroken.setStyle("-fx-text-fill: white;");
        rbBroken.setUserData(TipoButaca.BROKEN);
        rbBroken.setToggleGroup(colorGroup);

        Button btnGuardar = new Button("Guardar Diseño");
        btnGuardar.setMaxWidth(Double.MAX_VALUE);
        btnGuardar.getStyleClass().add(Styles.SUCCESS);
        btnGuardar.setOnAction(e -> guardarDiseno());

        palette.getChildren().addAll(
            btnModoVista,
            new Separator(),
            lblHerramientas, rbCelda, rbFila, rbColumna,
            new Separator(),
            lblColores, rbNormal, rbNulo, rbBroken,
            new Separator(),
            btnGuardar
        );
        
        ScrollPane paletteScroll = new ScrollPane(palette);
        paletteScroll.setFitToWidth(true);
        paletteScroll.setPrefWidth(220);
        paletteScroll.setStyle("-fx-background: #1e1e1e; -fx-background-color: transparent; -fx-border-color: #333;");
        setLeft(paletteScroll);

        // ZONA CENTRAL
        gridCelda.setAlignment(Pos.CENTER);
        gridCelda.setHgap(4);
        gridCelda.setVgap(4);
        gridCelda.setPadding(new Insets(20));

        scrollCentral = new ScrollPane(gridCelda); // Por defecto inicia en Modo Celda
        scrollCentral.setFitToWidth(true);
        scrollCentral.setFitToHeight(true);
        scrollCentral.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: #333;");

        setCenter(scrollCentral);
        
        cuadriculaSala.setOnCellClick(this::manejarClicEnCelda);
    }

    private void cargarListaSalas() {
        new Thread(() -> {
            try {
                String jsonStr = cliente.listarSalas();
                Platform.runLater(() -> {
                    cmbSalas.getItems().clear();
                    cmbSalas.getItems().add("--- CREAR NUEVA SALA ---");
                    if (jsonStr != null && !jsonStr.trim().isEmpty() && !jsonStr.equals("[]")) {
                        String[] arr = jsonStr.replaceAll("^\\[|\\]$", "").split("\\},\\{");
                        for (String s : arr) {
                            String name = extractField(s, "nombre");
                            String id = extractField(s, "id");
                            if (name != null && id != null) {
                                salaNameToId.put(name, id);
                                cmbSalas.getItems().add(name);
                            }
                        }
                    }
                    cmbSalas.getSelectionModel().selectFirst();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void manejarSeleccionSala() {
        String sel = cmbSalas.getValue();
        if (sel == null || sel.equals("--- CREAR NUEVA SALA ---")) {
            currentSalaId = null;
            txtNombre.clear();
            txtFilas.clear();
            txtColumnas.clear();
            filasActuales = 0;
            columnasActuales = 0;
            matrizTipos = null;
            dibujarLienzo();
        } else {
            String nombre = sel.trim();
            String id = salaNameToId.get(nombre);
            currentSalaId = id;
            txtNombre.setText(nombre.replace("Sala ", "").trim());
            
            new Thread(() -> {
                try {
                    String configJson = cliente.obtenerSalaConfig(id);
                    Platform.runLater(() -> cargarSalaConfig(configJson));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
    
    private void cargarSalaConfig(String json) {
        if (json == null || json.isEmpty()) return;
        String filasStr = extractField(json, "filas");
        String colsStr = extractField(json, "columnas");
        String matrizStr = extractField(json, "matriz");
        
        if (filasStr == null || colsStr == null || matrizStr == null) return;
        
        int f = Integer.parseInt(filasStr);
        int c = Integer.parseInt(colsStr);
        txtFilas.setText(String.valueOf(f));
        txtColumnas.setText(String.valueOf(c));
        
        filasActuales = f;
        columnasActuales = c;
        matrizTipos = new TipoButaca[f][c];
        
        String[] celdas = matrizStr.split(",");
        int idx = 0;
        for (int i = 0; i < f; i++) {
            for (int j = 0; j < c; j++) {
                if (idx < celdas.length) {
                    matrizTipos[i][j] = TipoButaca.fromCode(celdas[idx]);
                    idx++;
                } else {
                    matrizTipos[i][j] = TipoButaca.NORMAL;
                }
            }
        }
        dibujarLienzo();
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

    private int parseInt(String str, int defaultVal) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private void generarLienzo(int f, int c) {
        if (f <= 0 || c <= 0) return;
        this.filasActuales = f;
        this.columnasActuales = c;
        this.matrizTipos = new TipoButaca[f][c];

        for (int i = 0; i < f; i++) {
            for (int j = 0; j < c; j++) {
                matrizTipos[i][j] = TipoButaca.NORMAL;
            }
        }
        dibujarLienzo();
    }

    private void dibujarLienzo() {
        if (btnModoVista.isSelected()) {
            scrollCentral.setContent(cuadriculaSala);
            dibujarModoVista();
        } else {
            scrollCentral.setContent(gridCelda);
            dibujarModoCelda();
        }
    }

    private void dibujarModoCelda() {
        gridCelda.getChildren().clear();
        for (int i = 0; i < filasActuales; i++) {
            for (int j = 0; j < columnasActuales; j++) {
                final int f = i;
                final int c = j;

                Rectangle rect = new Rectangle(30, 30);
                rect.setArcWidth(5);
                rect.setArcHeight(5);
                rect.setStroke(Color.web("#555"));
                rect.setFill(Color.web(obtenerColorPorTipo(matrizTipos[f][c])));

                StackPane cell = new StackPane(rect);
                cell.setCursor(javafx.scene.Cursor.HAND);
                
                cell.setOnMouseClicked(e -> manejarClicEnCelda(f, c));

                gridCelda.add(cell, c, f);
            }
        }
    }

    private void dibujarModoVista() {
        String[][] estadosMatriz = new String[filasActuales][columnasActuales];
        for (int i = 0; i < filasActuales; i++) {
            for (int j = 0; j < columnasActuales; j++) {
                estadosMatriz[i][j] = matrizTipos[i][j].code();
            }
        }
        cuadriculaSala.renderizarMatriz(estadosMatriz, null);
    }

    private String obtenerColorPorTipo(TipoButaca tipo) {
        if (tipo == TipoButaca.BROKEN) return "#e74c3c"; 
        if (tipo == TipoButaca.PASILLO) return "#121212"; 
        return "#2ecc71"; // NORMAL
    }

    private void manejarClicEnCelda(int fila, int columna) {
        Toggle tool = toolGroup.getSelectedToggle();
        Toggle colorToggle = colorGroup.getSelectedToggle();
        if (tool == null || colorToggle == null) return;

        TipoButaca tipoElegido = (TipoButaca) colorToggle.getUserData();
        String herramienta = ((RadioButton) tool).getText();

        if (herramienta.contains("Celda")) {
            matrizTipos[fila][columna] = tipoElegido;
        } else if (herramienta.contains("Fila")) {
            for (int c = 0; c < columnasActuales; c++) {
                matrizTipos[fila][c] = tipoElegido;
            }
        } else if (herramienta.contains("Columna")) {
            for (int f = 0; f < filasActuales; f++) {
                matrizTipos[f][columna] = tipoElegido;
            }
        }

        dibujarLienzo();
    }

    private void guardarDiseno() {
        String nombreSala = txtNombre.getText();
        if (filasActuales == 0 || columnasActuales == 0 || nombreSala == null || nombreSala.trim().isEmpty()) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Debes dar un nombre y generar la cuadrícula.");
            a.show();
            return;
        }

        StringBuilder csv = new StringBuilder();
        for (int i = 0; i < filasActuales; i++) {
            for (int j = 0; j < columnasActuales; j++) {
                csv.append(matrizTipos[i][j].code());
                if (i < filasActuales - 1 || j < columnasActuales - 1) {
                    csv.append(",");
                }
            }
        }
        
        new Thread(() -> {
            try {
                boolean exito = cliente.crearSala(currentSalaId, nombreSala, filasActuales, columnasActuales, csv.toString());
                Platform.runLater(() -> {
                    if (exito) {
                        System.out.println("Diseño de Sala '" + nombreSala + "' Guardado en el Servidor!");
                        Alert a = new Alert(Alert.AlertType.INFORMATION, "La Sala ha sido guardada exitosamente.");
                        a.show();
                        cargarListaSalas();
                    } else {
                        Alert a = new Alert(Alert.AlertType.ERROR, "Error al guardar la sala.");
                        a.show();
                    }
                });
            } catch (java.io.IOException ex) {
                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.ERROR, "No se pudo conectar al servidor:\n" + ex.getMessage());
                    a.show();
                });
            }
        }).start();
    }
}
