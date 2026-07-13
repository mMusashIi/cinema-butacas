package com.cine.dominio;

import java.util.UUID;

/**
 * Representa un complejo de cine físico (local).
 * Entidad con identidad propia que agrupa múltiples salas (SalaCine)
 * y disponibilidad de películas (DisponibilidadPelicula).
 */
public class Cine {
    
    private final String id;
    private String name;
    private String address;
    private String city;
    private boolean active;

    public Cine(String name, String address, String city) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre (name) no puede estar vacío");
        }
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException("La ciudad (city) no puede estar vacía");
        }
        
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.address = address;
        this.city = city;
        this.active = true; // Por defecto nace activo
    }

    /**
     * Constructor para restaurar desde la base de datos.
     */
    public Cine(String id, String name, String address, String city, boolean active) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.city = city;
        this.active = active;
    }

    /**
     * Da de baja el local comercial.
     * No se elimina el registro por integridad referencial (historial de boletas, salas, etc).
     */
    public void deactivate() {
        this.active = false;
    }

    // --- Getters y Setters ---

    public String getId() {
        return id;
    }

    public String getNombre() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre (name) no puede estar vacío");
        }
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException("La ciudad (city) no puede estar vacía");
        }
        this.city = city;
    }

    public boolean isActive() {
        return active;
    }
}
