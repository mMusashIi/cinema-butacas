package com.cine.servidor;

import com.cine.constructor.ConstructorSala;
import com.cine.constructor.EditorSala;
import com.cine.dominio.*;
import com.cine.dominio.precios.MotorPrecios;
import com.cine.dominio.precios.ConstructorReglaPrecio;
import com.cine.dominio.boletos.Boleto;

import com.cine.servidor.repositorios.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fuente de verdad del servidor: contiene todas las instancias del dominio
 * que se comparten entre múltiples clientes concurrentes.
 */
public class EstadoServidor {

    private Cine cinema;
    private SalaCine sala;
    private Pelicula pelicula;
    private Funcion funcion;
    private final MotorPrecios pricingEngine;

    private final CineRepository cineRepo = new CineRepository();
    private final PeliculaRepository peliculaRepo = new PeliculaRepository();
    private final SalaCineRepository salaRepo = new SalaCineRepository(cineRepo);
    private final FuncionRepository funcionRepo = new FuncionRepository(salaRepo, peliculaRepo);
    private final ReservaRepository reservaRepo = new ReservaRepository();

    /** Historial de tickets generados en esta sesión */
    private final List<Boleto> tickets = Collections.synchronizedList(new ArrayList<>());

    public EstadoServidor() {
        pricingEngine = new MotorPrecios();
        pricingEngine.addPermanentRule(new ConstructorReglaPrecio()
                .onDays(DayOfWeek.WEDNESDAY)
                .discount(0.50)
                .buildNamed("Día del espectador", "50% off los miércoles"));

        cargarDesdeBaseDeDatos();
    }

    private void cargarDesdeBaseDeDatos() {
        List<Cine> cines = cineRepo.findAll();
        if (cines.isEmpty()) {
            crearDatosDePrueba();
        } else {
            this.cinema = cines.get(0);
            this.sala = salaRepo.findAll().get(0);
            this.pelicula = peliculaRepo.findAll().get(0);
            this.funcion = funcionRepo.findAll().get(0);

            // Cargar asientos reservados
            List<String> bookedSeats = reservaRepo.getBookedSeatIds(this.funcion.getId());
            for (String seatId : bookedSeats) {
                Butaca s = findSeatById(seatId);
                if (s != null) {
                    s.seleccionar();
                    s.reservarButaca();
                }
            }
        }
    }

    private void crearDatosDePrueba() {
        cinema = new Cine("Cineplex Central", "Av. Principal 123", "Ciudad");
        cineRepo.save(cinema);

        sala = new ConstructorSala()
                .name("Sala 1")
                .cinema(cinema)
                .rows(7)
                .columns(12)
                .build();

        new EditorSala(sala)
                .setZoneType(5, 0, 6, 11, TipoButaca.VIP)
                .setSeatType(0, 0, TipoButaca.SILLA_RUEDAS)
                .setSeatType(0, 1, TipoButaca.SILLA_RUEDAS);
        salaRepo.save(sala);

        pelicula = new Pelicula("Avatar: El Camino del Agua", 192, "PG-13");
        pelicula.setReleaseDate(LocalDate.of(2024, 12, 16));
        pelicula.startSale();
        peliculaRepo.save(pelicula);

        funcion = new Funcion(sala, pelicula, LocalDateTime.of(2026, 7, 11, 20, 30),
                FormatoFuncion._3D, 15.00);
        funcionRepo.save(funcion);
    }

