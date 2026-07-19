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
    private final java.util.Map<String, SalaCine> salasEnMemoria = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, Funcion> funcionesEnMemoria = new java.util.concurrent.ConcurrentHashMap<>();

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
            cines = cineRepo.findAll();
        }

        for (SalaCine s : salaRepo.findAll()) {
            salasEnMemoria.put(s.getId(), s);
        }

        for (Funcion f : funcionRepo.findAll()) {
            funcionesEnMemoria.put(f.getId(), f);
            List<String> bookedSeats = reservaRepo.getBookedSeatIds(f.getId());
            for (String seatId : bookedSeats) {
                Butaca s = f.getSala().getTodasLasButacas().stream()
                        .filter(seat -> seat.getId().equals(seatId))
                        .findFirst()
                        .orElse(null);
                if (s != null) {
                    try { s.seleccionar(); } catch(Exception ignored) {}
                    try { s.reservarButaca(); } catch(Exception ignored) {}
                }
            }
        }

        // Barrido de seguridad: SELECTED es un estado volátil de sesión que nunca
        // debe sobrevivir a un reinicio del servidor. Si el servidor crasheó mientras
        // un cliente tenía butacas bloqueadas, éstas quedarían en SELECTED en memoria
        // sin que ningún cliente tenga el lock, bloqueándolas indefinidamente.
        // Este barrido garantiza que al arrancar, todas las butacas están en FREE o BOOKED.
        for (SalaCine sala : salasEnMemoria.values()) {
            for (Butaca b : sala.getTodasLasButacas()) {
                if (b.getEstado() == EstadoButaca.SELECTED) {
                    try {
                        b.liberar();
                        System.out.printf("[EstadoServidor] Butaca %s liberada (estaba en SELECTED al arrancar)%n", b.getId());
                    } catch (Exception ignored) {}
                }
            }
        }

        if (!cines.isEmpty()) this.cinema = cines.get(0);
        if (!salasEnMemoria.isEmpty()) this.sala = salasEnMemoria.values().iterator().next();
        if (!funcionesEnMemoria.isEmpty()) {
            this.funcion = funcionesEnMemoria.values().iterator().next();
            this.pelicula = this.funcion.getPelicula();
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
    public synchronized List<Boleto> confirmPurchase(List<String> seatIds, String dniInput, String funcionId) {
        Funcion f = funcionId != null ? funcionesEnMemoria.get(funcionId) : this.funcion;
        if (f == null) throw new IllegalStateException("Función no encontrada");

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

        // Fase 2: reservar todas (rollback completo si falla alguna)
        List<Butaca> booked = new ArrayList<>();
        try {
            for (Butaca butaca : seatsToBook) {
                butaca.reservarButaca();
                booked.add(butaca);
            }
        } catch (Exception ex) {
            // Rollback total: liberar TODAS las butacas del grupo,
            // tanto las ya BOOKED como las que aún quedaron en SELECTED.
            for (Butaca s : seatsToBook) {
                EstadoButaca est = s.getEstado();
                if (est == EstadoButaca.BOOKED || est == EstadoButaca.SELECTED) {
                    try { s.liberar(); } catch (Exception ignored) {}
                }
            }
            throw ex;
        }

        // Fase 3: generar tickets y guardar en DB
        List<String> discountNames = pricingEngine.getActivePermanentRules().stream()
                .map(r -> r.getNombre()).toList();

        List<Boleto> result = new ArrayList<>();
        for (Butaca butaca : seatsToBook) {
            double base = f.getPrecioBase()
                    * f.getFormat().getMultiplicadorPrecio()
                    * butaca.getTipo().priceMultiplier();
            double final_ = pricingEngine.calcularPrecio(butaca, f);

            Boleto t = new Boleto.Builder()
                    .butaca(butaca)
                    .funcion(f)
                    .originalPrice(base)
                    .pricePaid(final_)
                    .appliedDiscountNames(discountNames)
                    .buyerIdNumber(dniInput)
                    .build();
            result.add(t);
        }
        
        // Guardar en la base de datos
        reservaRepo.saveReservas(f, result);
        
        tickets.addAll(result);
        return result;
    }

    /**
     * Busca una butaca por su ID textual (ej. "B3") en todas las salas en memoria.
     */
    public Butaca findSeatById(String seatId) {
        for (SalaCine s : salasEnMemoria.values()) {
            Butaca b = s.getTodasLasButacas().stream()
                    .filter(seat -> seat.getId().equals(seatId))
                    .findFirst()
                    .orElse(null);
            if (b != null) return b;
        }
        return null;
    }

    /**
     * Devuelve el historial de compras como JSON para el panel de administración.
     * Cada objeto contiene: ref, dni, fecha, pelicula, sala, cine, horario, butacas (legibles).
     */
    public synchronized String getReservasJson() {
        java.util.List<String[]> rows = reservaRepo.findAllWithDetails();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            // r: [0]=ref [1]=dni [2]=fecha [3]=pelicula [4]=sala [5]=cine [6]=horario [7]=butacaIds
            // Convertir IDs de butacas a etiquetas legibles (Fila+Número)
            String butacasLegibles = "";
            if (r[7] != null && !r[7].isBlank()) {
                String[] ids = r[7].split("\\|");
                java.util.StringJoiner sj = new java.util.StringJoiner(", ");
                for (String id : ids) {
                    Butaca b = findSeatById(id.trim());
                    sj.add(b != null ? b.getFila() + b.getNumero() : id.substring(0, 8) + "…");
                }
                butacasLegibles = sj.toString();
            }
            sb.append("{")
              .append("\"ref\":\"").append(r[0]).append("\",")
              .append("\"dni\":\"").append(r[1] != null ? r[1] : "").append("\",")
              .append("\"fecha\":\"").append(r[2] != null ? r[2].substring(0, Math.min(16, r[2].length())) : "").append("\",")
              .append("\"pelicula\":\"").append(r[3]).append("\",")
              .append("\"sala\":\"").append(r[4]).append("\",")
              .append("\"cine\":\"").append(r[5]).append("\",")
              .append("\"horario\":\"").append(r[6] != null ? r[6].substring(0, Math.min(16, r[6].length())) : "").append("\",")
              .append("\"butacas\":\"").append(butacasLegibles).append("\"")
              .append("}");
            if (i < rows.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Serializa el estado actual de toda la sala como JSON simple.
     * Formato: [{"id":"A1","tipo":"NORMAL","estado":"FREE"}, ...]
     */
    public synchronized String getRoomStateJson(String funcionId) {
        Funcion f = funcionId != null ? funcionesEnMemoria.get(funcionId) : this.funcion;
        if (f == null) return "[]";
        SalaCine sToUse = f.getSala();
        
        StringBuilder sb = new StringBuilder("[");
        List<Butaca> all = sToUse.getTodasLasButacas();
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
                    // Bug 5: PASILLO significa celda vacía (null), no una Butaca con tipo PASILLO.
                    // Usar clearCell() para que el modelo de dominio sea consistente.
                    if (tipo == TipoButaca.PASILLO) {
                        editor.clearCell(i, j);
                    } else {
                        editor.setSeatType(i, j, tipo);
                    }
                    idx++;
                }
            }
        }

        salaRepo.save(nuevaSala);
        salasEnMemoria.put(nuevaSala.getId(), nuevaSala);
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
        SalaCine memSala = salasEnMemoria.get(salaId);
        if (memSala != null) sala = memSala;
        
        Funcion f = new Funcion(sala, pelicula, horaInicio, formato, precioBase);
        funcionRepo.save(f);
        funcionesEnMemoria.put(f.getId(), f);
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
        sb.append("\"funcionId\":\"").append(f.getId()).append("\",")
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

        // Butacas — incluye TODAS las celdas (incluyendo PASILLO/null) para
        // que el cliente pueda reconstruir la geometría exacta de la sala.
        sb.append("\"butacas\":[");
        boolean first = true;
        for (int r = 0; r < s.getTotalRows(); r++) {
            for (int col = 0; col < s.getTotalColumns(); col++) {
                Butaca b = s.getButaca(r, col);
                if (!first) sb.append(",");
                if (b != null) {
                    sb.append("{")
                      .append("\"id\":\"").append(b.getId()).append("\",")
                      .append("\"f\":").append(r).append(",")
                      .append("\"c\":").append(col).append(",")
                      .append("\"t\":\"").append(b.getTipo().code()).append("\"")
                      .append("}");
                } else {
                    // Celda vacía: se envía como PASILLO para que el cliente la limpie
                    sb.append("{")
                      .append("\"id\":\"").append("\",")
                      .append("\"f\":").append(r).append(",")
                      .append("\"c\":").append(col).append(",")
                      .append("\"t\":\"PASILLO\"")
                      .append("}");
                }
                first = false;
            }
        }
        sb.append("]"); // end butacas
        sb.append("}"); // end sala
        sb.append("}"); // end funcion
        return sb.toString();
    }
}
