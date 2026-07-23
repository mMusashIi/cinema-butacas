package com.cine.servidor;

import com.cine.dominio.*;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Fuente de verdad en memoria para el servidor.
 * Administra Películas, Salas, Funciones y el estado transaccional de AsientoFuncion.
 *
 * // [PARADIGMA POO]: Se utiliza encapsulamiento para proteger las colecciones internas,
 * exponiendo únicamente métodos de negocio seguros (crearSala, confirmarCompra).
 */
public class EstadoServidor {

    private final Map<String, SalaCine> salas = new ConcurrentHashMap<>();
    private final Map<String, Pelicula> peliculas = new ConcurrentHashMap<>();
    private final Map<String, Funcion> funciones = new ConcurrentHashMap<>();
    private final Map<String, Boleta> boletas = new ConcurrentHashMap<>();

    public EstadoServidor() {
        PersistenciaManager.cargar(this);
        if (salas.isEmpty() && peliculas.isEmpty()) {
            crearDatosDePrueba();
            PersistenciaManager.guardar(this);
        }
    }

    public Map<String, SalaCine> getSalasMap() { return salas; }
    public Map<String, Pelicula> getPeliculasMap() { return peliculas; }
    public Map<String, Funcion> getFuncionesMap() { return funciones; }
    public Map<String, Boleta> getBoletasMap() { return boletas; }
    private void crearDatosDePrueba() {
        // Sala de prueba
        SalaCine sala1 = new SalaCine("Sala 1", 5, 5);
        salas.put(sala1.getId(), sala1);

        // Película de prueba
        Pelicula p1 = new Pelicula("Avatar 3", 150);
        peliculas.put(p1.getId(), p1);

        // Función de prueba
        java.time.LocalTime hora = java.time.LocalTime.now().withHour(18).withMinute(0).withSecond(0).withNano(0);
        Funcion f1 = new Funcion(p1, sala1, hora);
        funciones.put(f1.getId(), f1);
    }

    // // [RUTINA]: Crea o actualiza una sala e hidrata su matriz desde un formato CSV simple.
    // Valida que el nombre sea un número puro y verifica unicidad.
    public void guardarSala(String id, String numeroSala, int filas, int cols, String[] matrizCSV) {
        if (numeroSala == null || !numeroSala.matches("\\d+")) {
            throw new IllegalArgumentException("El identificador de la sala debe ser estrictamente numérico.");
        }
        String nombreOficial = "Sala " + numeroSala;
        
        for (SalaCine s : salas.values()) {
            if (s.getNombre().equalsIgnoreCase(nombreOficial)) {
                if (id == null || id.isEmpty() || !s.getId().equals(id)) {
                    throw new IllegalArgumentException("Ya existe una sala con el número " + numeroSala);
                }
            }
        }

        String targetId = (id != null && !id.isEmpty()) ? id : UUID.randomUUID().toString();
        SalaCine nueva = new SalaCine(targetId, nombreOficial, filas, cols);
        
        // Configurar huecos
        int idx = 0;
        for (int r = 0; r < filas; r++) {
            for (int c = 0; c < cols; c++) {
                if (idx < matrizCSV.length) {
                    TipoButaca tipo = TipoButaca.fromCode(matrizCSV[idx]);
                    if (tipo == TipoButaca.PASILLO) {
                        nueva.clearCell(r, c);
                    } else {
                        Butaca b = new Butaca(String.valueOf((char)('A' + r)), c + 1, tipo);
                        nueva.replaceSeat(r, c, b);
                    }
                    idx++;
                }
            }
        }
        salas.put(nueva.getId(), nueva);
        PersistenciaManager.guardar(this);
    }

    public void crearPelicula(String nombre, int duracion) {
        Pelicula p = new Pelicula(nombre, duracion);
        peliculas.put(p.getId(), p);
        PersistenciaManager.guardar(this);
    }

    public void crearFuncion(String salaId, String peliculaId, java.time.LocalTime hora) {
        SalaCine sala = salas.get(salaId);
        Pelicula pelicula = peliculas.get(peliculaId);
        if (sala == null || pelicula == null) {
            throw new IllegalArgumentException("Sala o película no encontrada.");
        }

        Funcion propuesta = new Funcion(pelicula, sala, hora);
        
        // Validación funcional pura
        List<Funcion> existentes = new ArrayList<>(funciones.values());
        if (ValidadorFunciones.haySolapamiento(existentes, propuesta)) {
            throw new IllegalStateException("Choque de horarios detectado en la sala.");
        }

        funciones.put(propuesta.getId(), propuesta);
        PersistenciaManager.guardar(this);
    }

