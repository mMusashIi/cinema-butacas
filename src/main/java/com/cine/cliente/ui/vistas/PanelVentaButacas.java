package com.cine.cliente.ui.vistas;

import atlantafx.base.theme.Styles;
import com.cine.cliente.ClienteRed;
import com.cine.cliente.ui.GestorVistas;
import com.cine.cliente.ui.componentes.atomos.BotonVolver;
import com.cine.cliente.ui.componentes.atomos.BotonButacaSvg;
import com.cine.cliente.ui.componentes.moleculas.ItemLeyenda;
import com.cine.cliente.ui.componentes.organismos.CuadriculaSala;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import com.cine.dominio.MetodoPago;
import java.util.Optional;
import com.cine.cliente.ui.componentes.moleculas.DialogSeleccionPago;
import com.cine.cliente.ui.componentes.moleculas.DialogBoleta;

public class PanelVentaButacas extends BorderPane {

    private final ClienteRed cliente;
    private final PanelCliente panelAnterior;
    private final String funcionId;
    private final LocalDate fechaReservada;
    private final String tituloLimpio;
    
    private final List<String> seleccionadas = new ArrayList<>();
    private final CuadriculaSala cuadriculaSala = new CuadriculaSala();
    private Button btnComprar;
    
    private Label lblTotal;
    private Label lblLibres;
    private Label lblOcupadas;
    private ProgressBar progressBar;
    private Label lblAgotado;
    
    private Label lblSeleccionadasText;
    
    private String[][] lockIdsMatriz;
    private String[][] estadosMatriz;

