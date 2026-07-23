package com.cine.cliente.ui.componentes.moleculas;

import com.cine.cliente.util.GeneradorPdfBoleta;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Diálogo de comprobante de compra.
 * Diseño de ticket inspirado en el PDF generado por GeneradorPdfBoleta:
 * header oscuro con datos del cine + referencia, cuerpo en dos columnas,
 * separadores por sección, y pie de emisión.
 */
public class DialogBoleta {

    // Paleta que espeja la del PDF
    private static final String COLOR_HEADER_BG  = "#1e1e2d";
    private static final String COLOR_BODY_BG     = "#f8f8f8";
    private static final String COLOR_LABEL       = "#666688";
    private static final String COLOR_VALUE       = "#1a1a2e";
    private static final String COLOR_SEPARATOR   = "#ddddee";
    private static final String COLOR_REF         = "#e6b429";
    private static final String COLOR_CONFIRM_BG  = "#1e1e2d";
    private static final String COLOR_CONFIRM_TXT = "#ffffff";
    private static final String COLOR_CONFIRM_OK  = "#2ecc71";

    public static void mostrar(String jsonBoleta) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Comprobante de Compra");
        stage.setResizable(false);

        // ===== EXTRAER CAMPOS =====
        String id        = extractField(jsonBoleta, "id");
        String peli      = nvl(extractField(jsonBoleta, "pelicula"),      "N/A");
        String sala      = nvl(extractField(jsonBoleta, "sala"),          "N/A");
        String hora      = nvl(extractField(jsonBoleta, "hora"),          "");
        String fechaRes  = nvl(extractField(jsonBoleta, "fechaReservada"),"");
        String pago      = nvl(extractField(jsonBoleta, "metodoPago"),    "N/A");
        String dni       = nvl(extractField(jsonBoleta, "dni"),           "noDNI");
        String asientosR = nvl(extractField(jsonBoleta, "asientos"),      "");
        String emision   = nvl(extractField(jsonBoleta, "fechaEmision"),  "");

        // Limpiar hora (ISO → HH:mm)
        if (hora.contains("T")) hora = hora.split("T")[1].substring(0, 5);

        // Limpiar emisión (ISO → dd/MM/yyyy HH:mm)
        if (emision.contains("T")) emision = emision.replace("T", " ").substring(0, 16);

        // ID corto para referencia (sin UUID crudo)
        String idCorto = (id != null && id.length() >= 8)
                ? "REF-" + id.substring(0, 8).toUpperCase()
                : "N/A";

        // Asientos: solo la parte legible tras el último '_'
        String asientos = Arrays.stream(asientosR.split(","))
                .filter(s -> !s.isBlank())
                .map(s -> s.contains("_") ? s.substring(s.lastIndexOf('_') + 1) : s)
                .collect(Collectors.joining("   "));

        // ===== HEADER (espeja PDF) =====
        BorderPane header = new BorderPane();
        header.setStyle("-fx-background-color: " + COLOR_HEADER_BG + "; -fx-padding: 18 22 18 22;");

        VBox headerLeft = new VBox(3);
        Label lblCine = new Label("CINE BUTACAS CORE");
        lblCine.setFont(Font.font("System", FontWeight.BOLD, 20));
        lblCine.setTextFill(Color.WHITE);
        Label lblSub = new Label("Comprobante Oficial de Compra");
        lblSub.setFont(Font.font("System", 12));
        lblSub.setTextFill(Color.LIGHTGRAY);
        headerLeft.getChildren().addAll(lblCine, lblSub);

        VBox headerRight = new VBox(2);
        headerRight.setAlignment(Pos.CENTER_RIGHT);
        Label lblRef = new Label(idCorto);
        lblRef.setFont(Font.font("System", FontWeight.BOLD, 12));
        lblRef.setTextFill(Color.web(COLOR_REF));
        Label lblRefLabel = new Label("NO. BOLETA");
        lblRefLabel.setFont(Font.font("System", 8));
        lblRefLabel.setTextFill(Color.LIGHTGRAY);
        headerRight.getChildren().addAll(lblRef, lblRefLabel);

        header.setLeft(headerLeft);
        header.setRight(headerRight);

        // ===== CUERPO: GRILLA DOS COLUMNAS (igual que PDF) =====
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(0);
        grid.setPadding(new Insets(20, 22, 10, 22));
        grid.setStyle("-fx-background-color: " + COLOR_BODY_BG + ";");

        ColumnConstraints colLabel = new ColumnConstraints(110);
        ColumnConstraints colValue = new ColumnConstraints();
        colValue.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(colLabel, colValue);

        int row = 0;

        // Sección: Función
        row = addRow(grid, row, "PELICULA",  peli);
        row = addRow(grid, row, "SALA",      sala);
        row = addRow(grid, row, "FECHA",     fechaRes);
        row = addRow(grid, row, "HORA",      hora);

        row = addSeparator(grid, row);

        // Sección: Compra
        row = addRow(grid, row, "DNI",       dni);
        row = addRow(grid, row, "ASIENTOS",  asientos);
        row = addRow(grid, row, "PAGO",      pago);

