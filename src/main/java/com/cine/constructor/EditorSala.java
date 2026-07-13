package com.cine.constructor;

import com.cine.dominio.SalaCine;
import com.cine.dominio.Butaca;
import com.cine.dominio.EstadoButaca;
import com.cine.dominio.TipoButaca;

/**
 * Editor de salas de cine ya construidas.
 * Permite ajustar la geometría y los tipos de butaca de un SalaCine
 * después de haberlo creado con ConstructorSala.
 *
 * Responsabilidades:
 *   - Crear huecos (pasillos, escaleras, espacios vacíos) → clearCell / clearRow
 *   - Cambiar el tipo de una butaca o zona → setSeatType / setZoneType
 *
 * Regla CRÍTICA que este editor garantiza:
 *   Al crear un hueco, NUNCA se renumeran las butacas vecinas.
 *   La butaca A8 sigue siendo A8 aunque se eliminen A4, A5, A6, A7.
 *   Esto es intencional: preserva la integridad de boletas ya emitidas.
 *
 * Restricciones de seguridad:
 *   - Solo se puede editar una sala si NO tiene butacas en estado BOOKED.
 *     Editar una sala con reservas activas podría invalidar boletas ya vendidas.
 *   - Las butacas SELECTED o BROKEN se pueden editar, pero se emite advertencia.
 */
public class EditorSala {

    private final SalaCine sala;

    public EditorSala(SalaCine sala) {
        if (sala == null) {
            throw new IllegalArgumentException("La sala (sala) no puede ser nula");
        }
        this.sala = sala;
    }

    // --- Operaciones de huecos ---

    /**
     * Convierte la celda [rowIndex][colIndex] en un espacio vacío (null).
     * Si la celda ya era null, no hace nada.
     * NUNCA modifica el número de ninguna otra butaca.
     *
     * @throws IllegalStateException si la celda tiene una butaca BOOKED
     */
    public EditorSala clearCell(int rowIndex, int colIndex) {
        Butaca existing = sala.getButaca(rowIndex, colIndex);
        if (existing == null) return this; // Ya es un hueco, no hay nada que hacer

        if (existing.getEstado() == EstadoButaca.BOOKED) {
            throw new IllegalStateException(
                String.format("No se puede eliminar la butaca %s: tiene una reserva activa (BOOKED)",
                              existing.getId())
            );
        }
        sala.clearCell(rowIndex, colIndex);
        return this;
    }

    /**
     * Convierte todas las celdas de una fila en espacios vacíos.
     * Útil para representar pasillos horizontales completos (escaleras, zona de acceso).
     *
     * @throws IllegalStateException si alguna celda de la fila tiene butaca BOOKED
     */
    public EditorSala clearRow(int rowIndex) {
        // Primero validamos toda la fila antes de modificar nada (todo o nada)
        for (int c = 0; c < sala.getTotalColumns(); c++) {
            Butaca butaca = sala.getButaca(rowIndex, c);
            if (butaca != null && butaca.getEstado() == EstadoButaca.BOOKED) {
                throw new IllegalStateException(
                    String.format("No se puede limpiar la fila %d: la butaca %s tiene reserva activa (BOOKED)",
                                  rowIndex, butaca.getId())
                );
            }
        }
        for (int c = 0; c < sala.getTotalColumns(); c++) {
            sala.clearCell(rowIndex, c);
        }
        return this;
    }

    /**
     * Restaura una celda vacía [rowIndex][colIndex] con una butaca de tipo NORMAL.
     * Útil para deshacer un clearCell.
     * El número de la butaca será columnIndex + 1 (regla de numeración fija).
     *
     * @throws IllegalStateException si la celda ya tiene una butaca
     */
    public EditorSala restoreCell(int rowIndex, int colIndex) {
        return restoreCell(rowIndex, colIndex, TipoButaca.NORMAL);
    }

