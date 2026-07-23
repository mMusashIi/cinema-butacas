package com.cine.cliente.ui.componentes.atomos;

import atlantafx.base.theme.Styles;
import javafx.scene.control.Button;
import javafx.scene.Cursor;

public class BotonVolver extends Button {
    public BotonVolver(Runnable onAction) {
        super("← Volver");
        setCursor(Cursor.HAND);
        getStyleClass().add(Styles.BUTTON_OUTLINED);
        setOnAction(e -> onAction.run());
    }
}
