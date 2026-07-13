package com.cine.cliente.ui.vistas;

import com.cine.cliente.ClienteRed;
import com.cine.cliente.ui.componentes.atomos.BotonButaca;
import com.cine.cliente.ui.componentes.organismos.PanelCabecera;
import com.cine.cliente.ui.componentes.organismos.PanelCuadriculaButacas;
import com.cine.cliente.ui.componentes.organismos.PanelSeleccion;
import com.cine.dominio.*;
import com.cine.dominio.precios.MotorPrecios;
import com.cine.compartido.Protocolo;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.util.*;

public class PuntoVentaVista extends BorderPane {

    private final Cine demoCinema;
    private final SalaCine demoRoom;
    private final Pelicula demoMovie;
    private final Funcion demoShowtime;
    private final MotorPrecios motor;

    private ClienteRed clienteRed;
    private final Set<String> mySelectedSeatIds = new LinkedHashSet<>();
    private final List<Butaca> mySelectedSeats = new ArrayList<>();

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
        String json = clienteRed.getRoomState();
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
                    applyStatusToSeat(butaca, serverStatus);
                    if ("SELECTED".equals(serverStatus)) {
                        BotonButaca btn = panelCuadriculaButacas.getSeatButton(butaca.getId());
                        if (btn != null) {
                            btn.setLockedByOther(true);
                        }
                    }
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
            new Thread(() -> {
                try {
                    boolean ok = clienteRed.selectSeat(butaca.getId());
                    Platform.runLater(() -> {
                        if (ok) {
                            butaca.seleccionar();
                            BotonButaca btn = panelCuadriculaButacas.getSeatButton(butaca.getId());
                            if (btn != null) btn.actualizarVisuales();
                            mySelectedSeatIds.add(butaca.getId());
                            mySelectedSeats.add(butaca);
                            updateUI();
                        } else {
                            showAlert(Alert.AlertType.WARNING, "Butaca no disponible", "La butaca fue tomada por otro cliente.");
                        }
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        butaca.seleccionar();
                        mySelectedSeatIds.add(butaca.getId());
                        mySelectedSeats.add(butaca);
                        updateUI();
                    });
                }
            }, "seleccionar-thread").start();

        } else if (butaca.getEstado() == EstadoButaca.SELECTED) {
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
                        ? clienteRed.bookSeats(seatIds, dni)
                        : "LOCAL-" + UUID.randomUUID().toString().substring(0, 6);

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
                        showAlert(Alert.AlertType.INFORMATION, "Compra confirmada",
                                seatsSnap.size() + " butaca(s) reservadas.\nRef: " + refCode);
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
        if (clienteRed != null) {
            clienteRed.disconnect();
            clienteRed = null;
        }
        showAlert(Alert.AlertType.WARNING, "Atención finalizada", 
            "El tiempo de sesión ha culminado o la transacción ha sido completada.");
        // Regresar a pantalla de inicio
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
