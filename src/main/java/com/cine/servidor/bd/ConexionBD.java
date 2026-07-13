package com.cine.servidor.bd;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gestor de conexión a la Base de Datos SQLite (Nivel 3).
 * Se encarga de inicializar el archivo cine.db y crear las tablas si no existen.
 */
public class ConexionBD {

    private static final String URL = "jdbc:sqlite:cine.db";

    /**
     * Obtiene una nueva conexión a la base de datos.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    /**
     * Inicializa la estructura de la base de datos (CREATE TABLES).
     */
    public static void inicializarEstructura() {
        String sqlCines = """
            CREATE TABLE IF NOT EXISTS cines (
                id TEXT PRIMARY KEY,
                nombre TEXT NOT NULL,
                direccion TEXT,
                ciudad TEXT
            );
        """;

        String sqlSalas = """
            CREATE TABLE IF NOT EXISTS salas (
                id TEXT PRIMARY KEY,
                cine_id TEXT NOT NULL,
                nombre TEXT NOT NULL,
                filas INTEGER NOT NULL,
                columnas INTEGER NOT NULL,
                FOREIGN KEY (cine_id) REFERENCES cines(id)
            );
        """;

        String sqlSalaButacas = """
            CREATE TABLE IF NOT EXISTS sala_butacas (
                id TEXT PRIMARY KEY,
                sala_id TEXT NOT NULL,
                fila INTEGER NOT NULL,
                columna INTEGER NOT NULL,
                tipo TEXT NOT NULL,
                FOREIGN KEY (sala_id) REFERENCES salas(id),
                UNIQUE (sala_id, fila, columna)
            );
        """;

        String sqlPeliculas = """
            CREATE TABLE IF NOT EXISTS peliculas (
                id TEXT PRIMARY KEY,
                titulo TEXT NOT NULL,
                duracion_minutos INTEGER,
                clasificacion TEXT,
                fecha_estreno TEXT
            );
        """;

        String sqlFunciones = """
            CREATE TABLE IF NOT EXISTS funciones (
                id TEXT PRIMARY KEY,
                sala_id TEXT NOT NULL,
                pelicula_id TEXT NOT NULL,
                hora_inicio TEXT NOT NULL,
                formato TEXT NOT NULL,
                precio_base REAL NOT NULL,
                FOREIGN KEY (sala_id) REFERENCES salas(id),
                FOREIGN KEY (pelicula_id) REFERENCES peliculas(id)
            );
        """;

        String sqlReservas = """
            CREATE TABLE IF NOT EXISTS reservas (
                id TEXT PRIMARY KEY,
                funcion_id TEXT NOT NULL,
                codigo_referencia TEXT NOT NULL,
                dni_comprador TEXT,
                fecha_compra TEXT NOT NULL,
                FOREIGN KEY (funcion_id) REFERENCES funciones(id)
            );
        """;

        String sqlReservaButacas = """
            CREATE TABLE IF NOT EXISTS reserva_butacas (
                reserva_id TEXT NOT NULL,
                butaca_id TEXT NOT NULL,
                PRIMARY KEY (reserva_id, butaca_id),
                FOREIGN KEY (reserva_id) REFERENCES reservas(id)
            );
        """;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sqlCines);
            stmt.execute(sqlSalas);
            stmt.execute(sqlSalaButacas);
            stmt.execute(sqlPeliculas);
            stmt.execute(sqlFunciones);
            stmt.execute(sqlReservas);
            stmt.execute(sqlReservaButacas);
            System.out.println("[BD] Estructura de Base de Datos SQLite inicializada correctamente.");
        } catch (SQLException e) {
            System.err.println("[BD] Error inicializando la base de datos:");
            e.printStackTrace();
        }
    }
}
