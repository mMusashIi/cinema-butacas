package com.cine.servidor;

import com.cine.dominio.EstadoButaca;
import com.cine.compartido.Protocolo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

// [PARADIGMA CONCURRENTE / MULTITHREAD]: Esta clase implementa Runnable, 
// lo que significa que cada cliente conectado correrá en su propio hilo.
public class ManejadorCliente implements Runnable {

    private final String clientId = UUID.randomUUID().toString().substring(0, 8);
    private final Socket socket;
    private final EstadoServidor state;
    private final ManejadorBloqueoButacas lockManager;
    private final BiConsumer<String, String> broadcaster;
    private PrintWriter out;
    private final AtomicBoolean disconnected = new AtomicBoolean(false);

    public ManejadorCliente(Socket socket, EstadoServidor state, ManejadorBloqueoButacas lockManager,
                         BiConsumer<String, String> broadcaster) {
        this.socket = socket;
        this.state = state;
        this.lockManager = lockManager;
        this.broadcaster = broadcaster;
    }

    public String getClientId() { return clientId; }

    public synchronized void sendMessage(String msg) {
        if (out != null) out.println(msg);
    }

    // // [RUTINA]: Ciclo de vida del cliente (escucha continua de socket).
    @Override
    public void run() {
        System.out.printf("[Server] Cliente conectado: %s (%s)%n", clientId, socket.getRemoteSocketAddress());
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter pw = new PrintWriter(socket.getOutputStream(), true)
        ) {
            this.out = pw;
            
            String line;
            while ((line = in.readLine()) != null) {
                handleMessage(line.trim());
            }
        } catch (IOException e) {
            if (!disconnected.get()) System.out.printf("[Server] Cliente desconectado: %s%n", clientId);
        } finally {
            onDisconnect(false);
        }
    }

    // // [RUTINA]: Interpreta y rutea los comandos de texto del protocolo.
    private void handleMessage(String msg) {
        if (msg.isEmpty()) return;
        System.out.printf("[Server][%s] ← %s%n", clientId, msg);

        try {
            if (msg.equals(Protocolo.PING)) {
                sendMessage(Protocolo.PONG);
            } else if (msg.startsWith(Protocolo.OBTENER_SALA + Protocolo.SEP)) {
                String payload = msg.substring(Protocolo.OBTENER_SALA.length() + 1);
                String[] p = payload.split("\\" + Protocolo.SEP_SUB);
                if (p.length >= 2) sendMessage(Protocolo.roomState(state.getRoomStateJson(p[0], p[1], clientId, lockManager)));
            } else if (msg.startsWith(Protocolo.CONTEO_BUTACAS + Protocolo.SEP)) {
                String payload = msg.substring(Protocolo.CONTEO_BUTACAS.length() + 1);
                String[] p = payload.split("\\" + Protocolo.SEP_SUB);
                if (p.length >= 2) sendMessage(Protocolo.RESPUESTA_CONTEO + Protocolo.SEP + state.getConteoButacas(p[0], p[1]));
            } else if (msg.startsWith(Protocolo.SELECCIONAR + Protocolo.SEP)) {
                handleSelect(msg.substring(Protocolo.SELECCIONAR.length() + 1));
            } else if (msg.startsWith(Protocolo.DESELECCIONAR + Protocolo.SEP)) {
                handleDeselect(msg.substring(Protocolo.DESELECCIONAR.length() + 1));
            } else if (msg.startsWith(Protocolo.RESERVAR + Protocolo.SEP)) {
                handleBook(msg.substring(Protocolo.RESERVAR.length() + 1));
            } else if (msg.equals(Protocolo.LISTAR_SALAS)) {
                sendMessage(Protocolo.RESPUESTA_SALAS + Protocolo.SEP + state.getSalasJson());
            } else if (msg.equals(Protocolo.LISTAR_PELICULAS)) {
                sendMessage(Protocolo.RESPUESTA_PELICULAS + Protocolo.SEP + state.getPeliculasJson());
            } else if (msg.startsWith(Protocolo.LISTAR_FUNCIONES + Protocolo.SEP)) {
                String fechaStr = msg.substring(Protocolo.LISTAR_FUNCIONES.length() + 1);
                sendMessage(Protocolo.RESPUESTA_FUNCIONES + Protocolo.SEP + state.getFuncionesJson(fechaStr));
            } else if (msg.startsWith(Protocolo.OBTENER_SALA_CONFIG + Protocolo.SEP)) {
                String salaId = msg.substring(Protocolo.OBTENER_SALA_CONFIG.length() + 1);
                sendMessage(Protocolo.RESPUESTA_SALA_CONFIG + Protocolo.SEP + state.getSalaConfigJson(salaId));
            } else if (msg.startsWith(Protocolo.CREAR_SALA + Protocolo.SEP)) {
                handleCrearSala(msg.substring(Protocolo.CREAR_SALA.length() + 1));
            } else if (msg.startsWith(Protocolo.CREAR_PELICULA + Protocolo.SEP)) {
                handleCrearPelicula(msg.substring(Protocolo.CREAR_PELICULA.length() + 1));
            } else if (msg.startsWith(Protocolo.CREAR_FUNCION + Protocolo.SEP)) {
                handleCrearFuncion(msg.substring(Protocolo.CREAR_FUNCION.length() + 1));
            } else if (msg.startsWith(Protocolo.LISTAR_BOLETAS + Protocolo.SEP)) {
                String funcionId = msg.substring(Protocolo.LISTAR_BOLETAS.length() + 1);
                sendMessage(Protocolo.RESPUESTA_BOLETAS + Protocolo.SEP + state.getBoletasJson(funcionId));
            } else if (msg.equals(Protocolo.LISTAR_TODAS_BOLETAS)) {
                sendMessage(Protocolo.RESPUESTA_BOLETAS + Protocolo.SEP + state.getTodasBoletasJson());
            } else if (msg.startsWith(Protocolo.CANCELAR_BOLETA + Protocolo.SEP)) {
                String boletaId = msg.substring(Protocolo.CANCELAR_BOLETA.length() + 1);
                state.cancelarBoleta(boletaId);
                sendMessage(Protocolo.OK);
                // NOTA: Para actualizar a los demás en tiempo real, lo ideal sería emitir un broadcast, 
                // pero state.cancelarBoleta ya libera los asientos. Faltaría un broadcast de cada asiento liberado:
                // Por simplicidad, los clientes actualizarán la matriz al volver a entrar, pero podemos emitir:
                // (Omitido por simplicidad de la prueba)
            } else if (msg.startsWith(Protocolo.ACTIVAR_PELICULA + Protocolo.SEP)) {
                state.activarPelicula(msg.substring(Protocolo.ACTIVAR_PELICULA.length() + 1));
                sendMessage(Protocolo.OK);
            } else if (msg.startsWith(Protocolo.DESACTIVAR_PELICULA + Protocolo.SEP)) {
                state.desactivarPelicula(msg.substring(Protocolo.DESACTIVAR_PELICULA.length() + 1));
                sendMessage(Protocolo.OK);
            } else if (msg.startsWith(Protocolo.ACTIVAR_SALA + Protocolo.SEP)) {
                state.activarSala(msg.substring(Protocolo.ACTIVAR_SALA.length() + 1));
                sendMessage(Protocolo.OK);
            } else if (msg.startsWith(Protocolo.DESACTIVAR_SALA + Protocolo.SEP)) {
                state.desactivarSala(msg.substring(Protocolo.DESACTIVAR_SALA.length() + 1));
                sendMessage(Protocolo.OK);
            } else if (msg.startsWith(Protocolo.ACTIVAR_FUNCION + Protocolo.SEP)) {
                state.activarFuncion(msg.substring(Protocolo.ACTIVAR_FUNCION.length() + 1));
                sendMessage(Protocolo.OK);
            } else if (msg.startsWith(Protocolo.DESACTIVAR_FUNCION + Protocolo.SEP)) {
                state.desactivarFuncion(msg.substring(Protocolo.DESACTIVAR_FUNCION.length() + 1));
                sendMessage(Protocolo.OK);
            } else if (msg.startsWith(Protocolo.ELIMINAR_FUNCION + Protocolo.SEP)) {
                state.eliminarFuncion(msg.substring(Protocolo.ELIMINAR_FUNCION.length() + 1));
                sendMessage(Protocolo.OK);
            } else {
                sendMessage(Protocolo.error("Comando desconocido: " + msg));
            }
        } catch (Exception e) {
            sendMessage(Protocolo.error(e.getMessage()));
        }
    }

    // // [RUTINA]: Intenta obtener el lock lógico de un asiento para evitar colisiones.
    private void handleSelect(String lockId) {
        synchronized (state) {
            if (!state.isAsientoLibre(lockId)) {
                sendMessage(Protocolo.error("El asiento ya está ocupado."));
                return;
            }
            if (!lockManager.tryLock(lockId, clientId)) {
                sendMessage(Protocolo.error("No se pudo bloquear."));
                return;
            }
        }
        sendMessage(Protocolo.OK);
        broadcaster.accept(Protocolo.seatUpdate(lockId, Protocolo.NET_LOCKED), clientId);
    }

    private void handleDeselect(String lockId) {
        if (!lockManager.isLockedBy(lockId, clientId)) {
            sendMessage(Protocolo.error("No tienes el lock."));
            return;
        }
        lockManager.releaseLock(lockId, clientId);
        sendMessage(Protocolo.OK);
        broadcaster.accept(Protocolo.seatUpdate(lockId, EstadoButaca.LIBRE.name()), clientId);
    }

    // // [RUTINA]: Efectúa la validación final de todos los locks antes de procesar el pago.
    private void handleBook(String payload) {
        // payload: lockIds|fechaReservada|metodoPago|dni
        String[] parts = payload.split("\\|");
        String lockIdsStr = parts[0];
        java.time.LocalDate fecha = java.time.LocalDate.parse(parts[1]);
        com.cine.dominio.MetodoPago metodo = com.cine.dominio.MetodoPago.valueOf(parts[2]);
        String dni = parts.length > 3 ? parts[3] : "noDNI";
        
        String[] lockIds = lockIdsStr.split(Protocolo.SEP_SUB);

        for (String lid : lockIds) {
            if (!lockManager.isLockedBy(lid.trim(), clientId)) {
                sendMessage(Protocolo.error("Falta lock en " + lid));
                return;
            }
        }

        com.cine.dominio.Boleta boleta;
        try {
            boleta = state.confirmarReserva(Arrays.asList(lockIds), fecha, metodo, dni);
        } catch (Exception ex) {
            for (String lid : lockIds) lockManager.releaseLock(lid.trim(), clientId);
            sendMessage(Protocolo.error(ex.getMessage()));
            return;
        }

        for (String lid : lockIds) lockManager.releaseLock(lid.trim(), clientId);
        
        // Serializar boleta a JSON simple para el cliente
        String asientosStr = String.join(",", boleta.getAsientos());
        String boletaJson = String.format("{\"id\":\"%s\",\"pelicula\":\"%s\",\"sala\":\"%s\",\"hora\":\"%s\",\"fechaReservada\":\"%s\",\"metodoPago\":\"%s\",\"asientos\":\"%s\",\"estado\":\"%s\",\"fechaEmision\":\"%s\"}", 
                      boleta.getId(), boleta.getPeliculaNombre(), boleta.getSalaNombre(), boleta.getHoraFuncion().toString(),
                      boleta.getFechaReservada().toString(), boleta.getMetodoPago().name(), asientosStr,
                      boleta.getEstado().name(), boleta.getFechaEmision().toString());
                      
        sendMessage(Protocolo.OK + Protocolo.SEP + boletaJson);

        for (String lid : lockIds) {
            broadcaster.accept(Protocolo.seatUpdate(lid.trim(), EstadoButaca.OCUPADO.name()), null);
        }
    }

    private void handleCrearSala(String payload) {
        String[] parts = payload.split(Protocolo.SEP);
        String id = parts[0]; // Puede ser vacio
        String nombre = parts[1];
        int filas = Integer.parseInt(parts[2]);
        int cols = Integer.parseInt(parts[3]);
        String[] matriz = parts[4].split(Protocolo.SEP_SUB);
        state.guardarSala(id, nombre, filas, cols, matriz);
        sendMessage(Protocolo.OK);
    }

    private void handleCrearPelicula(String payload) {
        String[] parts = payload.split(Protocolo.SEP);
        String nombre = parts[0];
        int duracion = Integer.parseInt(parts[1]);
        state.crearPelicula(nombre, duracion);
        sendMessage(Protocolo.OK);
    }

    private void handleCrearFuncion(String payload) {
        String[] parts = payload.split(Protocolo.SEP, 3);
        String salaId = parts[0];
        String peliculaId = parts[1];
        java.time.LocalTime hora = java.time.LocalTime.parse(parts[2]);
        state.crearFuncion(salaId, peliculaId, hora);
        sendMessage(Protocolo.OK);
    }
    

    // // [RUTINA]: Procedimiento de desconexión segura (limpieza de recursos y liberación de locks).
    private void onDisconnect(boolean serverInitiated) {
        if (!disconnected.compareAndSet(false, true)) return;

        List<String> released = lockManager.releaseAllLocksOf(clientId);
        for (String sid : released) {
            broadcaster.accept(Protocolo.seatUpdate(sid, EstadoButaca.LIBRE.name()), clientId);
        }

        try { socket.close(); } catch (IOException ignored) {}
        System.out.printf("[Server] Cliente %s desconectado. Locks: %s%n", clientId, released);
    }
}
