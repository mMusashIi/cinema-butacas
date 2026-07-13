package com.cine.servidor.repositorios;

import com.cine.dominio.Cine;
import com.cine.servidor.bd.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CineRepository {

    public void save(Cine cine) {
        String sql = """
            INSERT INTO cines (id, nombre, direccion, ciudad) 
            VALUES (?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET 
                nombre=excluded.nombre, 
                direccion=excluded.direccion, 
                ciudad=excluded.ciudad;
        """;

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, cine.getId());
            pstmt.setString(2, cine.getNombre());
            pstmt.setString(3, cine.getAddress());
            pstmt.setString(4, cine.getCity());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Cine findById(String id) {
        String sql = "SELECT * FROM cines WHERE id = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return new Cine(
                    rs.getString("id"),
                    rs.getString("nombre"),
                    rs.getString("direccion"),
                    rs.getString("ciudad"),
                    true
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Cine> findAll() {
        List<Cine> cines = new ArrayList<>();
        String sql = "SELECT * FROM cines";
        try (Connection conn = ConexionBD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                cines.add(new Cine(
                    rs.getString("id"),
                    rs.getString("nombre"),
                    rs.getString("direccion"),
                    rs.getString("ciudad"),
                    true
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cines;
    }
}
