package com.cine.dominio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Representa una sala física.
 * Administra la matriz bidimensional de butacas donde:
 * - null = espacio vacío (pasillo, hueco, zona sin butaca)
 * - Butaca = butaca concreta con identidad y estado propio
 *
 * Regla de numeración (CRÍTICA):
 * El número de cada Butaca equivale a su índice de columna + 1.
 * Los huecos (null) NO rompen la numeración de las butacas vecinas.
 * Ejemplo — fila "A" con patrón OOO____OOO (10 columnas):
 * Columnas 0,1,2 → A1, A2, A3
 * Columnas 3,4,5,6 → null
 * Columnas 7,8,9 → A8, A9, A10
 *
 * SalaCine no sabe de precios, de funciones ni de compras — eso
 * pertenece a Funcion y a MotorPrecios.
 */
// [PARADIGMA POO]: Encapsulamiento de los datos de la sala
public class SalaCine {

    private final String id;
    private String name;
    private final int totalFilas; // Número de filas (altura de la matriz)
    private final int totalColumnas; // Número de columnas (ancho de la matriz)
    private final Butaca[][] seatingMatrix; // null = espacio vacío

    /**
     * Constructor. La matriz se inicializa completa (todas las celdas con Butaca
     * FREE),
     * sin huecos. Usar EditorSala para crear pasillos y cambiar tipos de butaca.
     *
     * @param name          Nombre de la sala (ej. "Sala 1", "Sala IMAX")
     * @param totalFilas    Número de filas (máximo 26 — una letra por fila)
     * @param totalColumnas Número de columnas por fila
     */
    // // [RUTINA]: Constructor principal de SalaCine.
    // Inicializa la matriz completa de butacas basándose en filas y columnas.
    public SalaCine(String name, int totalFilas, int totalColumnas) {
        this(name, name, totalFilas, totalColumnas);
    }
    
    public SalaCine(String id, String name, int totalFilas, int totalColumnas) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la sala no puede estar vacío");
        }
        if (totalFilas <= 0 || totalFilas > 26) {
            throw new IllegalArgumentException("totalRows debe estar entre 1 y 26 (A–Z)");
        }
        if (totalColumnas <= 0) {
            throw new IllegalArgumentException("totalColumns debe ser positivo");
        }

        this.id = id;
        this.name = name;
        this.totalFilas = totalFilas;
        this.totalColumnas = totalColumnas;
        this.seatingMatrix = buildMatrix(totalFilas, totalColumnas);
    }

    // --- Construcción inicial de la matriz ---

    /**
     * Genera la matriz inicial con todas las celdas ocupadas por Butaca FREE.
     * La letra de fila se asigna por índice (0=A, 1=B, …, 25=Z).
     * El número de butaca = índice de columna + 1.
     */
    private Butaca[][] buildMatrix(int filas, int columnas) {
        final String LETRAS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Butaca[][] matriz = new Butaca[filas][columnas];

        // [PARADIGMA ESTRUCTURADO]: Uso de ciclos for anidados para iterar
        // y llenar la matriz bidimensional.
        for (int r = 0; r < filas; r++) {
            String letraFila = String.valueOf(LETRAS.charAt(r));
            for (int c = 0; c < columnas; c++) {
                int numeroAsiento = c + 1; // Numeración basada en columna, no en contador
                matriz[r][c] = new Butaca(letraFila, numeroAsiento, TipoButaca.NORMAL);
            }
        }
        return matriz;
    }

    // --- Acceso a la matriz (solo lectura desde afuera) ---

    /**
     * Devuelve la butaca en la posición [rowIndex][colIndex], o null si es espacio
     * vacío.
     */
    // // [RUTINA]: Devuelve la butaca específica en una coordenada dada.
    public Butaca getButaca(int rowIndex, int colIndex) {
        validateBounds(rowIndex, colIndex);
        return seatingMatrix[rowIndex][colIndex];
    }

    /**
     * Devuelve todas las butacas reales (sin nulls), en orden de fila y columna.
     * Útil para operaciones funcionales sobre el conjunto de butacas.
     */
    // // [RUTINA]: Extrae todas las butacas válidas saltándose los huecos nulos.
    // Aplica filtrado básico estructurado.
    public List<Butaca> getTodasLasButacas() {
        List<Butaca> butacas = new ArrayList<>();
        // [PARADIGMA ESTRUCTURADO]: Iteración clásica sobre un arreglo 2D
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
        if (rowIndex < 0 || rowIndex >= totalFilas) {
            throw new IndexOutOfBoundsException("rowIndex fuera de rango: " + rowIndex);
        }
        return Arrays.copyOf(seatingMatrix[rowIndex], totalColumnas);
    }

    // --- Consultas funcionales ---

    /**
     * Cuenta el total de butacas reales en la sala (excluyendo huecos).
     */
    // [PARADIGMA FUNCIONAL]: Si usáramos streams se vería como
    // Arrays.stream(seatingMatrix)...
    // Aquí usamos un método abstracto que abstrae el conteo.
    // // [RUTINA]: Cuenta total de butacas reales instaladas en la sala.
    public long getTotalSeatCount() {
        return getTodasLasButacas().size();
    }

    // --- Modificación interna de la matriz (usada por EditorSala) ---

    /**
     * Reemplaza la butaca en [rowIndex][colIndex] con una nueva instancia de
     * Butaca.
     * Uso exclusivo de EditorSala — no invocar directamente desde la UI ni desde el
     * dominio.
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
        if (rowIndex < 0 || rowIndex >= totalFilas) {
            throw new IndexOutOfBoundsException("rowIndex fuera de rango: " + rowIndex);
        }
        if (colIndex < 0 || colIndex >= totalColumnas) {
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

    public int getTotalRows() {
        return totalFilas;
    }

    public int getTotalColumns() {
        return totalColumnas;
    }

    @Override
    public String toString() {
        return String.format("SalaCine{id='%s', name='%s', rows=%d, cols=%d, butacas=%d}",
                id, name, totalFilas, totalColumnas, getTotalSeatCount());
    }
}
