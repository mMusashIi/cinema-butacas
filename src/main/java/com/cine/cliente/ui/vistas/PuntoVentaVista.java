package com.cine.cliente.ui.vistas;

import com.cine.cliente.ClienteRed;
import com.cine.cliente.ui.componentes.atomos.BotonButaca;
import com.cine.cliente.ui.componentes.organismos.PanelCabecera;
import com.cine.cliente.ui.componentes.organismos.PanelCuadriculaButacas;
import com.cine.cliente.ui.componentes.organismos.PanelSeleccion;
import com.cine.dominio.*;
import com.cine.dominio.precios.MotorPrecios;
import com.cine.compartido.Protocolo;
import com.cine.dominio.boletos.Boleto;
import com.cine.dominio.boletos.GeneradorPdfBoleto;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class PuntoVentaVista extends BorderPane {

    private final Cine demoCinema;
    private final SalaCine demoRoom;
    private final Pelicula demoMovie;
    private final Funcion demoShowtime;
    private final MotorPrecios motor;

    private ClienteRed clienteRed;
    private final Set<String> mySelectedSeatIds = new LinkedHashSet<>();
    private final List<Butaca> mySelectedSeats = new ArrayList<>();

    /**
     * Bug 5: Garantiza que el fin de sesión (por timeout del servidor o por el
     * botón "← Volver") se procese exactamente una vez, evitando doble-navegación.
     */
    private final AtomicBoolean sessionHandled = new AtomicBoolean(false);

    private PanelCabecera panelCabecera;
    private PanelCuadriculaButacas panelCuadriculaButacas;
    private PanelSeleccion panelSeleccion;

    public PuntoVentaVista(Cine cinema, SalaCine room, Pelicula movie, Funcion showtime, MotorPrecios motor) {
        this.demoCinema = cinema;
        this.demoRoom = room;
        this.demoMovie = movie;
        this.demoShowtime = showtime;
        this.motor = motor;

        setStyle("-fx-background-color: #121212;");

        panelCuadriculaButacas = new PanelCuadriculaButacas(demoRoom, this::manejarClicButaca);
        panelCabecera = new PanelCabecera(demoShowtime);

        javafx.scene.control.Button btnVolver = new javafx.scene.control.Button("← Volver");
        btnVolver.setStyle("-fx-cursor: hand; -fx-background-color: #333; -fx-text-fill: white; -fx-padding: 5 15;");
        btnVolver.setOnAction(e -> {
            // Bug 5: marcar la sesión como manejada para que onSessionExpired
            // ignore cualquier mensaje tardío que llegue del servidor.
            if (!sessionHandled.compareAndSet(false, true)) return;
            if (clienteRed != null) clienteRed.disconnect();
            com.cine.cliente.ui.GestorVistas.navegarA(new SeleccionFuncionVista());
        });

        javafx.scene.layout.VBox topContainer = new javafx.scene.layout.VBox(10);
        topContainer.setPadding(new javafx.geometry.Insets(10, 10, 0, 10));
        topContainer.getChildren().addAll(btnVolver, panelCabecera);

        setTop(topContainer);
        setCenter(panelCuadriculaButacas);

        panelSeleccion = new PanelSeleccion(demoShowtime, motor,
                this::manejarRemoverButaca, this::manejarConfirmar, this::manejarCancelar);
        setRight(panelSeleccion);

        // Conectar al servidor en un hilo de fondo
        new Thread(this::conectarAlServidor, "connect-thread").start();
    }

    private void conectarAlServidor() {
        ClienteRed cliente = new ClienteRed();
        try {
            cliente.connect(
                    this::onSeatUpdateReceived,
                    this::onSessionTimeReceived,
                    this::onSessionExpired,
                    this::onDisconnected
            );
            clienteRed = cliente;

            sincronizarEstadoSalaDesdeServidor();
        } catch (IOException e) {
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.WARNING, "Sin conexión",
                        "No se pudo conectar al servidor en " + Protocolo.HOST + ":" + Protocolo.PORT + ".\n"
                        + "La aplicación funciona en modo offline.");
            });
        }
    }

    private void sincronizarEstadoSalaDesdeServidor() throws IOException {
        String json = clienteRed.getRoomState(demoShowtime.getId());
        parseAndApplyRoomState(json);
    }

    private void parseAndApplyRoomState(String json) {
        String[] objects = json.replaceAll("\\[|\\]", "").split("\\},\\{");
        Map<String, String> statusMap = new HashMap<>();

        for (String obj : objects) {
            obj = obj.replace("{", "").replace("}", "");
            String seatId = extractField(obj, "id");
            String estado = extractField(obj, "estado");
            if (seatId != null && estado != null) {
                statusMap.put(seatId, estado);
            }
        }

        Platform.runLater(() -> {
            for (Butaca butaca : demoRoom.getTodasLasButacas()) {
                String serverStatus = statusMap.get(butaca.getId());
                if (serverStatus != null) {
                    // Bug 1-Cliente: Si el servidor reporta una butaca como SELECTED en el
                    // snapshot inicial, significa que OTRO cliente la tiene bloqueada.
                    // Localmente la tratamos como BOOKED para que sea no-interactuable.
                    // No la agregamos a mySelectedSeatIds porque no es nuestra selección.
                    String localStatus = "SELECTED".equals(serverStatus) ? "BOOKED" : serverStatus;
                    applyStatusToSeat(butaca, localStatus);
                }
            }
            panelCuadriculaButacas.refreshAllSeats();
        });
    }

    private String extractField(String obj, String field) {
        String key = "\"" + field + "\":";
        int idx = obj.indexOf(key);
        if (idx < 0) return null;
        String rest = obj.substring(idx + key.length()).trim();
        if (rest.startsWith("\"")) {
            rest = rest.substring(1);
            int end = rest.indexOf("\"");
            return end >= 0 ? rest.substring(0, end) : null;
        } else {
            int end = rest.indexOf(",");
            return end >= 0 ? rest.substring(0, end).trim() : rest.trim();
        }
    }

    private void applyStatusToSeat(Butaca butaca, String estado) {
        try {
            EstadoButaca current = butaca.getEstado();
            EstadoButaca target = EstadoButaca.valueOf(estado);
            if (current == target) return;

            switch (target) {
                case FREE -> {
                    if (current == EstadoButaca.SELECTED || current == EstadoButaca.BOOKED) butaca.liberar();
                }
                case BOOKED -> {
                    if (current == EstadoButaca.FREE) butaca.seleccionar();
                    if (butaca.getEstado() == EstadoButaca.SELECTED) butaca.reservarButaca();
                }
                case SELECTED -> {
                    if (current == EstadoButaca.FREE) butaca.seleccionar();
                }
                default -> {}
            }
        } catch (Exception ignored) {}
    }

    private void manejarClicButaca(Butaca butaca) {
        if (butaca.getEstado() == EstadoButaca.BOOKED || butaca.getEstado() == EstadoButaca.BROKEN) return;

        if (butaca.getEstado() == EstadoButaca.FREE) {
            // Fix 1: el clienteRed llega en un hilo async; si aún no conectó, informar.
            if (clienteRed == null) {
                showAlert(Alert.AlertType.WARNING, "Sin conexión",
                        "Todavía conectando al servidor. Intenta en un momento.");
                return;
            }

            // Fix 3: evitar que dos clicks rápidos sobre la misma butaca spawnen
            // dos hilos. Usamos el propio estado FREE→algo para bloquear:
            // marcamos localmente antes de enviar, revertimos si el servidor rechaza.
            butaca.seleccionar();                            // FREE → SELECTED (local, temporal)
            panelCuadriculaButacas.refreshAllSeats();        // refrescar visual inmediatamente

            new Thread(() -> {
                try {
                    String resp = clienteRed.selectSeatRaw(butaca.getId()); // devuelve línea cruda
                    Platform.runLater(() -> {
                        if (resp != null && resp.startsWith(com.cine.compartido.Protocolo.OK)) {
                            // Confirmado por el servidor
                            BotonButaca btn = panelCuadriculaButacas.getSeatButton(butaca.getId());
                            if (btn != null) btn.actualizarVisuales();
                            mySelectedSeatIds.add(butaca.getId());
                            mySelectedSeats.add(butaca);
                            updateUI();
                        } else {
                            // Fix 2: extraer mensaje real del servidor para mostrarlo.
                            String serverMsg = resp != null && resp.contains(":")
                                    ? resp.substring(resp.indexOf(':') + 1).trim()
                                    : "Butaca no disponible.";
                            // Revertir selección local
                            try { butaca.liberar(); } catch (Exception ignored) {}
                            panelCuadriculaButacas.refreshAllSeats();
                            showAlert(Alert.AlertType.WARNING, "Butaca no disponible", serverMsg);
                        }
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        // Sin red: mantener la selección local (modo offline)
                        mySelectedSeatIds.add(butaca.getId());
                        mySelectedSeats.add(butaca);
                        updateUI();
                    });
                }
            }, "seleccionar-thread").start();

        } else if (butaca.getEstado() == EstadoButaca.SELECTED
                && mySelectedSeatIds.contains(butaca.getId())) {
            // Bug 6: solo des-seleccionar si la butaca realmente pertenece a este cliente.
            new Thread(() -> {
                try {
                    if (clienteRed != null) clienteRed.deselectSeat(butaca.getId());
                } catch (IOException ignored) {}
                Platform.runLater(() -> {
                    butaca.liberar();
                    mySelectedSeatIds.remove(butaca.getId());
                    mySelectedSeats.remove(butaca);
                    updateUI();
                });
            }, "deselect-thread").start();
        }
    }


    private void manejarRemoverButaca(Butaca butaca) {
        new Thread(() -> {
            try {
                if (clienteRed != null) clienteRed.deselectSeat(butaca.getId());
            } catch (IOException ignored) {}
            Platform.runLater(() -> {
                butaca.liberar();
                mySelectedSeatIds.remove(butaca.getId());
                mySelectedSeats.remove(butaca);
                updateUI();
            });
        }, "remove-thread").start();
    }

    private void manejarCancelar() {
        List<Butaca> toCancel = new ArrayList<>(mySelectedSeats);
        new Thread(() -> {
            for (Butaca s : toCancel) {
                try {
                    if (clienteRed != null) clienteRed.deselectSeat(s.getId());
                } catch (IOException ignored) {}
            }
            Platform.runLater(() -> {
                for (Butaca s : toCancel) { try { s.liberar(); } catch (Exception ignored) {} }
                mySelectedSeatIds.clear();
                mySelectedSeats.clear();
                updateUI();
            });
        }, "cancel-thread").start();
    }

    private void manejarConfirmar() {
        if (mySelectedSeats.isEmpty()) return;
        String dni = panelSeleccion.getDniInput();
        List<String> seatIds   = new ArrayList<>(mySelectedSeatIds);
        List<Butaca>   seatsSnap = new ArrayList<>(mySelectedSeats);

        new Thread(() -> {
            try {
                String refCode = clienteRed != null
                        ? clienteRed.bookSeats(seatIds, dni, demoShowtime.getId())
                        : "LOCAL-" + java.util.UUID.randomUUID().toString().substring(0, 6);

                Platform.runLater(() -> {
                    if (refCode != null) {
                        for (Butaca s : seatsSnap) {
                            if (s.getEstado() == EstadoButaca.SELECTED) {
                                try { s.reservarButaca(); } catch (Exception ignored) {}
                            }
                            BotonButaca btn = panelCuadriculaButacas.getSeatButton(s.getId());
                            if (btn != null) btn.actualizarVisuales();
                        }
                        mySelectedSeatIds.clear();
                        mySelectedSeats.clear();
                        panelSeleccion.clearDniInput();
                        updateUI();

                        // Marcar sesión como manejada ANTES de mostrar la boleta,
                        // para que onSessionExpired no muestre el dialog de "Atención finalizada".
                        sessionHandled.set(true);
                        mostrarBoleta(refCode, dni, seatsSnap);
                        com.cine.cliente.ui.GestorVistas.navegarA(new PantallaInicio());
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Error en la compra",
                                "El servidor rechazó la operación. Intenta nuevamente.");
                        for (Butaca s : seatsSnap) { try { s.liberar(); } catch (Exception ignored) {} }
                        mySelectedSeatIds.clear();
                        mySelectedSeats.clear();
                        updateUI();
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() ->
                    showAlert(Alert.AlertType.ERROR, "Error de red", e.getMessage()));
            }
        }, "book-thread").start();
    }

    /** Muestra la boleta de compra con todos los detalles de la transacción. */
    private void mostrarBoleta(String refCode, String dni, List<Butaca> seats) {
        javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog =
                new javafx.scene.control.Dialog<>();
        dialog.setTitle("Boleta de Compra");
        dialog.setHeaderText(null);

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(12);
        content.setPadding(new javafx.geometry.Insets(24));
        content.setStyle("-fx-background-color: #1a1a2e;");
        content.setPrefWidth(420);

        // Título de la película
        javafx.scene.control.Label lblPeli = new javafx.scene.control.Label("🎬  " + demoMovie.getTitulo());
        lblPeli.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        lblPeli.setWrapText(true);

        // Separador
        javafx.scene.control.Separator sep1 = new javafx.scene.control.Separator();
        sep1.setStyle("-fx-background-color: #333;");

        // Detalles de función
        javafx.scene.control.Label lblCine = row("🏛️", demoCinema.getNombre() + "  ·  " + demoRoom.getNombre());
        javafx.scene.control.Label lblFecha = row("📅", demoShowtime.getStartTime()
                .format(java.time.format.DateTimeFormatter.ofPattern("EEEE d MMM yyyy",
                        new java.util.Locale("es", "ES"))));
        javafx.scene.control.Label lblHora = row("🕐", demoShowtime.getStartTime()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        javafx.scene.control.Label lblFormato = row("🎬", demoShowtime.getFormat().name());

        // Butacas
        String butacasStr = seats.stream()
                .map(b -> b.getFila() + b.getNumero() + " (" + b.getTipo().displayName() + ")")
                .collect(java.util.stream.Collectors.joining(",  "));
        javafx.scene.control.Label lblButacas = row("🪯", butacasStr);
        lblButacas.setWrapText(true);

        // DNI
        String dniDisplay = (dni != null && !dni.isBlank()) ? dni : "Anónimo";
        javafx.scene.control.Label lblDni = row("👤", "DNI / ID: " + dniDisplay);

        javafx.scene.control.Separator sep2 = new javafx.scene.control.Separator();
        sep2.setStyle("-fx-background-color: #444;");

        // Precio total
        double total = seats.stream()
                .mapToDouble(b -> motor.calcularPrecio(b, demoShowtime))
                .sum();
        javafx.scene.control.Label lblPrecio = new javafx.scene.control.Label(
                String.format("Total pagado:  $%.2f", total));
        lblPrecio.setStyle("-fx-font-size: 15px; -fx-text-fill: #aaaaaa;");

        // Código de referencia (destacado)
        javafx.scene.control.Label lblRef = new javafx.scene.control.Label(refCode);
        lblRef.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #f1c40f; "
                + "-fx-background-color: #2a2a00; -fx-padding: 10 18; -fx-background-radius: 6;");
        lblRef.setAlignment(javafx.geometry.Pos.CENTER);
        lblRef.setMaxWidth(Double.MAX_VALUE);

        javafx.scene.control.Label lblRefTitulo = new javafx.scene.control.Label("Código de referencia");
        lblRefTitulo.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");

        javafx.scene.control.Button btnDownloadPdf = new javafx.scene.control.Button("📥 Descargar Boleta PDF");
        btnDownloadPdf.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        btnDownloadPdf.setOnAction(e -> {
            List<Boleto> boletos = new ArrayList<>();
            for (Butaca b : seats) {
                Boleto boleto = new Boleto.Builder()
                        .butaca(b)
                        .funcion(demoShowtime)
                        .buyerIdNumber(dni)
                        .pricePaid(motor.calcularPrecio(b, demoShowtime))
                        .originalPrice(motor.calcularPrecio(b, demoShowtime))
                        .referenceCode(refCode)
                        .build();
                boletos.add(boleto);
            }
            FileChooser fc = new FileChooser();
            fc.setTitle("Guardar Boleta PDF");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            fc.setInitialFileName("Boleta_" + refCode + ".pdf");
            File file = fc.showSaveDialog(getScene().getWindow());
            if (file != null) {
                try {
                    GeneradorPdfBoleto.generateToFile(boletos.get(0), file.toPath()); // genera el primero? no, generateBatch
                    if (boletos.size() > 1) {
                         GeneradorPdfBoleto.generateBatch(boletos); // Retorna byte[], hagamos generateToFile pero no hay método para list en disco directo
                    }
                    // Escribir bytes a disco:
                    byte[] pdfBytes = boletos.size() > 1 ? GeneradorPdfBoleto.generateBatch(boletos) : GeneradorPdfBoleto.generate(boletos.get(0));
                    java.nio.file.Files.write(file.toPath(), pdfBytes);
                    showAlert(Alert.AlertType.INFORMATION, "PDF Guardado", "La boleta se guardó en: " + file.getAbsolutePath());
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Error", "No se pudo guardar el PDF: " + ex.getMessage());
                }
            }
        });

        content.getChildren().addAll(
                lblPeli, sep1,
                lblCine, lblFecha, lblHora, lblFormato, lblButacas, lblDni,
                sep2, lblPrecio, lblRefTitulo, lblRef, btnDownloadPdf);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: #1a1a2e;");
        dialog.getDialogPane().getButtonTypes().add(
                new javafx.scene.control.ButtonType("Listo ✓",
                        javafx.scene.control.ButtonBar.ButtonData.OK_DONE));
        dialog.showAndWait();
    }

    /** Helper para crear una fila de icono + texto en la boleta. */
    private javafx.scene.control.Label row(String icon, String text) {
        javafx.scene.control.Label lbl = new javafx.scene.control.Label(icon + "  " + text);
        lbl.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 13px;");
        return lbl;
    }

    private void onSeatUpdateReceived(String seatId, String netStatus) {
        if (!Protocolo.NET_LOCKED.equals(netStatus)) {
            Butaca butaca = demoRoom.getTodasLasButacas().stream()
                    .filter(s -> s.getId().equals(seatId)).findFirst().orElse(null);
            if (butaca != null) {
                applyStatusToSeat(butaca, netStatus);

                if (("FREE".equals(netStatus) || "BOOKED".equals(netStatus))
                        && mySelectedSeatIds.contains(seatId)) {
                    mySelectedSeatIds.remove(seatId);
                    mySelectedSeats.remove(butaca);
                }
            }
        }
        panelCuadriculaButacas.manejarActualizacionRed(seatId, netStatus, mySelectedSeatIds);
        if (!Protocolo.NET_LOCKED.equals(netStatus) && !mySelectedSeats.isEmpty()) {
            panelSeleccion.updateSelection(mySelectedSeats);
        }
    }

    private void onSessionTimeReceived(long secondsRemaining) {
        panelCabecera.actualizarTemporizador(secondsRemaining);
    }

    private void onSessionExpired() {
        // Si manejarConfirmar ya marcó sessionHandled=true (compra exitosa),
        // no hacemos nada aquí — la navegación la gestiona manejarConfirmar.
        if (!sessionHandled.compareAndSet(false, true)) return;
        if (clienteRed != null) {
            clienteRed.disconnect();
            clienteRed = null;
        }
        // Navegar silenciosamente sin mostrar ningún diálogo de "sesión expirada".
        Platform.runLater(() -> com.cine.cliente.ui.GestorVistas.navegarA(new PantallaInicio()));
    }

    private void onDisconnected(String reason) {
        showAlert(Alert.AlertType.ERROR, "Conexión perdida", reason);
    }

    private void updateUI() {
        panelSeleccion.updateSelection(mySelectedSeats);
        panelCuadriculaButacas.refreshAllSeats();
    }

    private void showAlert(Alert.AlertType tipo, String title, String message) {
        Alert alert = new Alert(tipo, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
    
    public void stop() {
        if (clienteRed != null) {
            clienteRed.disconnect();
        }
    }
}
