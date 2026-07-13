package com.cine.cliente.ui.vistas.admin;

import com.cine.dominio.TipoButaca;
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

    private GridPane gridSala;
    
    // Herramientas
    private ToggleGroup toolGroup;
    private ToggleGroup colorGroup;

    // Controles de configuración
    private ComboBox<String> comboCine;
    private TextField txtNombre;
    private TextField txtFilas;
    private TextField txtColumnas;

    public PanelEditorSala() {
        setPadding(new Insets(20));

        // CONTROLES SUPERIORES (A x B)
        FlowPane topControls = new FlowPane();
        topControls.setHgap(15);
        topControls.setVgap(10);
        topControls.setAlignment(Pos.CENTER_LEFT);
        topControls.setPadding(new Insets(0, 0, 15, 0));

        comboCine = new ComboBox<>();
        comboCine.setPromptText("Cargando Cines...");
        
        txtNombre = new TextField();
        txtNombre.setPromptText("Nombre Sala");

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

        topControls.getChildren().addAll(comboCine, new Label("Sala:"), txtNombre, new Label("Tamaño:"), txtFilas, txtColumnas, btnGenerar);
        setTop(topControls);

        // Map para IDs de cines
        java.util.Map<String, String> cineMap = new java.util.HashMap<>();
        
        // Cargar cines
        new Thread(() -> {
            com.cine.cliente.ClienteRed c = new com.cine.cliente.ClienteRed();
            try {
                c.connect(null, null, null, null);
                String json = c.listarCines();
                javafx.application.Platform.runLater(() -> {
                    comboCine.setPromptText("Seleccione Cine");
                    if (!json.equals("[]")) {
                        String[] cines = json.replaceAll("^\\[|\\]$", "").split("\\},\\{");
                        for (String str : cines) {
                            str = str.replace("{", "").replace("}", "");
                            String id = extractField(str, "id");
                            String nom = extractField(str, "nombre");
                            cineMap.put(nom, id);
                            comboCine.getItems().add(nom);
                        }
                    }
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> comboCine.setPromptText("Error al cargar"));
            } finally {
                c.disconnect();
            }
        }).start();

        // CONTROLES LATERALES (PALETA)
        VBox palette = new VBox(15);
        palette.setPadding(new Insets(10));

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

        RadioButton rbVip = new RadioButton("VIP (Morado)");
        rbVip.setStyle("-fx-text-fill: white;");
        rbVip.setUserData(TipoButaca.VIP);
        rbVip.setToggleGroup(colorGroup);

        RadioButton rbSilla = new RadioButton("SILLA (Azul)");
        rbSilla.setStyle("-fx-text-fill: white;");
        rbSilla.setUserData(TipoButaca.SILLA_RUEDAS);
        rbSilla.setToggleGroup(colorGroup);

        RadioButton rbNulo = new RadioButton("NULO (Transparente)");
        rbNulo.setStyle("-fx-text-fill: white;");
        rbNulo.setUserData(TipoButaca.PASILLO);
        rbNulo.setToggleGroup(colorGroup);

        Button btnVistaPrevia = new Button("Alternar Vista Previa");
        btnVistaPrevia.setOnAction(e -> alternarVistaPrevia());

        Button btnGuardar = new Button("Guardar Diseño en Servidor");
        btnGuardar.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
        btnGuardar.setOnAction(e -> guardarDiseno());

        palette.getChildren().addAll(
            lblHerramientas, rbCelda, rbFila, rbColumna,
            new Separator(),
            lblColores, rbNormal, rbVip, rbSilla, rbNulo,
            new Separator(),
            btnVistaPrevia,
            btnGuardar
        );
        
        ScrollPane paletteScroll = new ScrollPane(palette);
        paletteScroll.setFitToWidth(true);
        paletteScroll.setPrefWidth(200);
        paletteScroll.setStyle("-fx-background: #1e1e1e; -fx-background-color: transparent; -fx-border-color: #333;");
        setLeft(paletteScroll);

        // ZONA CENTRAL (GRID)
        gridSala = new GridPane();
        gridSala.setAlignment(Pos.CENTER);
        gridSala.setHgap(4);
        gridSala.setVgap(4);
        gridSala.setPadding(new Insets(20));

        scrollCentral = new ScrollPane(gridSala);
        scrollCentral.setFitToWidth(true);
        scrollCentral.setFitToHeight(true);
        scrollCentral.setStyle("-fx-background: #1e1e1e; -fx-border-color: #333;");

        setCenter(scrollCentral);
    }

    private ScrollPane scrollCentral;
    private boolean enVistaPrevia = false;

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
        gridSala.getChildren().clear();

        for (int i = 0; i < filasActuales; i++) {
            for (int j = 0; j < columnasActuales; j++) {
                final int f = i;
                final int c = j;

                Rectangle rect = new Rectangle(30, 30);
                rect.setArcWidth(5);
                rect.setArcHeight(5);
                rect.setStroke(Color.web("#555"));
                rect.setFill(obtenerColorPorTipo(matrizTipos[f][c]));

                StackPane cell = new StackPane(rect);
                cell.setCursor(javafx.scene.Cursor.HAND);
                
                cell.setOnMouseClicked(e -> manejarClicEnCelda(f, c));

                gridSala.add(cell, c, f);
            }
        }
    }

    private Color obtenerColorPorTipo(TipoButaca tipo) {
        if (tipo == TipoButaca.VIP) return Color.web("#9b59b6"); // Morado
        if (tipo == TipoButaca.SILLA_RUEDAS) return Color.web("#3498db"); // Azul
        if (tipo == TipoButaca.PASILLO) return Color.web("#121212"); // Fondo
        return Color.web("#2ecc71"); // Normal (Verde)
    }

    private void alternarVistaPrevia() {
        if (filasActuales == 0 || columnasActuales == 0) return;
        enVistaPrevia = !enVistaPrevia;
        
        if (enVistaPrevia) {
            com.cine.dominio.Cine dummyCine = new com.cine.dominio.Cine("Dummy", "Dir", "Ciudad");
            com.cine.dominio.SalaCine tempSala = new com.cine.dominio.SalaCine("Preview", dummyCine, filasActuales, columnasActuales);
            for (int i = 0; i < filasActuales; i++) {
                for (int j = 0; j < columnasActuales; j++) {
                    if (matrizTipos[i][j] == com.cine.dominio.TipoButaca.PASILLO) {
                        tempSala.clearCell(i, j);
                    } else {
                        com.cine.dominio.Butaca b = new com.cine.dominio.Butaca(String.valueOf((char)('A' + i)), j + 1, matrizTipos[i][j]);
                        tempSala.replaceSeat(i, j, b);
                    }
                }
            }
            com.cine.cliente.ui.componentes.organismos.PanelCuadriculaButacas preview = new com.cine.cliente.ui.componentes.organismos.PanelCuadriculaButacas(tempSala, id -> {});
            scrollCentral.setContent(preview);
        } else {
            scrollCentral.setContent(gridSala);
            dibujarLienzo();
        }
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
        
        String selCine = comboCine.getValue();
        if (selCine == null) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Debes seleccionar un Cine.");
            a.show();
            return;
        }

        // We stored the map as a local variable before, but we can't access it easily here unless we make it a class field.
        // Let's just look it up if we have to, or we can use the Cine name as the ID fallback on the server if it matches.
        // Actually, the server matches by ID. We need the ID.
        // I will change the logic below to just pass the selCine directly if we don't have the ID, wait, I can extract the ID by reading the Map. But since it's a local variable in the constructor, I'll just change the map to be a class field.
        // For simplicity right now, I'll just send the name and let the server find it by name if ID doesn't match? No, server does .equals(cineId).
        // Let's just use ContextoGlobal fallback for now or we just need to add the field. 
        // Wait, I can't redefine the class structure easily in one block without replacing the whole class.
        // Since I'm injecting this block, I will use a hack: store the ID in the ComboBox user data or find it again.
        // Since I can't change the constructor easily, I'll just fetch the cines again synchronously or trust the server to match by Name.
        // Wait, I will just add extractField.
        
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
            com.cine.cliente.ClienteRed cliente = new com.cine.cliente.ClienteRed();
            try {
                cliente.connect((a,b)->{}, t->{}, ()->{}, reason->{});
                
                // Fetch cines to find the ID of the selected cine (inefficient but safe since we didn't make the map global)
                String jsonCines = cliente.listarCines();
                String cineId = "default-cine";
                if (!jsonCines.equals("[]")) {
                    String[] cines = jsonCines.replaceAll("^\\[|\\]$", "").split("\\},\\{");
                    for (String str : cines) {
                        str = str.replace("{", "").replace("}", "");
                        if (selCine.equals(extractField(str, "nombre"))) {
                            cineId = extractField(str, "id");
                            break;
                        }
                    }
                }

                String payloadSala = cineId + com.cine.compartido.Protocolo.SEP + nombreSala + com.cine.compartido.Protocolo.SEP + filasActuales + com.cine.compartido.Protocolo.SEP + columnasActuales + com.cine.compartido.Protocolo.SEP + csv.toString();
                String resp = cliente.sendAndWait(com.cine.compartido.Protocolo.CREAR_SALA + com.cine.compartido.Protocolo.SEP + payloadSala);
                boolean exito = resp.startsWith(com.cine.compartido.Protocolo.OK);
                
                javafx.application.Platform.runLater(() -> {
                    if (exito) {
                        System.out.println("Diseño de Sala '" + nombreSala + "' Guardado en el Servidor!");
                        Alert a = new Alert(Alert.AlertType.INFORMATION, "La Sala ha sido guardada en la Base de Datos del Servidor.");
                        a.show();
                    } else {
                        Alert a = new Alert(Alert.AlertType.ERROR, "Error al guardar la sala.");
                        a.show();
                    }
                });
            } catch (java.io.IOException ex) {
                javafx.application.Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.ERROR, "No se pudo conectar al servidor:\n" + ex.getMessage());
                    a.show();
                });
            } finally {
                cliente.disconnect();
            }
        }).start();
    }
    
    private String extractField(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        
        if (json.charAt(start) == '"') {
            start++; // saltar comilla
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