    private void preBook(int row, int col) {
        Butaca s = sala.getButaca(row, col);
        if (s != null) { s.seleccionar(); s.reservarButaca(); }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Operaciones atómicas sobre butacas
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Confirma la compra de un grupo de butacas de forma atómica.
     * Si alguna falla, hace rollback de todas las anteriores.
     *
     * @param seatIds lista de IDs de butacas ya bloqueadas por el cliente
     * @param dniInput DNI del comprador (puede ser null)
     * @return lista de tickets generados
     * @throws IllegalStateException si alguna butaca no está en estado SELECTED
     */
    public synchronized List<Boleto> confirmPurchase(List<String> seatIds, String dniInput) {
        // Fase 1: validar que todas están en SELECTED
        List<Butaca> seatsToBook = new ArrayList<>();
        for (String id : seatIds) {
            Butaca s = findSeatById(id);
            if (s == null) throw new IllegalStateException("Butaca no encontrada: " + id);
            if (s.getEstado() != EstadoButaca.SELECTED) {
                throw new IllegalStateException("Butaca " + id + " no está en estado SELECTED");
            }
            seatsToBook.add(s);
        }

        // Fase 2: reservar todas (rollback si falla alguna)
        List<Butaca> booked = new ArrayList<>();
        try {
            for (Butaca butaca : seatsToBook) {
                butaca.reservarButaca();
                booked.add(butaca);
            }
        } catch (Exception ex) {
            // Rollback: liberar las que se alcanzaron a reservar
            for (Butaca s : booked) {
                try { s.liberar(); } catch (Exception ignored) {}
            }
            throw ex;
        }

        // Fase 3: generar tickets y guardar en DB
        List<String> discountNames = pricingEngine.getActivePermanentRules().stream()
                .map(r -> r.getNombre()).toList();

        List<Boleto> result = new ArrayList<>();
        for (Butaca butaca : seatsToBook) {
            double base = funcion.getPrecioBase()
                    * funcion.getFormat().getMultiplicadorPrecio()
                    * butaca.getTipo().priceMultiplier();
            double final_ = pricingEngine.calcularPrecio(butaca, funcion);

            Boleto t = new Boleto.Builder()
                    .butaca(butaca)
                    .funcion(funcion)
                    .originalPrice(base)
                    .pricePaid(final_)
                    .appliedDiscountNames(discountNames)
                    .buyerIdNumber(dniInput)
                    .build();
            result.add(t);
        }
        
        // Guardar en la base de datos
        reservaRepo.saveReservas(this.funcion, result);
        
        tickets.addAll(result);
        return result;
    }

    /**
     * Busca una butaca por su ID textual (ej. "B3").
     */
    public Butaca findSeatById(String seatId) {
        return sala.getTodasLasButacas().stream()
                .filter(s -> s.getId().equals(seatId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Serializa el estado actual de toda la sala como JSON simple.
     * Formato: [{"id":"A1","tipo":"NORMAL","estado":"FREE"}, ...]
     */
    public synchronized String getRoomStateJson() {
        StringBuilder sb = new StringBuilder("[");
        List<Butaca> all = sala.getTodasLasButacas();
        for (int i = 0; i < all.size(); i++) {
            Butaca s = all.get(i);
            sb.append("{")
              .append("\"id\":\"").append(s.getId()).append("\",")
              .append("\"row\":\"").append(s.getFila()).append("\",")
              .append("\"number\":").append(s.getNumero()).append(",")
              .append("\"tipo\":\"").append(s.getTipo().code()).append("\",")
              .append("\"estado\":\"").append(s.getEstado().name()).append("\"")
              .append("}");
            if (i < all.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public synchronized String getCinesJson() {
        StringBuilder sb = new StringBuilder("[");
        List<Cine> cines = cineRepo.findAll();
        for (int i = 0; i < cines.size(); i++) {
            Cine c = cines.get(i);
            sb.append("{")
              .append("\"id\":\"").append(c.getId()).append("\",")
              .append("\"nombre\":\"").append(c.getNombre()).append("\",")
              .append("\"direccion\":\"").append(c.getAddress()).append("\",")
              .append("\"ciudad\":\"").append(c.getCity()).append("\"")
              .append("}");
            if (i < cines.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public void crearCine(String nombre, String direccion, String ciudad) {
        Cine nuevoCine = new Cine(nombre, direccion, ciudad);
        cineRepo.save(nuevoCine);
    }

    public void crearSala(String cineId, String nombre, int filas, int columnas, String[] matrizCSV) {
        Cine cineAsociado = cineRepo.findAll().stream()
                .filter(c -> c.getId().equals(cineId))
                .findFirst()
                .orElse(this.cinema); // Fallback al demo

        SalaCine nuevaSala = new ConstructorSala()
                .name(nombre)
                .cinema(cineAsociado)
                .rows(filas)
                .columns(columnas)
                .build();

        EditorSala editor = new EditorSala(nuevaSala);
        int idx = 0;
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                if (idx < matrizCSV.length) {
                    TipoButaca tipo = TipoButaca.fromCode(matrizCSV[idx]);
                    editor.setSeatType(i, j, tipo);
                    idx++;
                }
            }
        }

        salaRepo.save(nuevaSala);
    }

    public synchronized String getPeliculasJson() {
        StringBuilder sb = new StringBuilder("[");
        List<Pelicula> peliculas = peliculaRepo.findAll();
        for (int i = 0; i < peliculas.size(); i++) {
            Pelicula p = peliculas.get(i);
            sb.append("{")
              .append("\"id\":\"").append(p.getId()).append("\",")
              .append("\"titulo\":\"").append(p.getTitulo()).append("\",")
              .append("\"duracion\":").append(p.getDurationMinutes()).append(",")
              .append("\"clasificacion\":\"").append(p.getClassification()).append("\"")
              .append("}");
            if (i < peliculas.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public synchronized String getSalasJson() {
        StringBuilder sb = new StringBuilder("[");
        List<SalaCine> salas = salaRepo.findAll();
        for (int i = 0; i < salas.size(); i++) {
            SalaCine s = salas.get(i);
            sb.append("{")
              .append("\"id\":\"").append(s.getId()).append("\",")
              .append("\"nombre\":\"").append(s.getNombre()).append("\",")
              .append("\"cineId\":\"").append(s.getCinema().getId()).append("\",")
              .append("\"cineNombre\":\"").append(s.getCinema().getNombre()).append("\"")
              .append("}");
            if (i < salas.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public void crearPelicula(String titulo, int duracion, String clasificacion) {
        Pelicula p = new Pelicula(titulo, duracion, clasificacion);
        peliculaRepo.save(p);
    }

    public void crearFuncion(String salaId, String peliculaId, java.time.LocalDateTime horaInicio, com.cine.dominio.FormatoFuncion formato, double precioBase) {
        SalaCine sala = salaRepo.findById(salaId);
        Pelicula pelicula = peliculaRepo.findById(peliculaId);
        if (sala == null || pelicula == null) {
            throw new IllegalArgumentException("Sala o Película no encontrada");
        }
        Funcion f = new Funcion(sala, pelicula, horaInicio, formato, precioBase);
        funcionRepo.save(f);
    }

    // Getters para el ManejadorCliente
    public Cine getCinema()             { return cinema; }
    public SalaCine getSala()           { return sala; }
    public Pelicula getPelicula()               { return pelicula; }
    public Funcion getFuncion()         { return funcion; }
    public MotorPrecios getPricingEngine(){ return pricingEngine; }
    public List<Boleto> getTickets()      { return Collections.unmodifiableList(tickets); }

    public synchronized String getFuncionesJson() {
        StringBuilder sb = new StringBuilder("[");
        List<Funcion> funciones = funcionRepo.findAll();
        for (int i = 0; i < funciones.size(); i++) {
            Funcion f = funciones.get(i);
            sb.append("{")
              .append("\"id\":\"").append(f.getId()).append("\",")
              .append("\"peliculaTitulo\":\"").append(f.getPelicula().getTitulo()).append("\",")
              .append("\"salaNombre\":\"").append(f.getSala().getNombre()).append("\",")
              .append("\"cineNombre\":\"").append(f.getSala().getCinema().getNombre()).append("\",")
              .append("\"horaInicio\":\"").append(f.getStartTime().toString()).append("\",")
              .append("\"formato\":\"").append(f.getFormat().name()).append("\",")
              .append("\"precioBase\":").append(f.getPrecioBase())
              .append("}");
            if (i < funciones.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public synchronized String getFuncionDetalleJson(String id) {
        Funcion f = funcionRepo.findById(id);
        if (f == null) return "{}";
        
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":\"").append(f.getId()).append("\",")
          .append("\"horaInicio\":\"").append(f.getStartTime().toString()).append("\",")
          .append("\"formato\":\"").append(f.getFormat().name()).append("\",")
          .append("\"precioBase\":").append(f.getPrecioBase()).append(",");

        // Pelicula
        Pelicula p = f.getPelicula();
        sb.append("\"pelicula\":{")
          .append("\"id\":\"").append(p.getId()).append("\",")
          .append("\"titulo\":\"").append(p.getTitulo()).append("\",")
          .append("\"duracion\":").append(p.getDurationMinutes()).append(",")
          .append("\"clasificacion\":\"").append(p.getClassification()).append("\"")
          .append("},");

        // Sala
        SalaCine s = f.getSala();
        sb.append("\"sala\":{")
          .append("\"id\":\"").append(s.getId()).append("\",")
          .append("\"nombre\":\"").append(s.getNombre()).append("\",")
          .append("\"filas\":").append(s.getTotalRows()).append(",")
          .append("\"columnas\":").append(s.getTotalColumns()).append(",");
          
        // Cine inside Sala
        Cine c = s.getCinema();
        sb.append("\"cine\":{")
          .append("\"id\":\"").append(c.getId()).append("\",")
          .append("\"nombre\":\"").append(c.getNombre()).append("\",")
          .append("\"direccion\":\"").append(c.getAddress()).append("\",")
          .append("\"ciudad\":\"").append(c.getCity()).append("\"")
          .append("},");

        // Butacas
        sb.append("\"butacas\":[");
        int count = 0;
        int max = s.getTotalRows() * s.getTotalColumns();
        for (int r = 0; r < s.getTotalRows(); r++) {
            for (int col = 0; col < s.getTotalColumns(); col++) {
                Butaca b = s.getButaca(r, col);
                if (b != null) {
                    sb.append("{")
                      .append("\"id\":\"").append(b.getId()).append("\",")
                      .append("\"f\":").append(r).append(",")
                      .append("\"c\":").append(col).append(",")
                      .append("\"t\":\"").append(b.getTipo().code()).append("\"")
                      .append("}");
                    if (count < max - 1) sb.append(",");
                }
                count++;
            }
        }
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("]"); // end butacas
        sb.append("}"); // end sala
        sb.append("}"); // end funcion
        return sb.toString();
    }
}
