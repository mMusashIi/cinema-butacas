package com.cine.cliente.ui.componentes.moleculas;

import com.cine.dominio.MetodoPago;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.geometry.Insets;

public class DialogSeleccionPago {
    public static class ResultadoPago {
        public final MetodoPago metodo;
        public final String dni;
        public ResultadoPago(MetodoPago m, String d) { this.metodo = m; this.dni = d; }
    }

    public static Optional<ResultadoPago> mostrar() {
        Dialog<ResultadoPago> dialog = new Dialog<>();
        dialog.setTitle("Metodo de Pago");
        dialog.setHeaderText("Complete los datos de la reserva");

        ButtonType btnAceptar = new ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnAceptar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtDni = new TextField();
        txtDni.setPromptText("Opcional");
        
        ComboBox<MetodoPago> cbMetodo = new ComboBox<>();
        cbMetodo.getItems().addAll(MetodoPago.TARJETA, MetodoPago.EFECTIVO);
        cbMetodo.setValue(MetodoPago.TARJETA);

        grid.add(new Label("DNI:"), 0, 0);
        grid.add(txtDni, 1, 0);
        grid.add(new Label("Método:"), 0, 1);
        grid.add(cbMetodo, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnAceptar) {
                String dniStr = txtDni.getText().trim();
                if (dniStr.isEmpty()) dniStr = "noDNI";
                return new ResultadoPago(cbMetodo.getValue(), dniStr);
            }
            return null;
        });

        return dialog.showAndWait();
    }
}
