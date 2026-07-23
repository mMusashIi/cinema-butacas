package com.cine.cliente.ui.vistas;

import com.cine.cliente.ClienteRed;
import com.cine.cliente.ui.GestorVistas;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import com.cine.cliente.ui.componentes.atomos.BotonVolver;
import com.cine.cliente.ui.componentes.atomos.TarjetaFuncion;
import atlantafx.base.theme.Styles;
import java.time.LocalDate;
import javafx.scene.control.DateCell;
import javafx.util.Callback;

public class PanelCliente extends BorderPane {

    private final ClienteRed cliente;
    private final PantallaInicio pantallaInicio;
    private VBox boxFunciones;
    private DatePicker datePicker;
    private String lastJsonStr = "[]";

    public PanelCliente(ClienteRed cliente, PantallaInicio pantallaInicio) {
        this.cliente = cliente;
        this.pantallaInicio = pantallaInicio;

        setStyle("-fx-background-color: #121212;");

        // Cabecera superior
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 20, 15, 20));
        header.setStyle("-fx-background-color: #1e1e1e; -fx-border-color: #333; -fx-border-width: 0 0 1 0;");

        Label titulo = new Label("Elegir Función a Comprar");
        titulo.getStyleClass().add(Styles.TITLE_3);
        titulo.setStyle("-fx-text-fill: white;");

        BotonVolver btnVolver = new BotonVolver(() -> GestorVistas.navegarA(pantallaInicio));

        Button btnRefrescar = new Button("↻ Refrescar Cartelera");
        btnRefrescar.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnRefrescar.setStyle("-fx-cursor: hand;");
        btnRefrescar.setOnAction(e -> cargarFunciones());

        header.getChildren().addAll(btnVolver, btnRefrescar, titulo);
        setTop(header);

        // Controles de filtrado
        HBox filtrosBox = new HBox(15);
        filtrosBox.setAlignment(Pos.CENTER_LEFT);
        filtrosBox.setPadding(new Insets(10, 30, 0, 30));
        
        Label lblFecha = new Label("Fecha de reserva:");
        lblFecha.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        
        datePicker = new DatePicker(LocalDate.now());
        datePicker.setEditable(false); // Prevenir que el usuario escriba una fecha anterior manualmente
        // Bloquear fechas anteriores a hoy
        Callback<DatePicker, DateCell> dayCellFactory = dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (item.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #ffc0cb;"); // Color rosado tenue para deshabilitados
                }
            }
        };
        datePicker.setDayCellFactory(dayCellFactory);
        datePicker.setOnAction(e -> cargarFunciones());
        
        filtrosBox.getChildren().addAll(lblFecha, datePicker);

        // Contenedor principal de botones y grupos
        boxFunciones = new VBox(20);
        boxFunciones.setAlignment(Pos.TOP_LEFT);
        boxFunciones.setPadding(new Insets(0, 0, 20, 0));
        
        ScrollPane scroll = new ScrollPane(boxFunciones);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setPadding(new Insets(20, 30, 30, 30));
        
        VBox centerBox = new VBox(15, filtrosBox, scroll);
        setCenter(centerBox);

        // Cargar funciones al iniciar
        cargarFunciones();
    }

    private void cargarFunciones() {
        boxFunciones.getChildren().clear();
        Label loading = new Label("Cargando funciones disponibles...");
        loading.setStyle("-fx-text-fill: #aaa; -fx-font-size: 16px;");
        boxFunciones.getChildren().add(loading);

        new Thread(() -> {
            try {
                String fechaSel = datePicker.getValue() != null ? datePicker.getValue().toString() : LocalDate.now().toString();
                String jsonStr = cliente.listarFunciones(fechaSel);
                Platform.runLater(() -> {
                    this.lastJsonStr = jsonStr;
                    renderizarBotones(jsonStr);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    boxFunciones.getChildren().clear();
                    Label err = new Label("Error de red: " + e.getMessage());
                    err.setStyle("-fx-text-fill: red;");
                    boxFunciones.getChildren().add(err);
                });
            }
        }).start();
    }

    private void renderizarBotones(String jsonStr) {
        boxFunciones.getChildren().clear();

        if (jsonStr == null || jsonStr.trim().isEmpty() || jsonStr.equals("[]")) {
            Label empty = new Label("No hay funciones programadas en este momento.");
            empty.setStyle("-fx-text-fill: #aaa; -fx-font-size: 16px;");
            boxFunciones.getChildren().add(empty);
            return;
        }

        java.util.Map<String, java.util.List<FuncionViewData>> porPelicula = new java.util.HashMap<>();

        String[] arr = jsonStr.replaceAll("^\\[|\\]$", "").split("\\},\\{");
        for (String s : arr) {
            s = s.replace("{", "").replace("}", "");
            FuncionViewData f = new FuncionViewData();
            f.id = extractField(s, "id");
            f.peli = extractField(s, "pelicula");
            if (f.peli == null) f.peli = "Desconocida";
            f.sala = extractField(s, "sala");
            
            String horaStr = extractField(s, "hora");
            if (horaStr != null && horaStr.contains("T")) {
                f.horaStr = horaStr.split("T")[1].substring(0, 5); // Ej: 18:00
            } else {
                f.horaStr = horaStr;
            }
            
            String libresStr = extractField(s, "libres");
            String totalStr = extractField(s, "total");
            f.terminadaStr = extractField(s, "terminada");
            
            f.libres = libresStr != null && !libresStr.isEmpty() ? Integer.parseInt(libresStr) : 0;
            f.total = totalStr != null && !totalStr.isEmpty() ? Integer.parseInt(totalStr) : 0;

            porPelicula.computeIfAbsent(f.peli, k -> new java.util.ArrayList<>()).add(f);
        }

        java.util.List<String> peliculasOrdenadas = new java.util.ArrayList<>(porPelicula.keySet());
        java.util.Collections.sort(peliculasOrdenadas);

        LocalDate fechaSeleccionada = datePicker.getValue();

        for (String peli : peliculasOrdenadas) {
            Label lblPeli = new Label(peli);
            lblPeli.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

            FlowPane flowPeli = new FlowPane();
            flowPeli.setHgap(15);
            flowPeli.setVgap(15);
            flowPeli.setAlignment(Pos.CENTER_LEFT);

            java.util.List<FuncionViewData> funcList = porPelicula.get(peli);
            java.util.Collections.sort(funcList);

            for (FuncionViewData f : funcList) {
                boolean agotado = f.total > 0 && f.libres == 0;
                boolean cerrado = fechaSeleccionada.equals(LocalDate.now()) && "true".equals(f.terminadaStr);
                
                String subtitulo = "Sala: " + (f.sala != null ? f.sala : "") + "\nHora: " + (f.horaStr != null ? f.horaStr : "") + "\nLibres: " + f.libres + "/" + f.total;

                TarjetaFuncion card = new TarjetaFuncion(
                    f.peli,
                    subtitulo,
                    agotado,
                    cerrado,
                    () -> {
                        if (f.id != null) {
                            GestorVistas.navegarA(new PanelVentaButacas(cliente, this, f.id, fechaSeleccionada, f.peli, f.sala, f.horaStr));
                        }
                    }
                );
                flowPeli.getChildren().add(card);
            }

            VBox seccionPelicula = new VBox(15, lblPeli, flowPeli);
            boxFunciones.getChildren().add(seccionPelicula);
        }
    }

    private static class FuncionViewData implements Comparable<FuncionViewData> {
        String id;
        String peli;
        String sala;
        String horaStr;
        int libres;
        int total;
        String terminadaStr;

        @Override
        public int compareTo(FuncionViewData o) {
            if (this.horaStr == null && o.horaStr == null) return 0;
            if (this.horaStr == null) return -1;
            if (o.horaStr == null) return 1;
            return this.horaStr.compareTo(o.horaStr);
        }
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
}