    public PanelVentaButacas(ClienteRed cliente, PanelCliente panelAnterior, String funcionId, LocalDate fechaReservada, String peli, String sala, String hora) {
        this.cliente = cliente;
        this.panelAnterior = panelAnterior;
        this.funcionId = funcionId;
        this.fechaReservada = fechaReservada;
        this.tituloLimpio = peli + " - " + sala + " - " + hora;

        setStyle("-fx-background-color: #121212;");

        // --- TOP ---
        VBox topBox = new VBox(15);
        topBox.setPadding(new Insets(20));
        topBox.setAlignment(Pos.CENTER);

        HBox cabecera = new HBox(20);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        
        BotonVolver btnBack = new BotonVolver(() -> {
            // Liberar butacas seleccionadas antes de volver
            for(String lockId : seleccionadas) {
                try {
                    cliente.deselectSeat(lockId);
                } catch(Exception ignored) {}
            }
            // ¡NUNCA desconectar el socket aquí! cliente.disconnect() rompe la sesión de red.
            GestorVistas.navegarA(panelAnterior);
        });
        
        Label title = new Label(tituloLimpio + " (" + fechaReservada.toString() + ")");
        title.getStyleClass().add(Styles.TITLE_3);
        title.setStyle("-fx-text-fill: white;");
        cabecera.getChildren().addAll(btnBack, title);

        // Leyenda
        HBox leyenda = new HBox(15,
            new ItemLeyenda("Libre", "LIBRE"),
            new ItemLeyenda("Tu Selección", "SELECCIONADO"),
            new ItemLeyenda("En Proceso", "LOCKED"),
            new ItemLeyenda("Ocupado", "OCUPADO"),
            new ItemLeyenda("Dañada", "BROKEN")
        );
        topBox.getChildren().addAll(cabecera, leyenda);
        setTop(topBox);

        // --- CENTER ---
        VBox centerBox = new VBox(20);
        centerBox.setAlignment(Pos.TOP_CENTER);
        centerBox.setPadding(new Insets(20));
        
        ScrollPane scrollPane = new ScrollPane(cuadriculaSala);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        centerBox.getChildren().add(scrollPane);
        setCenter(centerBox);

        // --- RIGHT ---
        VBox rightBox = new VBox(15);
        rightBox.setPadding(new Insets(20));
        rightBox.setPrefWidth(250);
        rightBox.setStyle("-fx-background-color: #1a1a1a; -fx-border-color: #333; -fx-border-width: 0 0 0 1;");
        
        Label lblResumen = new Label("Resumen");
        lblResumen.getStyleClass().add(Styles.TITLE_4);
        lblResumen.setStyle("-fx-text-fill: white;");

        lblTotal = new Label("Total: -");
        lblLibres = new Label("Libres: -");
        lblLibres.setStyle("-fx-text-fill: #4ade80;"); // verde
        lblOcupadas = new Label("Ocupadas: -");
        lblOcupadas.setStyle("-fx-text-fill: #f87171;"); // rojo

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: #f87171;");

        lblAgotado = new Label("");
        lblAgotado.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
        lblAgotado.setManaged(false);
        lblAgotado.setVisible(false);

        VBox conteoBox = new VBox(5, lblTotal, lblLibres, lblOcupadas, progressBar, lblAgotado);
        conteoBox.setPadding(new Insets(10, 0, 10, 0));

        Button btnRefrescar = new Button("Refrescar Manualmente");
        btnRefrescar.setMaxWidth(Double.MAX_VALUE);
        btnRefrescar.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnRefrescar.setOnAction(e -> {
            for (String lid : seleccionadas) {
                try {
                    cliente.deselectSeat(lid);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            seleccionadas.clear();
            refrescarSala();
        });

        btnComprar = new Button("Confirmar Compra (0)");
        btnComprar.setMaxWidth(Double.MAX_VALUE);
        btnComprar.getStyleClass().add(Styles.SUCCESS);
        btnComprar.setOnAction(e -> comprarSeleccion());
        
        lblSeleccionadasText = new Label("Seleccionadas:\nNinguna");
        lblSeleccionadasText.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold; -fx-wrap-text: true;");
        
        rightBox.getChildren().addAll(lblResumen, conteoBox, new Separator(), lblSeleccionadasText, new Separator(), btnRefrescar, new Region(), btnComprar);
        VBox.setVgrow(rightBox.getChildren().get(6), Priority.ALWAYS); // Spacer
        setRight(rightBox);

        cuadriculaSala.setOnCellClick(this::manejarClicEnSillon);

        cliente.setOnSeatUpdate((sId, estado) -> {
            if (sId.startsWith(funcionId + "_")) {
                refrescarSala();
            }
        });

        refrescarSala();
    }

    private void refrescarSala() {
        new Thread(() -> {
            try {
                String rawState = cliente.getRoomState(funcionId, fechaReservada.toString());
                int[] conteo = cliente.getConteoButacas(funcionId, fechaReservada.toString());
                if (rawState != null && !rawState.isEmpty()) {
                    Platform.runLater(() -> {
                        procesarDatosDeRed(rawState);
                        actualizarConteoUI(conteo);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> mostrarAlerta("Error de Red", e.getMessage()));
            }
        }).start();
    }

    private void procesarDatosDeRed(String rawState) {
        String[] filasStr = rawState.split(";");
        if (filasStr.length == 0) return;

        int numFilas = filasStr.length;
        int numCols = filasStr[0].split(",").length;
        
        estadosMatriz = new String[numFilas][numCols];
        lockIdsMatriz = new String[numFilas][numCols];
        String[][] tooltips = new String[numFilas][numCols];

        for (int r = 0; r < numFilas; r++) {
            String[] celdas = filasStr[r].split(",");
            for (int c = 0; c < numCols && c < celdas.length; c++) {
                String info = celdas[c];
                if (info.equals("null")) {
                    estadosMatriz[r][c] = "PASILLO";
                    lockIdsMatriz[r][c] = null;
                } else {
                    int lastColon = info.lastIndexOf(':');
                    if (lastColon != -1 && lastColon < info.length() - 1) {
                        String lockId = info.substring(0, lastColon);
                        String estadoOriginal = info.substring(lastColon + 1);
                        
                        String shortId = lockId.contains("_") ? lockId.substring(lockId.lastIndexOf('_') + 1) : lockId;
                        
                        lockIdsMatriz[r][c] = lockId;
                        tooltips[r][c] = "Butaca: " + shortId + "\nEstado: " + estadoOriginal;
                        
                        if (estadoOriginal.equals("SELECCIONADO") || seleccionadas.contains(lockId)) {
                            estadosMatriz[r][c] = "SELECCIONADO";
                        } else if (estadoOriginal.equals("LOCKED")) {
                            estadosMatriz[r][c] = "LOCKED";
                        } else if (estadoOriginal.equals("BROKEN") || estadoOriginal.equals("OCUPADO")) {
                            estadosMatriz[r][c] = estadoOriginal; 
                        } else {
                            estadosMatriz[r][c] = "LIBRE";
                        }
                    }
                }
            }
        }
        
        cuadriculaSala.renderizarMatriz(estadosMatriz, tooltips);
        
        for (int r = 0; r < numFilas; r++) {
            for (int c = 0; c < numCols; c++) {
                if ("OCUPADO".equals(estadosMatriz[r][c])) {
                    BotonButacaSvg btn = cuadriculaSala.getBotonAt(r, c);
                    if (btn != null) btn.setDisable(true);
                }
            }
        }
        
        actualizarBotonComprar();
    }

    private void manejarClicEnSillon(int fila, int columna) {
        if (estadosMatriz == null || lockIdsMatriz == null) return;
        String lockId = lockIdsMatriz[fila][columna];
        if (lockId == null) return;
        
        BotonButacaSvg btn = cuadriculaSala.getBotonAt(fila, columna);
        if (btn == null) return;
        
        if ("OCUPADO".equals(estadosMatriz[fila][columna])) return;
        
        try {
            if (seleccionadas.contains(lockId)) {
                if (cliente.deselectSeat(lockId)) {
                    seleccionadas.remove(lockId);
                    btn.aplicarColorPorEstado("LIBRE");
                }
            } else {
                if (cliente.selectSeat(lockId)) {
                    seleccionadas.add(lockId);
                    btn.aplicarColorPorEstado("SELECCIONADO");
                }
            }
            actualizarBotonComprar();
        } catch (Exception e) {
            mostrarAlerta("Error", "Error al cambiar estado: " + e.getMessage());
        }
    }
    
    private void actualizarBotonComprar() {
        btnComprar.setText("Confirmar Compra (" + seleccionadas.size() + ")");
        
        if (seleccionadas.isEmpty()) {
            lblSeleccionadasText.setText("Seleccionadas:\nNinguna");
        } else {
            List<String> cortos = seleccionadas.stream()
                .map(s -> s.contains("_") ? s.substring(s.lastIndexOf('_') + 1) : s)
                .toList();
            lblSeleccionadasText.setText("Seleccionadas:\n" + String.join(", ", cortos));
        }
    }

    private void comprarSeleccion() {
        if (seleccionadas.isEmpty()) {
            mostrarAlerta("Atención", "No hay butacas seleccionadas.");
            return;
        }
        
        Optional<DialogSeleccionPago.ResultadoPago> resOpt = DialogSeleccionPago.mostrar();
        if (resOpt.isEmpty()) {
            return; // Cancelado por el usuario
        }
        
        MetodoPago metodoPago = resOpt.get().metodo;
        String dni = resOpt.get().dni;

        try {
            String res = cliente.bookSeats(seleccionadas, fechaReservada, metodoPago, dni);
            if (res != null) {
                // res debe ser el JSON de la boleta si la compra fue exitosa
                Platform.runLater(() -> {
                    try {
                        DialogBoleta.mostrar(res);
                        seleccionadas.clear();
                        refrescarSala();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        mostrarAlerta("Error en Boleta", "Error al mostrar boleta: " + ex.getMessage());
                    }
                });
            } else {
                mostrarAlerta("Error", "La compra falló. Es posible que los asientos ya no estén disponibles.");
            }
        } catch (Exception ex) {
            mostrarAlerta("Error", ex.getMessage());
        }
    }

    private void actualizarConteoUI(int[] conteo) {
        if (conteo == null || conteo.length < 3) return;
        int total = conteo[0];
        int libres = conteo[1];
        int ocupadas = conteo[2];

        lblTotal.setText("Total: " + total);
        lblLibres.setText("Libres: " + libres);
        lblOcupadas.setText("Ocupadas: " + ocupadas);

        if (total > 0) {
            progressBar.setProgress((double) ocupadas / total);
        } else {
            progressBar.setProgress(0);
        }

        if (libres == 0 && total > 0) {
            lblAgotado.setText("¡SALA AGOTADA!");
            lblAgotado.setManaged(true);
            lblAgotado.setVisible(true);
            btnComprar.setDisable(true);
        } else {
            lblAgotado.setManaged(false);
            lblAgotado.setVisible(false);
            btnComprar.setDisable(false);
        }
    }

    private void mostrarAlerta(String titulo, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }
}