        row = addSeparator(grid, row);

        // Sección: Confirmación (barra oscura como en el PDF)
        HBox confirmBox = new HBox();
        confirmBox.setStyle("-fx-background-color: " + COLOR_CONFIRM_BG + "; -fx-padding: 8 10 8 10;");
        confirmBox.setAlignment(Pos.CENTER_LEFT);
        Label lblConfirm = new Label("COMPRA CONFIRMADA");
        lblConfirm.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblConfirm.setTextFill(Color.web(COLOR_CONFIRM_TXT));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label lblOk = new Label("OK");
        lblOk.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblOk.setTextFill(Color.web(COLOR_CONFIRM_OK));
        confirmBox.getChildren().addAll(lblConfirm, spacer, lblOk);

        GridPane.setColumnSpan(confirmBox, 2);
        grid.add(confirmBox, 0, row++);

        // Pie de emisión
        Label lblEmision = new Label("Emitido: " + emision);
        lblEmision.setFont(Font.font("System", 9));
        lblEmision.setTextFill(Color.GRAY);
        lblEmision.setPadding(new Insets(6, 0, 0, 0));
        GridPane.setColumnSpan(lblEmision, 2);
        grid.add(lblEmision, 0, row++);

        // ===== BOTONES =====
        Button btnPdf = new Button("Guardar PDF");
        btnPdf.setStyle(
            "-fx-background-color: #27ae60; -fx-text-fill: white; " +
            "-fx-padding: 10 28; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13;"
        );
        btnPdf.setOnAction(e -> guardarPdf(jsonBoleta, idCorto, stage));

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setStyle(
            "-fx-background-color: #2c3e50; -fx-text-fill: white; " +
            "-fx-padding: 10 28; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13;"
        );
        btnCerrar.setOnAction(e -> stage.close());

        HBox botonesBox = new HBox(10, btnPdf, btnCerrar);
        botonesBox.setAlignment(Pos.CENTER);
        botonesBox.setPadding(new Insets(14, 22, 18, 22));
        botonesBox.setStyle("-fx-background-color: " + COLOR_BODY_BG + ";");

        // ===== RAÍZ =====
        VBox root = new VBox(header, grid, botonesBox);
        root.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0.3, 0, 2);");
        root.setPrefWidth(460);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.showAndWait();
    }

    // =========================================================================
    // Helpers de construcción de grilla
    // =========================================================================

    private static int addRow(GridPane grid, int row, String label, String value) {
        Label lblKey = new Label(label);
        lblKey.setFont(Font.font("System", FontWeight.BOLD, 8));
        lblKey.setTextFill(Color.web(COLOR_LABEL));
        lblKey.setPadding(new Insets(8, 0, 2, 0));

        Label lblVal = new Label(value);
        lblVal.setFont(Font.font("System", 13));
        lblVal.setTextFill(Color.web(COLOR_VALUE));
        lblVal.setWrapText(true);
        lblVal.setMaxWidth(280);
        lblVal.setPadding(new Insets(8, 0, 2, 0));

        grid.add(lblKey, 0, row);
        grid.add(lblVal, 1, row);
        return row + 1;
    }

    private static int addSeparator(GridPane grid, int row) {
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: " + COLOR_SEPARATOR + ";");
        GridPane.setColumnSpan(sep, 2);
        GridPane.setMargin(sep, new Insets(6, 0, 6, 0));
        grid.add(sep, 0, row);
        return row + 1;
    }

    // =========================================================================
    // Guardar PDF
    // =========================================================================

    private static void guardarPdf(String jsonBoleta, String idCorto, Stage owner) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar Boleta PDF");
        fc.setInitialFileName("boleta_" + idCorto.replace("REF-", "") + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo PDF", "*.pdf"));

        File dirInicial = new File(System.getProperty("user.home") + File.separator + "Desktop");
        if (!dirInicial.exists()) dirInicial = new File(System.getProperty("user.home"));
        fc.setInitialDirectory(dirInicial);

        File archivo = fc.showSaveDialog(owner);
        if (archivo == null) return;

        new Thread(() -> {
            try {
                GeneradorPdfBoleta.generateToFile(jsonBoleta, archivo.toPath());
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(archivo);
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error al generar PDF");
                    alert.setHeaderText(null);
                    alert.setContentText("No se pudo generar el PDF:\n" + ex.getMessage());
                    alert.show();
                });
            }
        }).start();
    }

    // =========================================================================
    // Utilidades
    // =========================================================================

    private static String extractField(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        if (start < json.length() && json.charAt(start) == '"') {
            start++;
            int end = json.indexOf("\"", start);
            return end >= 0 ? json.substring(start, end) : json.substring(start);
        } else {
            int endComa  = json.indexOf(",", start);
            int endLlave = json.indexOf("}", start);
            if (endComa  == -1) endComa  = Integer.MAX_VALUE;
            if (endLlave == -1) endLlave = Integer.MAX_VALUE;
            int end = Math.min(endComa, endLlave);
            return end != Integer.MAX_VALUE ? json.substring(start, end).trim() : json.substring(start).trim();
        }
    }

    private static String nvl(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
