package com.cine.servidor;

import com.cine.dominio.Butaca;
import com.cine.dominio.EstadoButaca;
import com.cine.dominio.boletos.Boleto;
import com.cine.compartido.Protocolo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * Maneja la conexión de UN cliente TCP en su propio hilo.
 *
 * NIVEL 2 — Temporizador de Sesión (Bloqueo Pesimista con Tiempo de Vida):
 * ─────────────────────────────────────────────────────────────────────────
 * Cada sesión de cliente tiene un máximo de 5 minutos (DURACION_SESION_MS).
 *
 * El servidor envía TIEMPO_SESION:{segundos} cada segundo al cliente para que
 * éste muestre un contador visible. Cuando el tiempo se agota:
 *
 *   1. El servidor envía SESION_EXPIRADA al cliente.
 *   2. Libera todos los locks pesimistas del cliente (butacas no compradas).
 *   3. Notifica a todos los demás clientes con ACTUALIZACION_BUTACA:...:FREE.
 *   4. Cierra la conexión.
 *
 * Si el cliente cierra manualmente antes del tiempo, onDisconnect() también
 * libera todos sus locks (esto ya existía en el nivel anterior).
 *
 * Si el cliente confirma una compra exitosa, el servidor cierra la sesión
 * enviando SESION_EXPIRADA (con diferente motivo visual en la UI).
 */
public class ManejadorCliente implements Runnable {

    private final String clientId = UUID.randomUUID().toString().substring(0, 8);

    private final Socket socket;
    private final EstadoServidor state;
    private final ManejadorBloqueoButacas lockManager;
    private final BiConsumer<String, String> broadcaster;

    private PrintWriter out;

    /** Evita que onDisconnect se llame dos veces */
    private final AtomicBoolean disconnected = new AtomicBoolean(false);

