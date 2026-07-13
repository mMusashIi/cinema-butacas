package com.cine.constructor;

import com.cine.dominio.Cine;
import com.cine.dominio.SalaCine;
import com.cine.dominio.TipoButaca;

/**
 * Constructor de salas de cine con API fluida (constructor pattern).
 * Genera un SalaCine con la geometría inicial completa —
 * todas las celdas ocupadas por Butaca de tipo NORMAL en estado FREE.
 *
 * Para ajustar la sala después (huecos, tipos especiales de butaca),
 * usar EditorSala sobre el SalaCine generado.
 *
 * Regla de numeración garantizada:
 * Cada Butaca generado tiene number = columnIndex + 1.
 * Los huecos nunca afectan la numeración — son responsabilidad de EditorSala.
 *
 * Uso:
 *   SalaCine sala = new ConstructorSala()
 *       .name("Sala 1")
 *       .cinema(miCine)
 *       .rows(10)
 *       .columns(15)
 *       .build();
 */
public class ConstructorSala {

    private String name;
    private Cine cinema;
    private int rows;
    private int columns;
    private TipoButaca defaultSeatType = TipoButaca.NORMAL; // Tipo inicial por defecto

    public ConstructorSala name(String name) {
        this.name = name;
        return this;
    }

    public ConstructorSala cinema(Cine cinema) {
        this.cinema = cinema;
        return this;
    }

    public ConstructorSala rows(int rows) {
        this.rows = rows;
        return this;
    }

    public ConstructorSala columns(int columns) {
        this.columns = columns;
        return this;
    }

    /**
     * Permite cambiar el tipo de butaca inicial para toda la sala
     * (por ejemplo, si se construye una sala VIP completa de entrada).
     * El valor por defecto es TipoButaca.NORMAL.
     */
    public ConstructorSala defaultSeatType(TipoButaca tipo) {
        if (tipo == null) throw new IllegalArgumentException("defaultSeatType no puede ser nulo");
        this.defaultSeatType = tipo;
        return this;
    }

    /**
     * Valida los parámetros y construye el SalaCine.
     * Si algún parámetro obligatorio no fue configurado, lanza un error descriptivo.
     */
    public SalaCine build() {
        validate();
        SalaCine sala = new SalaCine(name, cinema, rows, columns);

        // Si el tipo por defecto no es NORMAL, recorremos la sala y lo aplicamos
        if (defaultSeatType != TipoButaca.NORMAL) {
            applyDefaultType(sala);
        }
        return sala;
    }

    // --- Validación interna ---

    private void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalStateException("ConstructorSala: falta configurar el nombre de la sala (.name(...))");
        }
        if (cinema == null) {
            throw new IllegalStateException("ConstructorSala: falta configurar el local (.cinema(...))");
        }
        if (rows <= 0) {
            throw new IllegalStateException("ConstructorSala: falta configurar las filas (.rows(...)) con un valor positivo");
        }
        if (columns <= 0) {
            throw new IllegalStateException("ConstructorSala: falta configurar las columnas (.columns(...)) con un valor positivo");
        }
    }

    /**
     * Aplica el tipo de butaca por defecto a todas las celdas ya creadas.
     * Se usa cuando el tipo inicial no es NORMAL.
     * La numeración original (columnIndex + 1) se conserva intacta.
     */
    private void applyDefaultType(SalaCine sala) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                var butaca = sala.getButaca(r, c);
                if (butaca != null) {
                    // Recrear la butaca con el tipo correcto manteniendo fila y número
                    sala.replaceSeat(r, c,
                        new com.cine.dominio.Butaca(butaca.getFila(), butaca.getNumero(), defaultSeatType));
                }
            }
        }
    }
}