    /**
     * Restaura una celda vacía con una butaca del tipo especificado.
     *
     * @throws IllegalStateException si la celda ya tiene una butaca
     */
    public EditorSala restoreCell(int rowIndex, int colIndex, TipoButaca tipo) {
        Butaca existing = sala.getButaca(rowIndex, colIndex);
        if (existing != null) {
            throw new IllegalStateException(
                String.format("La celda [%d][%d] ya tiene una butaca (%s). " +
                              "Usa setSeatType() para cambiar el tipo.",
                              rowIndex, colIndex, existing.getId())
            );
        }
        String rowLetter = String.valueOf((char) ('A' + rowIndex));
        int seatNumber = colIndex + 1; // Numeración fija por columna
        sala.replaceSeat(rowIndex, colIndex, new Butaca(rowLetter, seatNumber, tipo));
        return this;
    }

    // --- Operaciones de tipo de butaca ---

    /**
     * Cambia el tipo de una butaca individual.
     * Crea una nueva instancia de Butaca con el mismo id (fila + número) pero distinto tipo.
     *
     * @throws IllegalStateException si la celda está vacía o la butaca está BOOKED
     */
    public EditorSala setSeatType(int rowIndex, int colIndex, TipoButaca newType) {
        if (newType == null) {
            throw new IllegalArgumentException("El nuevo tipo (newType) no puede ser nulo");
        }
        Butaca existing = sala.getButaca(rowIndex, colIndex);
        if (existing == null) {
            throw new IllegalStateException(
                String.format("La celda [%d][%d] está vacía. Usa restoreCell() para crear una butaca ahí.",
                              rowIndex, colIndex)
            );
        }
        if (existing.getEstado() == EstadoButaca.BOOKED) {
            throw new IllegalStateException(
                String.format("No se puede cambiar el tipo de la butaca %s: tiene una reserva activa (BOOKED)",
                              existing.getId())
            );
        }
        // Nueva instancia con misma fila y número, nuevo tipo
        sala.replaceSeat(rowIndex, colIndex, new Butaca(existing.getFila(), existing.getNumero(), newType));
        return this;
    }

    /**
     * Cambia el tipo de todas las butacas en un rango rectangular de celdas.
     * Útil para marcar zonas completas (ej. las últimas 2 filas como VIP).
     * Las celdas vacías (null) dentro del rango se ignoran.
     *
     * @param fromRow    Fila de inicio (inclusive)
     * @param fromCol    Columna de inicio (inclusive)
     * @param toRow      Fila de fin (inclusive)
     * @param toCol      Columna de fin (inclusive)
     * @param newType    Tipo a aplicar en toda la zona
     * @throws IllegalStateException si alguna butaca del rango está BOOKED
     */
    public EditorSala setZoneType(int fromRow, int fromCol, int toRow, int toCol, TipoButaca newType) {
        if (newType == null) {
            throw new IllegalArgumentException("El nuevo tipo (newType) no puede ser nulo");
        }
        validateRange(fromRow, fromCol, toRow, toCol);

        // Validar todo el rango antes de modificar (todo o nada)
        for (int r = fromRow; r <= toRow; r++) {
            for (int c = fromCol; c <= toCol; c++) {
                Butaca butaca = sala.getButaca(r, c);
                if (butaca != null && butaca.getEstado() == EstadoButaca.BOOKED) {
                    throw new IllegalStateException(
                        String.format("No se puede cambiar la zona: la butaca %s tiene reserva activa (BOOKED)",
                                      butaca.getId())
                    );
                }
            }
        }

        // Aplicar el cambio
        for (int r = fromRow; r <= toRow; r++) {
            for (int c = fromCol; c <= toCol; c++) {
                Butaca butaca = sala.getButaca(r, c);
                if (butaca != null) {
                    sala.replaceSeat(r, c, new Butaca(butaca.getFila(), butaca.getNumero(), newType));
                }
            }
        }
        return this;
    }

    // --- Validación interna ---

    private void validateRange(int fromRow, int fromCol, int toRow, int toCol) {
        if (fromRow > toRow || fromCol > toCol) {
            throw new IllegalArgumentException(
                String.format("Rango inválido: [%d,%d] → [%d,%d]", fromRow, fromCol, toRow, toCol)
            );
        }
    }

    /**
     * Devuelve la sala editada. Permite encadenar el editor con el constructor:
     *   SalaCine sala = new EditorSala(constructor.build())
     *       .clearRow(5)
     *       .setZoneType(8, 0, 9, 14, TipoButaca.VIP)
     *       .getSala();
     */
    public SalaCine getSala() {
        return sala;
    }
}
