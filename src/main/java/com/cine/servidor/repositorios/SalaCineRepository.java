package com.cine.servidor.repositorios;

import com.cine.dominio.Butaca;
import com.cine.dominio.Cine;
import com.cine.dominio.SalaCine;
import com.cine.dominio.TipoButaca;
import com.cine.servidor.bd.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SalaCineRepository {

    private final CineRepository cineRepository;

    public SalaCineRepository(CineRepository cineRepository) {
        this.cineRepository = cineRepository;
    }

    public void save(SalaCine sala) {
        String sqlSala = """
            INSERT INTO salas (id, cine_id, nombre, filas, columnas) 
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET 
                nombre=excluded.nombre, 
                filas=excluded.filas,
                columnas=excluded.columnas;
        """;

        String sqlDeleteButacas = "DELETE FROM sala_butacas WHERE sala_id = ?";
        String sqlInsertButaca = """
            INSERT INTO sala_butacas (id, sala_id, fila, columna, tipo)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = ConexionBD.getConnection()) {
            conn.setAutoCommit(false); // Transaccional

            try (PreparedStatement pstmtSala = conn.prepareStatement(sqlSala)) {
                pstmtSala.setString(1, sala.getId());
                pstmtSala.setString(2, sala.getCinema().getId());
                pstmtSala.setString(3, sala.getNombre());
                pstmtSala.setInt(4, sala.getTotalRows());
                pstmtSala.setInt(5, sala.getTotalColumns());
                pstmtSala.executeUpdate();
            }

            try (PreparedStatement pstmtDelete = conn.prepareStatement(sqlDeleteButacas)) {
                pstmtDelete.setString(1, sala.getId());
                pstmtDelete.executeUpdate();
            }

            try (PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsertButaca)) {
                for (int r = 0; r < sala.getTotalRows(); r++) {
                    for (int c = 0; c < sala.getTotalColumns(); c++) {
                        Butaca b = sala.getButaca(r, c);
                        if (b != null) {
                            pstmtInsert.setString(1, b.getId());
                            pstmtInsert.setString(2, sala.getId());
                            pstmtInsert.setInt(3, r);
                            pstmtInsert.setInt(4, c);
                            pstmtInsert.setString(5, b.getTipo().code());
                            pstmtInsert.addBatch();
                        }
                    }
                }
                pstmtInsert.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public SalaCine findById(String id) {
        String sqlSala = "SELECT * FROM salas WHERE id = ?";
        String sqlButacas = "SELECT * FROM sala_butacas WHERE sala_id = ?";

        try (Connection conn = ConexionBD.getConnection()) {
            SalaCine sala = null;
            
            try (PreparedStatement pstmt = conn.prepareStatement(sqlSala)) {
                pstmt.setString(1, id);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    Cine cine = cineRepository.findById(rs.getString("cine_id"));
                    sala = new SalaCine(
                        rs.getString("id"),
                        rs.getString("nombre"),
                        cine,
                        rs.getInt("filas"),
                        rs.getInt("columnas")
                    );
                }
            }

            if (sala != null) {
                try (PreparedStatement pstmt = conn.prepareStatement(sqlButacas)) {
                    pstmt.setString(1, id);
                    ResultSet rs = pstmt.executeQuery();
                    
                    String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
                    while (rs.next()) {
                        int r = rs.getInt("fila");
                        int c = rs.getInt("columna");
                        TipoButaca tipo = TipoButaca.fromCode(rs.getString("tipo"));
                        
                        String rowLetter = String.valueOf(ALPHABET.charAt(r));
                        int seatNumber = c + 1;
                        
                        Butaca butaca = new Butaca(rs.getString("id"), rowLetter, seatNumber, tipo);
                        sala.replaceSeat(r, c, butaca);
                    }
                }
                return sala;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<SalaCine> findAll() {
        List<SalaCine> salas = new ArrayList<>();
        String sql = "SELECT id FROM salas";
        try (Connection conn = ConexionBD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                salas.add(findById(rs.getString("id")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return salas;
    }
}
