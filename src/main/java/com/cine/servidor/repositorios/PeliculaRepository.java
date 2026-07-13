package com.cine.servidor.repositorios;

import com.cine.dominio.EstadoPelicula;
import com.cine.dominio.Pelicula;
import com.cine.servidor.bd.ConexionBD;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PeliculaRepository {

    public void save(Pelicula pelicula) {
        String sql = """
            INSERT INTO peliculas (id, titulo, duracion_minutos, clasificacion, fecha_estreno) 
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET 
                titulo=excluded.titulo, 
                duracion_minutos=excluded.duracion_minutos, 
                clasificacion=excluded.clasificacion,
                fecha_estreno=excluded.fecha_estreno;
        """;

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, pelicula.getId());
            pstmt.setString(2, pelicula.getTitulo());
            pstmt.setInt(3, pelicula.getDurationMinutes());
            pstmt.setString(4, pelicula.getClassification());
            pstmt.setString(5, pelicula.getReleaseDate() != null ? pelicula.getReleaseDate().toString() : null);
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Pelicula findById(String id) {
        String sql = "SELECT * FROM peliculas WHERE id = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String fechaStr = rs.getString("fecha_estreno");
                LocalDate fecha = fechaStr != null ? LocalDate.parse(fechaStr) : null;
                
                return new Pelicula(
                    rs.getString("id"),
                    rs.getString("titulo"),
                    rs.getInt("duracion_minutos"),
                    rs.getString("clasificacion"),
                    null, // genre (not stored yet)
                    null, // synopsis
                    null, // poster
                    null, // language
                    fecha,
                    EstadoPelicula.VENTA // Por ahora forzamos estado VENTA
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Pelicula> findAll() {
        List<Pelicula> peliculas = new ArrayList<>();
        String sql = "SELECT * FROM peliculas";
        try (Connection conn = ConexionBD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String fechaStr = rs.getString("fecha_estreno");
                LocalDate fecha = fechaStr != null ? LocalDate.parse(fechaStr) : null;

                peliculas.add(new Pelicula(
                    rs.getString("id"),
                    rs.getString("titulo"),
                    rs.getInt("duracion_minutos"),
                    rs.getString("clasificacion"),
                    null, null, null, null,
                    fecha,
                    EstadoPelicula.VENTA
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return peliculas;
    }
}
