package com.cine.dominio;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Representa una función de cine (plantilla: película + sala + hora).
 * NO contiene fecha — la fecha la aporta el cliente al momento de comprar.
 */
public class Funcion {
    private final String id;
    private final Pelicula pelicula;
    private final SalaCine sala;
    private final LocalTime horaInicio;
    private boolean activo = true;
    private boolean eliminada = false;

    public Funcion(Pelicula pelicula, SalaCine sala, LocalTime horaInicio) {
        this(pelicula.getNombre() + "-" + sala.getNombre() + "-" + horaInicio.toString().replace(":", ""), pelicula, sala, horaInicio);
    }

    public Funcion(String id, Pelicula pelicula, SalaCine sala, LocalTime horaInicio) {
        if (pelicula == null) throw new IllegalArgumentException("La función debe tener una película");
        if (sala == null)     throw new IllegalArgumentException("La función debe tener una sala");
        if (horaInicio == null) throw new IllegalArgumentException("La función debe tener una hora de inicio");

        this.id = id;
        this.pelicula = pelicula;
        this.sala = sala;
        this.horaInicio = horaInicio;
    }

    public String getId() { return id; }
    public Pelicula getPelicula() { return pelicula; }
    public SalaCine getSala() { return sala; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFin() { return horaInicio.plusMinutes(pelicula.getDuracionMinutos()); }

    /**
     * Verifica si hay un cruce de horarios con otra función en la misma sala.
     * Compara solo horas (la función es una plantilla recurrente, sin fecha).
     */
    public boolean hayCruceHorario(Funcion otra) {
        if (!this.sala.getId().equals(otra.sala.getId())) return false;
        LocalTime i1 = this.getHoraInicio(), f1 = this.getHoraFin();
        LocalTime i2 = otra.getHoraInicio(), f2 = otra.getHoraFin();
        return i1.isBefore(f2) && f1.isAfter(i2);
    }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public boolean isEliminada() { return eliminada; }
    public void setEliminada(boolean eliminada) { this.eliminada = eliminada; }

    @Override
    public String toString() {
        return String.format("%s - %s (%s)", horaInicio.toString(), pelicula.getNombre(), sala.getNombre());
    }
}