    /**
     * Calcula el conteo de butacas para una función dada.
     * Retorna un String con formato "total|libres|ocupados".
     */
    // // [RUTINA]: Obtener el conteo general de asientos.
    // [PARADIGMA FUNCIONAL]: Se usa Stream con filter y count para obtener
    // los conteos de forma declarativa, sin variables de ciclo manuales.
    public String getConteoButacas(String funcionId, String fechaStr) {
        Funcion f = funciones.get(funcionId);
        if (f == null) return "0|0|0";
        SalaCine s = f.getSala();
        
        long total = s.getTodasLasButacas().size();
        
        long ocupados = boletas.values().stream()
                .filter(b -> b.getEstado() == EstadoBoleta.ACTIVA)
                .filter(b -> b.getFuncionId().equals(funcionId))
                .filter(b -> b.getFechaReservada().toString().equals(fechaStr))
                .mapToLong(b -> b.getAsientos().size())
                .sum();
                
        long libres = total - ocupados;
        return total + "|" + libres + "|" + ocupados;
    }

    public boolean isAsientoLibre(String lockId) {
        String[] parts = lockId.split("_");
        if (parts.length < 3) return false;
        String funcionId = parts[0];
        String fechaStr = parts[1];
        String butacaId = parts[2];
        
        Funcion f = funciones.get(funcionId);
        if (f != null) {
            SalaCine s = f.getSala();
            Butaca butaca = s.getTodasLasButacas().stream()
                .filter(b -> b.getId().equals(butacaId))
                .findFirst().orElse(null);
            if (butaca == null || butaca.getTipo() == TipoButaca.BROKEN || butaca.getTipo() == TipoButaca.PASILLO) {
                return false;
            }
            
        }
        
        return boletas.values().stream()
            .filter(bol -> bol.getEstado() == com.cine.dominio.EstadoBoleta.ACTIVA)
            .filter(bol -> bol.getFuncionId().equals(funcionId))
            .filter(bol -> bol.getFechaReservada().toString().equals(fechaStr))
            .noneMatch(bol -> bol.getAsientos().contains(lockId));
    }

    // // [RUTINA]: Transacción crítica para confirmar compra de butacas.
    // [PARADIGMA CONCURRENTE]: Se usa 'synchronized' para garantizar atomicidad. 
    // Ningún otro hilo puede confirmar compras simultáneamente.
    public synchronized Boleta confirmarReserva(List<String> lockIds, java.time.LocalDate fechaReservada, MetodoPago metodoPago, String dni) {
        if (lockIds.isEmpty()) throw new IllegalArgumentException("No hay butacas para reservar.");
        
        String funcId = lockIds.get(0).split("_")[0];
        Funcion f = funciones.get(funcId);
        if (f == null) throw new IllegalStateException("Función no encontrada.");
        
        // Validar primero que los asientos no estén ya ocupados
        for (String lid : lockIds) {
            boolean isOcupado = boletas.values().stream()
                .filter(b -> b.getEstado() == EstadoBoleta.ACTIVA)
                .filter(b -> b.getFuncionId().equals(funcId))
                .filter(b -> b.getFechaReservada().equals(fechaReservada))
                .anyMatch(b -> b.getAsientos().contains(lid));
            
            if (isOcupado) {
                throw new IllegalStateException("El asiento " + lid + " ya no está disponible.");
            }
        }
        
        // Generar Boleta
        Boleta b = new Boleta(funcId, f.getPelicula().getNombre(), f.getSala().getNombre(), 
                              f.getHoraInicio(), fechaReservada, metodoPago, lockIds, dni);
        boletas.put(b.getId(), b);
        PersistenciaManager.guardar(this);
        
        return b;
    }
    
    public synchronized void cancelarBoleta(String boletaId) {
        Boleta b = boletas.get(boletaId);
        if (b != null && b.getEstado() == EstadoBoleta.ACTIVA) {
            Funcion f = funciones.get(b.getFuncionId());
            if (f != null) {
                throw new IllegalStateException("No se puede cancelar una boleta de una función que ya ha terminado.");
            }
            b.setEstado(EstadoBoleta.CANCELADA);
            PersistenciaManager.guardar(this);
        } else {
            throw new IllegalArgumentException("La boleta no existe o ya está cancelada.");
        }
    }

