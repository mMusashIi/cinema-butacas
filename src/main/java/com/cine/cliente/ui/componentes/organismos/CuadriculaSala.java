package com.cine.cliente.ui.componentes.organismos;

import com.cine.cliente.ui.componentes.atomos.BotonButacaSvg;
import com.cine.cliente.ui.componentes.atomos.EtiquetaCoordenada;
import com.cine.cliente.ui.componentes.moleculas.IndicadorPantalla;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import java.util.function.BiConsumer;

public class CuadriculaSala extends GridPane {
    
    private BiConsumer<Integer, Integer> onCellClick;
    private BotonButacaSvg[][] botones;

    public CuadriculaSala() {
        setAlignment(Pos.CENTER);
        setHgap(4);
        setVgap(4);
        setPadding(new Insets(20));
    }

    public void setOnCellClick(BiConsumer<Integer, Integer> onCellClick) {
        this.onCellClick = onCellClick;
    }

    public BotonButacaSvg getBotonAt(int fila, int columna) {
        if (botones != null && fila >= 0 && fila < botones.length && columna >= 0 && columna < botones[0].length) {
            return botones[fila][columna];
        }
        return null;
    }

    public void renderizarMatriz(String[][] estados, String[][] tooltips) {
        getChildren().clear();
        if (estados == null || estados.length == 0) return;

        int filasActuales = estados.length;
        int columnasActuales = estados[0].length;
        this.botones = new BotonButacaSvg[filasActuales][columnasActuales];

        IndicadorPantalla pantalla = new IndicadorPantalla();
        add(pantalla, 0, 0, columnasActuales + 1, 1);

        boolean[] colVacia = new boolean[columnasActuales];
        for (int c = 0; c < columnasActuales; c++) {
            boolean vacia = true;
            for (int r = 0; r < filasActuales; r++) {
                if (!"PASILLO".equals(estados[r][c]) && !"NULO".equals(estados[r][c])) vacia = false;
            }
            colVacia[c] = vacia;
        }

        int colNum = 1;
        for (int c = 0; c < columnasActuales; c++) {
            if (colVacia[c]) {
                Region spacer = new Region();
                spacer.setMinSize(10, 10);
                spacer.setPrefWidth(40);
                add(spacer, c + 1, 1);
            } else {
                EtiquetaCoordenada colLabel = new EtiquetaCoordenada(String.valueOf(colNum++), false);
                add(colLabel, c + 1, 1);
            }
        }

        int renderRow = 2; 
        int rowLetra = 0;

        for (int i = 0; i < filasActuales; i++) {
            boolean filaVacia = true;
            for (int c = 0; c < columnasActuales; c++) {
                if (!"PASILLO".equals(estados[i][c]) && !"NULO".equals(estados[i][c])) filaVacia = false;
            }

            if (!filaVacia) {
                char rowLetter = (char) ('A' + rowLetra++);
                EtiquetaCoordenada rowLabel = new EtiquetaCoordenada(String.valueOf(rowLetter), true);
                add(rowLabel, 0, renderRow);
            } else {
                Region spacer = new Region();
                spacer.setMinSize(10, 10);
                spacer.setMinHeight(40);
                add(spacer, 0, renderRow);
            }

            for (int j = 0; j < columnasActuales; j++) {
                final int f = i;
                final int c = j;
                
                String estado = estados[f][c];
                String tip = (tooltips != null && tooltips.length > f && tooltips[f].length > c) ? tooltips[f][c] : null;
                
                BotonButacaSvg cell = new BotonButacaSvg(estado, tip);
                botones[f][c] = cell;
                
                cell.setOnAction(e -> {
                    if (onCellClick != null) {
                        onCellClick.accept(f, c);
                    }
                });

                add(cell, c + 1, renderRow);
            }
            renderRow++;
        }
    }
}
