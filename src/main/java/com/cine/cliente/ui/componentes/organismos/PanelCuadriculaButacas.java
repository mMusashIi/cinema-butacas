package com.cine.cliente.ui.componentes.organismos;

import com.cine.cliente.ui.componentes.atomos.BotonButaca;
import com.cine.cliente.ui.componentes.moleculas.ItemLeyenda;
import com.cine.compartido.Protocolo;
import com.cine.dominio.SalaCine;
import com.cine.dominio.Butaca;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Componente Organismo: Contiene la grilla de butacas, la leyenda de colores
 * y la lógica visual de distribución (pasillos, números, letras).
 */
public class PanelCuadriculaButacas extends VBox {

    private final Map<String, BotonButaca> seatButtons = new HashMap<>();
    private final Consumer<Butaca> onSeatClicked;

    public PanelCuadriculaButacas(SalaCine sala, Consumer<Butaca> onSeatClicked) {
        this.onSeatClicked = onSeatClicked;
        
        setPadding(new Insets(30));
        setSpacing(40);
        setAlignment(Pos.TOP_CENTER);
        // Expande para llenar el espacio
        javafx.scene.layout.VBox.setVgrow(this, javafx.scene.layout.Priority.ALWAYS);

        // 1. Leyenda con íconos SVG de sillón
        HBox legend = new HBox(
                ItemLeyenda.solid   ("Normal libre",       "#2ecc71"),
                ItemLeyenda.solid   ("VIP libre",          "#9b59b6"),
                ItemLeyenda.bordered("Accesible",          "#2ecc71", "#3498db"),
                ItemLeyenda.withBorder("Seleccionada",     "#2ecc71"),
                ItemLeyenda.solid   ("Reservada",          "#111111"),
                ItemLeyenda.solid   ("Bloqueada (otro)",   "#e67e22"),
                ItemLeyenda.solid   ("Fuera de servicio",  "#e74c3c")
        );
        legend.setSpacing(25);
        legend.setAlignment(Pos.CENTER);

        // 2. Grilla de butacas
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setAlignment(Pos.CENTER);

        // Pantalla (indicador superior)
        Label screenLabel = new Label("P A N T A L L A");
        screenLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 10px; -fx-letter-spacing: 5px;");
        screenLabel.setAlignment(Pos.CENTER);
        screenLabel.setMaxWidth(Double.MAX_VALUE);
        
        VBox screenBox = new VBox(screenLabel);
        screenBox.setAlignment(Pos.CENTER);
        screenBox.setStyle("-fx-border-color: #333; -fx-border-width: 0 0 1 0; -fx-padding: 0 0 10 0;");
        screenBox.setMaxWidth(600);

        // Pre-calcular cuáles columnas son completamente nulas
        boolean[] columnaVacia = new boolean[sala.getTotalColumns()];
        for (int c = 0; c < sala.getTotalColumns(); c++) {
            boolean todosNull = true;
            for (int r = 0; r < sala.getTotalRows(); r++) {
                if (sala.getButaca(r, c) != null) { todosNull = false; break; }
            }
            columnaVacia[c] = todosNull;
        }

        // Numeración de columnas — solo las que tienen al menos una butaca
        int colNumVisible = 1;
        for (int c = 0; c < sala.getTotalColumns(); c++) {
            if (columnaVacia[c]) {
                // Columna vacía: insertar espacio visual en la fila de números
                javafx.scene.layout.Region colSpacer = new javafx.scene.layout.Region();
                colSpacer.setMinWidth(40);
                colSpacer.setPrefWidth(40);
                grid.add(colSpacer, c + 1, 0);
                continue;
            }
            Label colLabel = new Label(String.valueOf(colNumVisible++));
            colLabel.setStyle("-fx-text-fill: #777; -fx-font-weight: bold;");
            colLabel.setPrefWidth(40);
            colLabel.setAlignment(Pos.CENTER);
            grid.add(colLabel, c + 1, 0);
        }

        // Renderizar matriz
        int renderRowIndex = 1; // Fila 0 es para los números de columna
        int letraFila = 0;      // Contador de letras de fila visibles (A, B, C…)

        for (int r = 0; r < sala.getTotalRows(); r++) {

            // Detectar si toda la fila es null (pasillo/hueco completo)
            boolean filaVacia = true;
            for (int c = 0; c < sala.getTotalColumns(); c++) {
                if (sala.getButaca(r, c) != null) { filaVacia = false; break; }
            }

            if (filaVacia) {
                // Separador visual de pasillo: misma medida que el spacer de columna (40px)
                javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                spacer.setMinHeight(40);
                grid.add(spacer, 0, renderRowIndex, sala.getTotalColumns() + 1, 1);
                renderRowIndex++;
                continue; // No incrementar letraFila — la letra se corre
            }

            // Etiqueta de fila: solo filas con al menos una butaca
            char rowLetter = (char) ('A' + letraFila++);
            Label rowLabel = new Label(String.valueOf(rowLetter));
            rowLabel.setStyle("-fx-text-fill: #5c7cfa; -fx-font-weight: bold;");
            rowLabel.setPrefWidth(30);
            rowLabel.setAlignment(Pos.CENTER_RIGHT);
            grid.add(rowLabel, 0, renderRowIndex);

            for (int c = 0; c < sala.getTotalColumns(); c++) {
                Butaca butaca = sala.getButaca(r, c);
                if (butaca != null) {
                    BotonButaca btn = new BotonButaca(butaca, this::manejarClicButaca);
                    seatButtons.put(butaca.getId(), btn);
                    grid.add(btn, c + 1, renderRowIndex);
                }
                // Celda individual null → espacio vacío, sin botón
            }
            renderRowIndex++;
        }

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox gridContainer = new VBox(20, screenBox, scroll);
        gridContainer.setAlignment(Pos.TOP_CENTER);
        javafx.scene.layout.VBox.setVgrow(gridContainer, javafx.scene.layout.Priority.ALWAYS);

        getChildren().addAll(legend, gridContainer);
    }

    private void manejarClicButaca(Butaca butaca) {
        onSeatClicked.accept(butaca);
    }

    public void refreshAllSeats() {
        seatButtons.values().forEach(BotonButaca::actualizarVisuales);
    }

    /**
     * Actualiza una butaca con un estado de red (LOCKED, FREE, BOOKED).
     * Llamado desde el hilo de UI por ClienteRed al recibir ACTUALIZACION_BUTACA.
     *
     * @param seatId    ID de la butaca (ej. "B3")
     * @param netStatus Estado de red: "LOCKED", "FREE", "SELECTED", "BOOKED"
     * @param mySeatId  ID de las butacas que YO tengo seleccionadas
     *                  (para no marcarlas como locked si las tengo yo)
     */
    public void manejarActualizacionRed(String seatId, String netStatus,
                                    java.util.Set<String> mySelected) {
        BotonButaca btn = seatButtons.get(seatId);
        if (btn == null) return;

        if (Protocolo.NET_LOCKED.equals(netStatus) && !mySelected.contains(seatId)) {
            btn.setLockedByOther(true);
        } else {
            btn.setLockedByOther(false);
            btn.actualizarVisuales();
        }
    }

    public BotonButaca getSeatButton(String seatId) {
        return seatButtons.get(seatId);
    }
}
