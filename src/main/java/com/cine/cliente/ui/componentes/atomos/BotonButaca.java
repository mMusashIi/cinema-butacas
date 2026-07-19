package com.cine.cliente.ui.componentes.atomos;

import atlantafx.base.theme.Styles;
import com.cine.dominio.Butaca;
import com.cine.dominio.EstadoButaca;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

import java.util.function.Consumer;

/**
 * Componente Atómico: Representa visualmente una butaca de cine.
 *
 * DISEÑO DE ESTADOS:
 * ─────────────────────────────────────────────────────────────────────────
 * • FREE      → Color pleno (verde, morado para VIP, verde con borde azul accesible)
 * • SELECTED  → Color base original + borde/glow amarillo superpuesto
 *               (el color no cambia, solo se agrega el "aro" de selección)
 * • BOOKED    → Gris oscuro (apagado)
 * • BROKEN    → Rojo
 * • LOCKED    → Naranja (bloqueado por otro cliente, estado de red)
 */
public class BotonButaca extends Button {

    public static final String SVG_ARMCHAIR =
        "M6,3 C4.89,3 4,3.89 4,5 V10 H3 C1.89,10 1,10.89 1,12 V17 C1,18.1 1.89,19 3,19 H4 V21 H6 V19 H18 V21 H20 V19 H21 C22.1,19 23,18.1 23,17 V12 C23,10.89 22.11,10 21,10 H20 V5 C20,3.89 19.1,3 18,3 H6 Z";

    private final Butaca butaca;
    private final Consumer<Butaca> onSeatClicked;

    /** Estado de red: true = bloqueado por OTRO cliente */
    private boolean lockedByOther = false;

    public BotonButaca(Butaca butaca, Consumer<Butaca> onSeatClicked) {
        super("");
        this.butaca = butaca;
        this.onSeatClicked = onSeatClicked;

        setPrefSize(40, 40);
        setMinSize(40, 40);
        setMaxSize(40, 40);
        setId("butaca-" + butaca.getId());
        setOnAction(e -> handleAction());

        actualizarVisuales();
    }

    private void handleAction() {
        if (!lockedByOther
                && butaca.getEstado() != EstadoButaca.BOOKED
                && butaca.getEstado() != EstadoButaca.BROKEN) {
            onSeatClicked.accept(butaca);
        }
    }

    public void setLockedByOther(boolean locked) {
        this.lockedByOther = locked;
        actualizarVisuales();
    }

    public boolean isLockedByOther() { return lockedByOther; }

    /**
     * Devuelve el estilo de fondo de la butaca. Si está seleccionada,
     * agrega un borde amarillo usando insets.
     */
    private String getBackgroundStyle(boolean selected) {
        String tipo = butaca.getTipo().code();
        
        if (selected) {
            if ("SILLA_RUEDAS".equals(tipo)) {
                // Borde amarillo, borde interior azul, centro verde
                return "-fx-background-color: #f1c40f, #3498db, #2ecc71; -fx-background-insets: 0, 2, 4; ";
            } else {
                String color = "VIP".equals(tipo) ? "#9b59b6" : "#2ecc71";
                // Borde amarillo, centro color base
                return "-fx-background-color: #f1c40f, " + color + "; -fx-background-insets: 0, 2; ";
            }
        } else {
            if ("SILLA_RUEDAS".equals(tipo)) {
                return "-fx-background-color: #3498db, #2ecc71; -fx-background-insets: 0, 2; ";
            } else {
                String color = "VIP".equals(tipo) ? "#9b59b6" : "#2ecc71";
                return "-fx-background-color: " + color + "; ";
            }
        }
    }

    public void actualizarVisuales() {
        getStyleClass().removeAll(Styles.SUCCESS, Styles.WARNING, Styles.DANGER, Styles.ACCENT);

        // Base compartida: forma SVG de sillón
        // Se usa font-size 9px y padding 0 para evitar que nombres largos como E10 o F11 se trunquen con (...)
        String shapeStyle = "-fx-shape: \"" + SVG_ARMCHAIR + "\"; -fx-scale-shape: true; "
                + "-fx-font-size: 9px; -fx-padding: 0; -fx-font-weight: bold; -fx-cursor: hand; ";

        // Estado de red tiene prioridad
        if (lockedByOther) {
            setStyle(shapeStyle
                    + "-fx-background-color: #e67e22; "
                    + "-fx-text-fill: #111111; -fx-cursor: default;");
            setTooltip(new Tooltip(butaca.getFila() + butaca.getNumero() + " — Bloqueado temporalmente por otro usuario"));
            return;
        }

        switch (butaca.getEstado()) {
            case FREE ->
                setStyle(shapeStyle + getBackgroundStyle(false) + "-fx-text-fill: #111111;");

            case SELECTED ->
                setStyle(shapeStyle + getBackgroundStyle(true) + "-fx-text-fill: #111111;");

            case BOOKED ->
                setStyle(shapeStyle
                        + "-fx-background-color: #f1c40f; "
                        + "-fx-text-fill: #111111; -fx-cursor: default;");

            case BROKEN ->
                setStyle(shapeStyle
                        + "-fx-background-color: #e74c3c; -fx-text-fill: #ffffff;");
        }

        setTooltip(new Tooltip(
                butaca.getFila() + butaca.getNumero() + " — " + butaca.getTipo().displayName()
                + "\nEstado: " + butaca.getEstado().name()));
    }

    public Butaca getButaca() { return butaca; }
}
