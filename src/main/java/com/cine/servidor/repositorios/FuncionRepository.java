package com.cine.servidor.repositorios;

import com.cine.dominio.FormatoFuncion;
import com.cine.dominio.Funcion;
import com.cine.dominio.Pelicula;
import com.cine.dominio.SalaCine;
import com.cine.servidor.bd.ConexionBD;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FuncionRepository {

    private final SalaCineRepository salaRepository;
    private final PeliculaRepository peliculaRepository;

    public FuncionRepository(SalaCineRepository salaRepository, PeliculaRepository peliculaRepository) {
        this.salaRepository = salaRepository;
        this.peliculaRepository = peliculaRepository;
    }

    public void save(Funcion funcion) {
        String sql = """
            INSERT INTO funciones (id, sala_id, pelicula_id, hora_inicio, formato, precio_base) 
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET 
                sala_id=excluded.sala_id, 
                pelicula_id=excluded.pelicula_id, 
                hora_inicio=excluded.hora_inicio,
                formato=excluded.formato,
                precio_base=excluded.precio_base;
        """;

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, funcion.getId());
            pstmt.setString(2, funcion.getSala().getId());
            pstmt.setString(3, funcion.getPelicula().getId());
            pstmt.setString(4, funcion.getStartTime().toString());
            pstmt.setString(5, funcion.getFormat().name());
            pstmt.setDouble(6, funcion.getPrecioBase());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Funcion findById(String id) {
        String sql = "SELECT * FROM funciones WHERE id = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                SalaCine sala = salaRepository.findById(rs.getString("sala_id"));
                Pelicula pelicula = peliculaRepository.findById(rs.getString("pelicula_id"));
                
                return new Funcion(
                    rs.getString("id"),
                    sala,
                    pelicula,
                    LocalDateTime.parse(rs.getString("hora_inicio")),
                    FormatoFuncion.valueOf(rs.getString("formato")),
                    rs.getDouble("precio_base"),
                    false // cancelled
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Funcion> findAll() {
        List<Funcion> funciones = new ArrayList<>();
        String sql = "SELECT id FROM funciones";
        try (Connection conn = ConexionBD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                funciones.add(findById(rs.getString("id")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return funciones;
    }
}
