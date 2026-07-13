package com.cine.dominio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Representa una sala física dentro de un complejo de cine (Cine).
 * Administra la matriz bidimensional de butacas donde:
 *   - null  = espacio vacío (pasillo, hueco, zona sin butaca)
 *   - Butaca  = butaca concreta con identidad y estado propio
 *
 * Regla de numeración (CRÍTICA):
 * El número de cada Butaca equivale a su índice de columna + 1.
 * Los huecos (null) NO rompen la numeración de las butacas vecinas.
 * Ejemplo — fila "A" con patrón OOO____OOO (10 columnas):
 *   Columnas 0,1,2 → A1, A2, A3
 *   Columnas 3,4,5,6 → null
 *   Columnas 7,8,9 → A8, A9, A10
 *
 * SalaCine no sabe de precios, de funciones ni de compras — eso
 * pertenece a Funcion y a MotorPrecios.
 */
public class SalaCine {

    private final String id;
    private String name;
    private final Cine cinema;        // Local al que pertenece esta sala
    private final int totalRows;        // Número de filas (altura de la matriz)
    private final int totalColumns;     // Número de columnas (ancho de la matriz)
    private final Butaca[][] seatingMatrix; // null = espacio vacío

    /**
     * Constructor. La matriz se inicializa completa (todas las celdas con Butaca FREE),
     * sin huecos. Usar EditorSala para crear pasillos y cambiar tipos de butaca.
     *
     * @param name         Nombre de la sala (ej. "Sala 1", "Sala IMAX")
     * @param cinema       Local al que pertenece
     * @param totalRows    Número de filas (máximo 26 — una letra por fila)
     * @param totalColumns Número de columnas por fila
     */
    public SalaCine(String name, Cine cinema, int totalRows, int totalColumns) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la sala no puede estar vacío");
        }
        if (cinema == null) {
            throw new IllegalArgumentException("La sala debe pertenecer a un Cine");
        }
        if (totalRows <= 0 || totalRows > 26) {
            throw new IllegalArgumentException("totalRows debe estar entre 1 y 26 (A–Z)");
        }
        if (totalColumns <= 0) {
            throw new IllegalArgumentException("totalColumns debe ser positivo");
        }

        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.cinema = cinema;
        this.totalRows = totalRows;
        this.totalColumns = totalColumns;
        this.seatingMatrix = buildMatrix(totalRows, totalColumns);
    }

    /**
     * Constructor para restaurar desde la base de datos, usando un ID existente
     * y sin construir la matriz por defecto (se cargará manualmente).
     */
    public SalaCine(String id, String name, Cine cinema, int totalRows, int totalColumns) {
        this.id = id;
        this.name = name;
        this.cinema = cinema;
        this.totalRows = totalRows;
        this.totalColumns = totalColumns;
        this.seatingMatrix = new Butaca[totalRows][totalColumns]; // Se llena luego
    }

    // --- Construcción inicial de la matriz ---

    /**
     * Genera la matriz inicial con todas las celdas ocupadas por Butaca FREE.
     * La letra de fila se asigna por índice (0=A, 1=B, …, 25=Z).
     * El número de butaca = índice de columna + 1.
     */
    private Butaca[][] buildMatrix(int rows, int columns) {
        final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Butaca[][] matrix = new Butaca[rows][columns];

        for (int r = 0; r < rows; r++) {
            String rowLetter = String.valueOf(ALPHABET.charAt(r));
            for (int c = 0; c < columns; c++) {
                int seatNumber = c + 1; // Numeración basada en columna, no en contador
                matrix[r][c] = new Butaca(rowLetter, seatNumber, TipoButaca.NORMAL);
            }
        }
        return matrix;
    }

    // --- Acceso a la matriz (solo lectura desde afuera) ---

    /**
     * Devuelve la butaca en la posición [rowIndex][colIndex], o null si es espacio vacío.
     */
    public Butaca getButaca(int rowIndex, int colIndex) {
        validateBounds(rowIndex, colIndex);
        return seatingMatrix[rowIndex][colIndex];
    }

    /**
     * Devuelve todas las butacas reales (sin nulls), en orden de fila y columna.
     * Útil para operaciones funcionales sobre el conjunto de butacas.
     */
    public List<Butaca> getTodasLasButacas() {
        List<Butaca> butacas = new ArrayList<>();
        for (Butaca[] row : seatingMatrix) {
            for (Butaca butaca : row) {
                if (butaca != null) {
                    butacas.add(butaca);
                }
            }
        }
        return butacas;
    }

    /**
     * Devuelve la fila completa (array con posibles nulls para huecos).
     * Útil para renderizar una fila en la UI respetando la posición de los huecos.
     */
    public Butaca[] getFila(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= totalRows) {
            throw new IndexOutOfBoundsException("rowIndex fuera de rango: " + rowIndex);
        }
        return Arrays.copyOf(seatingMatrix[rowIndex], totalColumns);
    }

    // --- Consultas funcionales ---

    /**
     * Cuenta las butacas disponibles (FREE) de forma funcional, sin mutar nada.
     */
    public long getAvailableSeatsCount() {
        return getTodasLasButacas().stream()
                .filter(butaca -> butaca.getEstado() == EstadoButaca.FREE)
                .count();
    }

    /**
     * Cuenta las butacas accesibles (SILLA_RUEDAS) disponibles en la sala.
     * Útil para validaciones de accesibilidad en la venta.
     */
    public long getAccessibleAvailableCount() {
        return getTodasLasButacas().stream()
                .filter(butaca -> butaca.getTipo().accessible() && butaca.getEstado() == EstadoButaca.FREE)
                .count();
    }

    /**
     * Cuenta el total de butacas reales en la sala (excluyendo huecos).
     */
    public long getTotalSeatCount() {
        return getTodasLasButacas().size();
    }

    // --- Modificación interna de la matriz (usada por EditorSala) ---

    /**
     * Reemplaza la butaca en [rowIndex][colIndex] con una nueva instancia de Butaca.
     * Uso exclusivo de EditorSala — no invocar directamente desde la UI ni desde el dominio.
     */
    public void replaceSeat(int rowIndex, int colIndex, Butaca newSeat) {
        validateBounds(rowIndex, colIndex);
        seatingMatrix[rowIndex][colIndex] = newSeat;
    }

    /**
     * Marca la celda [rowIndex][colIndex] como espacio vacío (null).
     * Nunca afecta la numeración de las butacas vecinas.
     * Uso exclusivo de EditorSala — no invocar directamente desde la UI.
     */
    public void clearCell(int rowIndex, int colIndex) {
        validateBounds(rowIndex, colIndex);
        seatingMatrix[rowIndex][colIndex] = null;
    }

    // --- Validación interna ---

    private void validateBounds(int rowIndex, int colIndex) {
        if (rowIndex < 0 || rowIndex >= totalRows) {
            throw new IndexOutOfBoundsException("rowIndex fuera de rango: " + rowIndex);
        }
        if (colIndex < 0 || colIndex >= totalColumns) {
            throw new IndexOutOfBoundsException("colIndex fuera de rango: " + colIndex);
        }
    }

    // --- Getters ---

    public String getId() {
        return id;
    }

    public String getNombre() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la sala no puede estar vacío");
        }
        this.name = name;
    }

    public Cine getCinema() {
        return cinema;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getTotalColumns() {
        return totalColumns;
    }

    @Override
    public String toString() {
        return String.format("SalaCine{id='%s', name='%s', cinema='%s', rows=%d, cols=%d, butacas=%d}",
                id, name, cinema.getNombre(), totalRows, totalColumns, getTotalSeatCount());
    }
}
