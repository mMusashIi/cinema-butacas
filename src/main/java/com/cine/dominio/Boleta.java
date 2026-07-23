package com.cine.dominio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Representa el comprobante de compra de un conjunto de butacas.
 * Es inmutable en sus datos principales, solo el estado puede cambiar (cancelación).
 */
public class Boleta {
    private final String id;
    private final String funcionId;
    private final String peliculaNombre;
    private final String salaNombre;
    private final LocalDateTime horaFuncion;
    private final LocalDate fechaReservada;
    private final MetodoPago metodoPago;
    private final List<String> asientos;
    private final String dni;
    private final LocalDateTime fechaEmision;
    
    private EstadoBoleta estado;

    public Boleta(String funcionId, String peliculaNombre, String salaNombre, 
                  LocalDateTime horaFuncion, LocalDate fechaReservada, 
                  MetodoPago metodoPago, List<String> asientos, String dni) {
        this("BOL-" + (dni != null && !dni.isBlank() ? dni : "noDNI") + "-" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
             funcionId, peliculaNombre, salaNombre, horaFuncion, fechaReservada, metodoPago, asientos, dni, LocalDateTime.now(), EstadoBoleta.ACTIVA);
    }

    public Boleta(String id, String funcionId, String peliculaNombre, String salaNombre, 
                  LocalDateTime horaFuncion, LocalDate fechaReservada, 
                  MetodoPago metodoPago, List<String> asientos, String dni, LocalDateTime fechaEmision, EstadoBoleta estado) {
        this.id = id;
        this.funcionId = funcionId;
        this.peliculaNombre = peliculaNombre;
        this.salaNombre = salaNombre;
        this.horaFuncion = horaFuncion;
        this.fechaReservada = fechaReservada;
        this.metodoPago = metodoPago;
        this.asientos = List.copyOf(asientos); // Copia inmutable
        this.dni = dni != null && !dni.isBlank() ? dni : "noDNI";
        this.fechaEmision = fechaEmision;
        this.estado = estado;
    }

    public String getId() {
        return id;
    }

    public String getFuncionId() {
        return funcionId;
    }

    public String getPeliculaNombre() {
        return peliculaNombre;
    }

    public String getSalaNombre() {
        return salaNombre;
    }

    public LocalDateTime getHoraFuncion() {
        return horaFuncion;
    }

    public LocalDate getFechaReservada() {
        return fechaReservada;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public List<String> getAsientos() {
        return asientos;
    }

    public String getDni() {
        return dni;
    }

    public LocalDateTime getFechaEmision() {
        return fechaEmision;
    }

    public EstadoBoleta getEstado() {
        return estado;
    }

    public void setEstado(EstadoBoleta estado) {
        this.estado = estado;
    }
}
