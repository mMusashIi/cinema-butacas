package com.cine.servidor.repositorios;

import com.cine.dominio.Funcion;
import com.cine.dominio.boletos.Boleto;
import com.cine.servidor.bd.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReservaRepository {

    public void saveReservas(Funcion funcion, List<Boleto> boletos) {
        String sqlReserva = """
            INSERT INTO reservas (id, funcion_id, codigo_referencia, dni_comprador, fecha_compra)
            VALUES (?, ?, ?, ?, ?)
        """;
        String sqlReservaButacas = """
            INSERT INTO reserva_butacas (reserva_id, butaca_id)
            VALUES (?, ?)
        """;

        try (Connection conn = ConexionBD.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmtReserva = conn.prepareStatement(sqlReserva);
                 PreparedStatement pstmtButacas = conn.prepareStatement(sqlReservaButacas)) {

                for (Boleto boleto : boletos) {
                    String reservaId = boleto.getButaca().getId() + "-" + System.currentTimeMillis();

                    pstmtReserva.setString(1, reservaId);
                    pstmtReserva.setString(2, funcion.getId());
                    // Corrección: guardar el código de referencia real del boleto (ej. "TK-20260719-000004")
                    pstmtReserva.setString(3, boleto.getReferenceCode());
                    pstmtReserva.setString(4, boleto.getBuyerIdNumber());
                    pstmtReserva.setString(5, java.time.LocalDateTime.now().toString());
                    pstmtReserva.addBatch();

                    pstmtButacas.setString(1, reservaId);
                    pstmtButacas.setString(2, boleto.getButaca().getId());
                    pstmtButacas.addBatch();
                }

                pstmtReserva.executeBatch();
                pstmtButacas.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getBookedSeatIds(String funcionId) {
        List<String> bookedSeats = new ArrayList<>();
        String sql = """
            SELECT rb.butaca_id
            FROM reserva_butacas rb
            JOIN reservas r ON rb.reserva_id = r.id
            WHERE r.funcion_id = ?
        """;
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, funcionId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                bookedSeats.add(rs.getString("butaca_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bookedSeats;
    }

    /**
     * Devuelve todos los registros de compra con datos relevantes para el historial:
     * [0] ref, [1] dni, [2] fecha, [3] pelicula, [4] sala, [5] cine, [6] horario, [7] butacaIds (separados por |)
     */
    public List<String[]> findAllWithDetails() {
        List<String[]> result = new ArrayList<>();
        String sql = """
            SELECT r.codigo_referencia,
                   COALESCE(r.dni_comprador, '') as dni,
                   r.fecha_compra,
                   p.titulo      as pelicula,
                   sa.nombre     as sala,
                   ci.nombre     as cine,
                   f.hora_inicio as horario,
                   GROUP_CONCAT(rb.butaca_id, '|') as butacas
            FROM reservas r
            JOIN funciones f  ON r.funcion_id  = f.id
            JOIN peliculas p  ON f.pelicula_id = p.id
            JOIN salas     sa ON f.sala_id      = sa.id
            JOIN cines     ci ON sa.cine_id     = ci.id
            LEFT JOIN reserva_butacas rb ON r.id = rb.reserva_id
            GROUP BY r.id
            ORDER BY r.fecha_compra DESC
        """;
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                result.add(new String[]{
                    rs.getString("codigo_referencia"),
                    rs.getString("dni"),
                    rs.getString("fecha_compra"),
                    rs.getString("pelicula"),
                    rs.getString("sala"),
                    rs.getString("cine"),
                    rs.getString("horario"),
                    rs.getString("butacas")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }
}
