package com.cine.cliente.ui.componentes.organismos;

import atlantafx.base.theme.Styles;
import com.cine.cliente.ui.componentes.moleculas.FilaButacaSeleccionada;
import com.cine.dominio.Butaca;
import com.cine.dominio.Funcion;
import com.cine.dominio.precios.MotorPrecios;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * Componente Organismo: Panel lateral derecho con la lista de butacas
 * seleccionadas, entrada de DNI y acciones de confirmación.
 */
public class PanelSeleccion extends VBox {

    private final VBox selectedListContainer;
    private final Label totalLabel;
    private final TextField dniField;
    private final Button confirmBtn;
    private final Button cancelBtn;

    private final Funcion currentShowtime;
    private final MotorPrecios pricingEngine;
    private final Consumer<Butaca> onRemoveSeat;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    public PanelSeleccion(Funcion funcion, MotorPrecios pricingEngine, Consumer<Butaca> onRemoveSeat, Runnable onConfirm, Runnable onCancel) {
        this.currentShowtime = funcion;
        this.pricingEngine = pricingEngine;
        this.onRemoveSeat = onRemoveSeat;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;

        setPrefWidth(350);
        setPadding(new Insets(30));
        setSpacing(20);
        setStyle("-fx-background-color: #1a1a24; -fx-border-color: #222; -fx-border-width: 0 0 0 1;");

        Label sectionTitle = new Label("SELECCIÓN ACTUAL");
        sectionTitle.setStyle("-fx-text-fill: #888888; -fx-font-size: 12px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        selectedListContainer = new VBox(10);
        Label emptyLabel = new Label("Ninguna butaca seleccionada");
        emptyLabel.setStyle("-fx-text-fill: #555555; -fx-font-style: italic;");
        selectedListContainer.getChildren().add(emptyLabel);

        ScrollPane scrollSelected = new ScrollPane(selectedListContainer);
        scrollSelected.setFitToWidth(true);
        scrollSelected.setPrefViewportHeight(200);
        scrollSelected.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        totalLabel = new Label("Total: $ 0.00");
        totalLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Label dniLabel = new Label("DNI / ID comprador (opcional)");
        dniLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");
        
        dniField = new TextField();
        dniField.setPromptText("Sin DNI → boleta anónima");
        
        VBox dniBox = new VBox(5, dniLabel, dniField);

        confirmBtn = new Button("Confirmar compra y generar Boleto");
        confirmBtn.setMaxWidth(Double.MAX_VALUE);
        confirmBtn.getStyleClass().add(Styles.ACCENT);
        confirmBtn.setDisable(true);
        confirmBtn.setOnAction(e -> onConfirm.run());

        cancelBtn = new Button("Cancelar selección");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.getStyleClass().add(Styles.DANGER);
        cancelBtn.setDisable(true);
        cancelBtn.setOnAction(e -> onCancel.run());

        getChildren().addAll(sectionTitle, scrollSelected, totalLabel, dniBox, confirmBtn, cancelBtn);
    }

    public void updateSelection(List<Butaca> selectedSeats) {
        selectedListContainer.getChildren().clear();
        double total = 0.0;

        if (selectedSeats.isEmpty()) {
            Label emptyLabel = new Label("Ninguna butaca seleccionada");
            emptyLabel.setStyle("-fx-text-fill: #555555; -fx-font-style: italic;");
            selectedListContainer.getChildren().add(emptyLabel);
        } else {
            for (Butaca s : selectedSeats) {
                double price = pricingEngine.calcularPrecio(s, currentShowtime);
                total += price;
                FilaButacaSeleccionada row = new FilaButacaSeleccionada(s, price, onRemoveSeat);
                selectedListContainer.getChildren().add(row);
            }
        }

        totalLabel.setText(String.format("Total: $ %.2f", total));
        
        boolean hasSelection = !selectedSeats.isEmpty();
        confirmBtn.setDisable(!hasSelection);
        cancelBtn.setDisable(!hasSelection);
    }

    public String getDniInput() {
        return dniField.getText();
    }

    public void clearDniInput() {
        dniField.clear();
    }
}
