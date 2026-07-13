package com.cine.cliente.ui.componentes.organismos;

import com.cine.dominio.Pelicula;
import com.cine.dominio.Funcion;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Componente Organismo: Cabecera que muestra la información de la película y el tiempo de sesión.
 */
public class PanelCabecera extends HBox {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE d MMM yyyy HH:mm", new Locale("es", "ES"));
    
    private Label timerLabel;

    public PanelCabecera(Funcion funcion) {
        setPadding(new Insets(20, 30, 20, 30));
        setStyle("-fx-background-color: #121212; -fx-border-color: #222; -fx-border-width: 0 0 1 0;");
        setAlignment(Pos.CENTER_LEFT);

        Pelicula pelicula = funcion.getPelicula();

        VBox infoBox = new VBox(5);
        Label titleLabel = new Label(pelicula.getTitulo());
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #e0e0e0;");

        String subtitle = String.format("%s · Función %s · %s · Precio base: $%.2f",
                funcion.getSala().getNombre(),
                funcion.getFormat(),
                funcion.getStartTime().format(DATE_FMT),
                funcion.getPrecioBase());

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 13px;");

        infoBox.getChildren().addAll(titleLabel, subtitleLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox timerBox = new VBox(2);
        timerBox.setAlignment(Pos.CENTER_RIGHT);
        
        Label timerTitle = new Label("TIEMPO RESTANTE");
        timerTitle.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px; -fx-font-weight: bold;");
        
        timerLabel = new Label("05:00");
        timerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2ecc71;");
        
        timerBox.getChildren().addAll(timerTitle, timerLabel);

        getChildren().addAll(infoBox, spacer, timerBox);
    }

    public void actualizarTemporizador(long secondsRemaining) {
        long m = secondsRemaining / 60;
        long s = secondsRemaining % 60;
        timerLabel.setText(String.format("%02d:%02d", m, s));
        if (secondsRemaining <= 60) {
            timerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;"); // Rojo al final
        } else {
            timerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2ecc71;");
        }
    }
}