    // --- Métodos JSON de salida para el cliente ---
    public String getSalasJson() {
        StringBuilder sb = new StringBuilder("[");
        List<SalaCine> list = new ArrayList<>(salas.values());
        for (int i = 0; i < list.size(); i++) {
            SalaCine s = list.get(i);
            sb.append(String.format("{\"id\":\"%s\",\"nombre\":\"%s\",\"filas\":%d,\"columnas\":%d,\"activo\":%b}", 
                      s.getId(), s.getNombre(), s.getTotalRows(), s.getTotalColumns(), s.isActivo()));
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public String getPeliculasJson() {
        StringBuilder sb = new StringBuilder("[");
        List<Pelicula> list = new ArrayList<>(peliculas.values());
        for (int i = 0; i < list.size(); i++) {
            Pelicula p = list.get(i);
            sb.append(String.format("{\"id\":\"%s\",\"nombre\":\"%s\",\"duracion\":%d,\"activo\":%b}", 
                      p.getId(), p.getNombre(), p.getDuracionMinutos(), p.isActivo()));
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public String getFuncionesJson(String fechaStr) {
        StringBuilder sb = new StringBuilder("[");
        List<Funcion> list = new ArrayList<>(funciones.values());
        for (int i = 0; i < list.size(); i++) {
            Funcion f = list.get(i);
            String conteo = getConteoButacas(f.getId(), fechaStr);
            String[] partes = conteo.split("\\|");
            
            boolean terminada = f.estaTerminada();
            
            sb.append(String.format("{\"id\":\"%s\",\"pelicula\":\"%s\",\"sala\":\"%s\",\"hora\":\"%s\",\"libres\":%s,\"total\":%s,\"duracion\":%d,\"terminada\":%b,\"activo\":%b,\"eliminada\":%b}", 
                      f.getId(), f.getPelicula().getNombre(), f.getSala().getNombre(), f.getHoraInicio().toString(), partes[1], partes[0], f.getPelicula().getDuracionMinutos(), terminada, f.isActivo(), f.isEliminada()));
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
    
    public String getBoletasJson(String funcionId) {
        StringBuilder sb = new StringBuilder("[");
        List<Boleta> lista = boletas.values().stream()
                .filter(b -> b.getFuncionId().equals(funcionId))
                // Ordenar de más reciente a más antigua
                .sorted((b1, b2) -> b2.getFechaEmision().compareTo(b1.getFechaEmision()))
                .collect(Collectors.toList());
                
        for (int i = 0; i < lista.size(); i++) {
            Boleta b = lista.get(i);
            // Convert list of seats to a joined string
            String asientosStr = String.join(",", b.getAsientos());
            sb.append(String.format("{\"id\":\"%s\",\"pelicula\":\"%s\",\"sala\":\"%s\",\"hora\":\"%s\",\"fechaReservada\":\"%s\",\"metodoPago\":\"%s\",\"asientos\":\"%s\",\"estado\":\"%s\",\"fechaEmision\":\"%s\",\"dni\":\"%s\"}", 
                      b.getId(), b.getPeliculaNombre(), b.getSalaNombre(), b.getHoraFuncion().toString(),
                      b.getFechaReservada().toString(), b.getMetodoPago().name(), asientosStr,
                      b.getEstado().name(), b.getFechaEmision().toString(), b.getDni()));
            if (i < lista.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public String getTodasBoletasJson() {
        StringBuilder sb = new StringBuilder("[");
        List<Boleta> lista = boletas.values().stream()
                .sorted((b1, b2) -> b2.getFechaEmision().compareTo(b1.getFechaEmision()))
                .collect(Collectors.toList());
                
        for (int i = 0; i < lista.size(); i++) {
            Boleta b = lista.get(i);
            String asientosStr = String.join(",", b.getAsientos());
            sb.append(String.format("{\"id\":\"%s\",\"pelicula\":\"%s\",\"sala\":\"%s\",\"hora\":\"%s\",\"fechaReservada\":\"%s\",\"metodoPago\":\"%s\",\"asientos\":\"%s\",\"estado\":\"%s\",\"fechaEmision\":\"%s\",\"dni\":\"%s\"}", 
                      b.getId(), b.getPeliculaNombre(), b.getSalaNombre(), b.getHoraFuncion().toString(),
                      b.getFechaReservada().toString(), b.getMetodoPago().name(), asientosStr,
                      b.getEstado().name(), b.getFechaEmision().toString(), b.getDni()));
            if (i < lista.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    // // [RUTINA]: Serializa el estado de toda la matriz de butacas a formato CSV personalizado.
    // Consulta lockManager para emitir LOCKED cuando hay bloqueo pesimista activo.
    public String getRoomStateJson(String funcionId, String fechaStr, String requestingClientId, ManejadorBloqueoButacas lockManager) {
        Funcion f = funciones.get(funcionId);
        if (f == null) return "";

        SalaCine s = f.getSala();
        StringBuilder sb = new StringBuilder();
        
        // [PARADIGMA ESTRUCTURADO]: Doble for para procesar la matriz 2D
        for (int r = 0; r < s.getTotalRows(); r++) {
            for (int c = 0; c < s.getTotalColumns(); c++) {
                Butaca b = s.getButaca(r, c);
                if (b == null) {
                    sb.append("null");
                } else {
                    String lockId = f.getId() + "_" + fechaStr + "_" + b.getId();
                    
                    boolean isOcupado = boletas.values().stream()
                        .filter(bol -> bol.getEstado() == EstadoBoleta.ACTIVA)
                        .filter(bol -> bol.getFuncionId().equals(funcionId))
                        .filter(bol -> bol.getFechaReservada().toString().equals(fechaStr))
                        .anyMatch(bol -> bol.getAsientos().contains(lockId));
                    
                    String estado;
                    if (b.getTipo() == TipoButaca.BROKEN) {
                        estado = "BROKEN";
                    } else if (isOcupado) {
                        estado = "OCUPADO";
                    } else if (lockManager != null && lockManager.isLockedBy(lockId, requestingClientId)) {
                        // Bloqueo pesimista del propio cliente
                        estado = "SELECCIONADO";
                    } else if (lockManager != null && lockManager.isLocked(lockId)) {
                        // Bloqueo pesimista activo: otro cliente lo tiene reservado temporalmente
                        estado = "LOCKED";
                    } else {
                        estado = "LIBRE";
                    }
                    sb.append(lockId).append(":").append(estado);
                }
                if (c < s.getTotalColumns() - 1) sb.append(",");
            }
            if (r < s.getTotalRows() - 1) sb.append(";"); // Separador de filas
        }
        return sb.toString();
    }
    
    public String getSalaConfigJson(String salaId) {
        SalaCine s = salas.get(salaId);
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        // Formato CSV igual que matrizCSV: N,N,P,B...
        for (int r = 0; r < s.getTotalRows(); r++) {
            for (int c = 0; c < s.getTotalColumns(); c++) {
                Butaca b = s.getButaca(r, c);
                if (b == null) {
                    sb.append(TipoButaca.PASILLO.code());
                } else {
                    sb.append(b.getTipo().code());
                }
                if (r < s.getTotalRows() - 1 || c < s.getTotalColumns() - 1) {
                    sb.append(",");
                }
            }
        }
        return String.format("{\"id\":\"%s\",\"nombre\":\"%s\",\"filas\":%d,\"columnas\":%d,\"matriz\":\"%s\"}",
            s.getId(), s.getNombre(), s.getTotalRows(), s.getTotalColumns(), sb.toString());
    }

    public void activarPelicula(String id) {
        Pelicula p = peliculas.get(id);
        if (p != null) { p.setActivo(true); PersistenciaManager.guardar(this); }
    }
    public void desactivarPelicula(String id) {
        Pelicula p = peliculas.get(id);
        if (p != null) { p.setActivo(false); PersistenciaManager.guardar(this); }
    }
    public void activarSala(String id) {
        SalaCine s = salas.get(id);
        if (s != null) { s.setActivo(true); PersistenciaManager.guardar(this); }
    }
    public void desactivarSala(String id) {
        SalaCine s = salas.get(id);
        if (s != null) { s.setActivo(false); PersistenciaManager.guardar(this); }
    }
    public void activarFuncion(String id) {
        Funcion f = funciones.get(id);
        if (f != null) { f.setActivo(true); PersistenciaManager.guardar(this); }
    }
    public void desactivarFuncion(String id) {
        Funcion f = funciones.get(id);
        if (f != null) { f.setActivo(false); PersistenciaManager.guardar(this); }
    }
    public void eliminarFuncion(String id) {
        Funcion f = funciones.get(id);
        if (f != null) { 
            // Soft delete
            f.setEliminada(true); 
            PersistenciaManager.guardar(this); 
        }
    }
}