    /** Pool del temporizador de sesión (1 hilo: tick + expiración) */
    private final ScheduledExecutorService sessionScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "session-timer-" + clientId);
                t.setDaemon(true);
                return t;
            });

    private final long sessionStartMs = System.currentTimeMillis();
    private ScheduledFuture<?> tickTask;
    private ScheduledFuture<?> expiryTask;

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

    @Override
    public void run() {
        System.out.printf("[Server] Cliente conectado: %s (%s)%n",
                clientId, socket.getRemoteSocketAddress());
        try (
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            PrintWriter pw = new PrintWriter(socket.getOutputStream(), true)
        ) {
            this.out = pw;

            // Iniciar el ticker de tiempo restante (cada 1 segundo)
            tickTask = sessionScheduler.scheduleAtFixedRate(
                    this::sendSessionTick, 0, 1, TimeUnit.SECONDS);

            // Iniciar el temporizador de expiración de sesión
            expiryTask = sessionScheduler.schedule(
                    this::expireSession, Protocolo.DURACION_SESION_MS, TimeUnit.MILLISECONDS);

            String line;
            while ((line = in.readLine()) != null) {
                handleMessage(line.trim());
            }
        } catch (IOException e) {
            if (!disconnected.get()) {
                System.out.printf("[Server] Cliente desconectado: %s%n", clientId);
            }
        } finally {
            onDisconnect(false);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Temporizador de sesión
    // ─────────────────────────────────────────────────────────────────────

    /** Envía el tiempo restante de sesión al cliente cada segundo. */
    private void sendSessionTick() {
        long elapsed = System.currentTimeMillis() - sessionStartMs;
        long remainingSeconds = Math.max(0,
                (Protocolo.DURACION_SESION_MS - elapsed) / 1000);
        sendMessage(Protocolo.TIEMPO_SESION + Protocolo.SEP + remainingSeconds);
    }

    /** Llamado cuando el temporizador de sesión llega a 0. */
    private void expireSession() {
        System.out.printf("[Server] Sesión expirada (timeout): %s%n", clientId);
        sendMessage(Protocolo.SESION_EXPIRADA);
        onDisconnect(true);
    }

    /**
     * Llamado también desde handleBook cuando la compra es exitosa:
     * cierra la sesión del cliente y libera lo que no fue comprado.
     */
    private void closeSessionAfterPurchase() {
        System.out.printf("[Server] Sesión cerrada por compra completada: %s%n", clientId);
        sendMessage(Protocolo.SESION_EXPIRADA);
        onDisconnect(true);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Despacho de comandos
    // ─────────────────────────────────────────────────────────────────────

    private void handleMessage(String msg) {
        if (msg.isEmpty()) return;
        System.out.printf("[Server][%s] ← %s%n", clientId, msg);

        if (msg.equals(Protocolo.PING)) {
            sendMessage(Protocolo.PONG);
        } else if (msg.equals(Protocolo.OBTENER_SALA)) {
            handleGetRoom();
        } else if (msg.startsWith(Protocolo.SELECCIONAR + Protocolo.SEP)) {
            handleSelect(msg.substring(Protocolo.SELECCIONAR.length() + 1));
        } else if (msg.startsWith(Protocolo.DESELECCIONAR + Protocolo.SEP)) {
            handleDeselect(msg.substring(Protocolo.DESELECCIONAR.length() + 1));
        } else if (msg.startsWith(Protocolo.RESERVAR + Protocolo.SEP)) {
            handleBook(msg.substring(Protocolo.RESERVAR.length() + 1));
        } else if (msg.equals(Protocolo.LISTAR_CINES)) {
            sendMessage(Protocolo.RESPUESTA_CINES + Protocolo.SEP + state.getCinesJson());
        } else if (msg.equals(Protocolo.LISTAR_PELICULAS)) {
            sendMessage(Protocolo.RESPUESTA_PELICULAS + Protocolo.SEP + state.getPeliculasJson());
        } else if (msg.equals(Protocolo.LISTAR_SALAS)) {
            sendMessage(Protocolo.RESPUESTA_SALAS + Protocolo.SEP + state.getSalasJson());
        } else if (msg.equals(Protocolo.LISTAR_FUNCIONES)) {
            sendMessage(Protocolo.RESPUESTA_FUNCIONES + Protocolo.SEP + state.getFuncionesJson());
        } else if (msg.startsWith(Protocolo.OBTENER_DETALLE_FUNCION + Protocolo.SEP)) {
            String id = msg.substring(Protocolo.OBTENER_DETALLE_FUNCION.length() + 1);
            sendMessage(Protocolo.RESPUESTA_DETALLE_FUNCION + Protocolo.SEP + state.getFuncionDetalleJson(id));
        } else if (msg.startsWith(Protocolo.CREAR_CINE + Protocolo.SEP)) {
            handleCrearCine(msg.substring(Protocolo.CREAR_CINE.length() + 1));
        } else if (msg.startsWith(Protocolo.CREAR_PELICULA + Protocolo.SEP)) {
            handleCrearPelicula(msg.substring(Protocolo.CREAR_PELICULA.length() + 1));
        } else if (msg.startsWith(Protocolo.CREAR_SALA + Protocolo.SEP)) {
            handleCrearSala(msg.substring(Protocolo.CREAR_SALA.length() + 1));
        } else if (msg.startsWith(Protocolo.CREAR_FUNCION + Protocolo.SEP)) {
            handleCrearFuncion(msg.substring(Protocolo.CREAR_FUNCION.length() + 1));
        } else {
            sendMessage(Protocolo.error("Comando desconocido: " + msg));
        }
    }

    private void handleGetRoom() {
        sendMessage(Protocolo.roomState(state.getRoomStateJson()));
    }

    private void handleSelect(String seatId) {
        Butaca butaca = state.findSeatById(seatId);
        if (butaca == null) { sendMessage(Protocolo.error("Butaca no encontrada: " + seatId)); return; }

        synchronized (state) {
            if (butaca.getEstado() != EstadoButaca.FREE) {
                if (butaca.getEstado() == EstadoButaca.SELECTED
                        && !lockManager.isLockedBy(seatId, clientId)) {
                    sendMessage(Protocolo.error("Butaca " + seatId + " bloqueada por otro cliente"));
                } else {
                    sendMessage(Protocolo.error("Butaca " + seatId + " no está libre"));
                }
                return;
            }
            if (!lockManager.tryLock(seatId, clientId)) {
                sendMessage(Protocolo.error("No se pudo bloquear: " + seatId));
                return;
            }
            butaca.seleccionar();
        }
        sendMessage(Protocolo.OK);
        broadcaster.accept(Protocolo.seatUpdate(seatId, Protocolo.NET_LOCKED), clientId);
    }

    private void handleDeselect(String seatId) {
        Butaca butaca = state.findSeatById(seatId);
        if (butaca == null) { sendMessage(Protocolo.error("Butaca no encontrada: " + seatId)); return; }

        synchronized (state) {
            if (!lockManager.isLockedBy(seatId, clientId)) {
                sendMessage(Protocolo.error("No tienes el lock sobre " + seatId));
                return;
            }
            lockManager.releaseLock(seatId, clientId);
            if (butaca.getEstado() == EstadoButaca.SELECTED) butaca.liberar();
        }
        sendMessage(Protocolo.OK);
        broadcaster.accept(Protocolo.seatUpdate(seatId, EstadoButaca.FREE.name()), clientId);
    }

    private void handleBook(String payload) {
        String[] parts = payload.split(Protocolo.SEP, 2);
        if (parts.length < 1) { sendMessage(Protocolo.error("Formato inválido")); return; }

        String[] seatIds = parts[0].split(Protocolo.SEP_SUB);
        String dni = (parts.length == 2) ? parts[1].trim() : "";

        for (String sid : seatIds) {
            if (!lockManager.isLockedBy(sid.trim(), clientId)) {
                sendMessage(Protocolo.error("No tienes el lock sobre " + sid));
                return;
            }
        }

        List<Boleto> issued;
        try {
            issued = state.confirmPurchase(
                    Arrays.stream(seatIds).map(String::trim).toList(),
                    dni.isEmpty() ? null : dni);
        } catch (IllegalStateException ex) {
            sendMessage(Protocolo.error(ex.getMessage()));
            return;
        }

        for (String sid : seatIds) lockManager.releaseLock(sid.trim(), clientId);

        // Confirmar al cliente con código de referencia
        sendMessage(Protocolo.OK + Protocolo.SEP + issued.get(0).getReferenceCode());

        // Notificar a todos que estas butacas están BOOKED
        for (String sid : seatIds) {
            broadcaster.accept(Protocolo.seatUpdate(sid.trim(), EstadoButaca.BOOKED.name()), null);
        }

        // Cerrar la sesión después de la compra (en hilo separado para no bloquear el envío)
        sessionScheduler.schedule(this::closeSessionAfterPurchase, 200, TimeUnit.MILLISECONDS);
    }

    private void handleCrearCine(String payload) {
        String[] parts = payload.split(Protocolo.SEP, 3);
        if (parts.length < 3) {
            sendMessage(Protocolo.error("Formato inválido para CREAR_CINE"));
            return;
        }
        try {
            state.crearCine(parts[0], parts[1], parts[2]);
            sendMessage(Protocolo.OK);
        } catch (Exception e) {
            sendMessage(Protocolo.error(e.getMessage()));
        }
    }

    private void handleCrearSala(String payload) {
        try {
            // formato: cine_id:nombre:filas:columnas:t1,t2,t3...
            String[] parts = payload.split(Protocolo.SEP);
            if (parts.length >= 5) {
                String cineId = parts[0];
                String nombre = parts[1];
                int filas = Integer.parseInt(parts[2]);
                int cols = Integer.parseInt(parts[3]);
                String[] matriz = parts[4].split(Protocolo.SEP_SUB);
                state.crearSala(cineId, nombre, filas, cols, matriz);
                sendMessage(Protocolo.OK);
            } else {
                sendMessage(Protocolo.error("Formato inválido para CREAR_SALA"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(Protocolo.error("Excepción en CREAR_SALA: " + e.getMessage()));
        }
    }

    private void handleCrearPelicula(String payload) {
        // formato: titulo,duracion,clasificacion
        String[] parts = payload.split(Protocolo.SEP_SUB);
        if (parts.length >= 3) {
            String titulo = parts[0];
            int duracion = Integer.parseInt(parts[1]);
            String clasif = parts[2];
            state.crearPelicula(titulo, duracion, clasif);
            sendMessage(Protocolo.OK);
        } else {
            sendMessage(Protocolo.error("Formato inválido para CREAR_PELICULA"));
        }
    }

    private void handleCrearFuncion(String payload) {
        // formato: salaId,peliculaId,horaInicio,formato,precioBase
        String[] parts = payload.split(Protocolo.SEP_SUB);
        if (parts.length >= 5) {
            String salaId = parts[0];
            String peliculaId = parts[1];
            java.time.LocalDateTime hora = java.time.LocalDateTime.parse(parts[2]);
            com.cine.dominio.FormatoFuncion formato = com.cine.dominio.FormatoFuncion.valueOf(parts[3]);
            double precio = Double.parseDouble(parts[4]);
            try {
                state.crearFuncion(salaId, peliculaId, hora, formato, precio);
                sendMessage(Protocolo.OK);
            } catch (Exception e) {
                sendMessage(Protocolo.error("Error al crear función: " + e.getMessage()));
            }
        } else {
            sendMessage(Protocolo.error("Formato inválido para CREAR_FUNCION"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Limpieza al desconectar
    // ─────────────────────────────────────────────────────────────────────

    private void onDisconnect(boolean serverInitiated) {
        if (!disconnected.compareAndSet(false, true)) return; // ya procesado

        // Detener timers
        if (tickTask   != null) tickTask.cancel(true);
        if (expiryTask != null) expiryTask.cancel(true);
        sessionScheduler.shutdownNow();

        // Liberar todos los locks del cliente (butacas no compradas)
        List<String> released = lockManager.releaseAllLocksOf(clientId);
        for (String sid : released) {
            Butaca butaca = state.findSeatById(sid);
            if (butaca != null && butaca.getEstado() == EstadoButaca.SELECTED) {
                synchronized (state) {
                    try { butaca.liberar(); } catch (Exception ignored) {}
                }
            }
            broadcaster.accept(Protocolo.seatUpdate(sid, EstadoButaca.FREE.name()), clientId);
        }

        // Cerrar socket
        try { socket.close(); } catch (IOException ignored) {}

        System.out.printf("[Server] Cliente %s desconectado %s. Locks liberados: %s%n",
                clientId, serverInitiated ? "(por servidor)" : "(por cliente)", released);
    }
}
