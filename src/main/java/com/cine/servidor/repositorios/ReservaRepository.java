package com.cine.servidor.repositorios;

import com.cine.dominio.Butaca;
import com.cine.dominio.Funcion;
import com.cine.dominio.boletos.Boleto;
import com.cine.servidor.bd.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
                    // Generar un ID simple para la reserva derivado del ticket (para mantener simplicidad)
                    String reservaId = boleto.getButaca().getId() + "-" + System.currentTimeMillis();
                    
                    pstmtReserva.setString(1, reservaId);
                    pstmtReserva.setString(2, funcion.getId());
                    pstmtReserva.setString(3, "REF-" + reservaId); // Opcional, un codigo visual
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
        List<String> bookedSeats = new java.util.ArrayList<>();
        String sql = """
            SELECT rb.butaca_id 
            FROM reserva_butacas rb
            JOIN reservas r ON rb.reserva_id = r.id
            WHERE r.funcion_id = ?
        """;
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, funcionId);
            java.sql.ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                bookedSeats.add(rs.getString("butaca_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bookedSeats;
    }
}
