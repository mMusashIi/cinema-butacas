package com.cine.cliente.ui.componentes.moleculas;

import atlantafx.base.theme.Styles;
import com.cine.dominio.Butaca;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.function.Consumer;

/**
 * Componente Molécula: Representa una fila en el panel de selección, 
 * mostrando la butaca, su tipo y precio, y un botón para removerla.
 */
public class FilaButacaSeleccionada extends HBox {

    public FilaButacaSeleccionada(Butaca butaca, double price, Consumer<Butaca> onRemoveClicked) {
        setSpacing(10);
        setAlignment(Pos.CENTER_LEFT);

        Label idLabel = new Label(butaca.getId());
        idLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label descLabel = new Label(String.format("(%s) - $%.2f", butaca.getTipo().displayName(), price));
        descLabel.setStyle("-fx-text-fill: #cccccc;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button removeBtn = new Button();
        removeBtn.setText("X"); // Fallback a texto normal
        removeBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.DANGER, Styles.FLAT);
        removeBtn.setOnAction(e -> onRemoveClicked.accept(butaca));

        getChildren().addAll(idLabel, descLabel, spacer, removeBtn);
    }
}
