package com.cine.cliente.ui.componentes.moleculas;

import com.cine.cliente.ui.componentes.atomos.BotonButaca;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * Molécula: Un ítem de leyenda con ícono de sillón SVG + etiqueta de texto.
 *
 * En vez de un cuadrado de color genérico, usa la misma forma de sillón
 * que BotonButaca para que la leyenda sea 100% fiel a la UI real.
 */
public class ItemLeyenda extends HBox {

    /**
     * @param text        Texto descriptivo del estado
     * @param bgStyle     Estilo CSS de fondo para el ícono (ej. "-fx-background-color: #2ecc71;")
     * @param effectStyle Efecto adicional (ej. glow para SELECTED), puede ser ""
     */
    public ItemLeyenda(String text, String bgStyle, String effectStyle) {
        setSpacing(8);
        setAlignment(Pos.CENTER_LEFT);

        // Ícono de sillón (mismo SVG que BotonButaca, sin texto)
        Button icon = new Button();
        icon.setPrefSize(24, 24);
        icon.setMinSize(24, 24);
        icon.setMaxSize(24, 24);
        icon.setMouseTransparent(true); // no interactivo
        icon.setStyle(
            "-fx-shape: \"" + BotonButaca.SVG_ARMCHAIR + "\"; "
            + "-fx-scale-shape: true; "
            + bgStyle
            + effectStyle
        );

        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12px;");

        getChildren().addAll(icon, label);
    }

    /** Ícono de color pleno (FREE normal, VIP, BOOKED, BROKEN) */
    public static ItemLeyenda solid(String text, String color) {
        return new ItemLeyenda(text, "-fx-background-color: " + color + "; ", "");
    }

    /** Ícono con borde (accesible: verde + borde azul) */
    public static ItemLeyenda bordered(String text, String fill, String border) {
        return new ItemLeyenda(text,
            "-fx-background-color: " + border + ", " + fill + "; -fx-background-insets: 0, 2; ",
            "");
    }

    /** Ícono con borde amarillo (SELECTED: mantiene color base + borde) */
    public static ItemLeyenda withBorder(String text, String baseColor) {
        return new ItemLeyenda(text,
            "-fx-background-color: #f1c40f, " + baseColor + "; -fx-background-insets: 0, 2; ",
            "");
    }

    /** Ícono con borde azul + borde amarillo (accesible seleccionado) */
    public static ItemLeyenda borderedSelected(String text, String fill, String innerBorder) {
        return new ItemLeyenda(text,
            "-fx-background-color: #f1c40f, " + innerBorder + ", " + fill + "; -fx-background-insets: 0, 2, 4; ",
            "");
    }
}
