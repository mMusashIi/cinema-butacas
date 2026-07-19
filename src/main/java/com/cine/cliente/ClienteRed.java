package com.cine.cliente;

import com.cine.compartido.Protocolo;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Gestiona la conexión TCP del cliente JavaFX con el servidor.
 *
 * DISEÑO: Un hilo listener lee TODOS los mensajes del socket.
 * ─────────────────────────────────────────────────────────────────────────
 * Los mensajes se clasifican en dos canales:
 *
 *  1. PUSH (ACTUALIZACION_BUTACA): se despachan inmediatamente al callback de UI
 *     vía Platform.runLater(). No bloquean ningún hilo.
 *
 *  2. RESPUESTAS SÍNCRONAS (OK, ERROR, ESTADO_SALA, PONG): se colocan en
 *     una BlockingQueue. Los métodos sendAndWait() consumen de esta cola
 *     con un timeout de 10 segundos.
 *
 * Esto elimina la condición de carrera anterior donde el listener y los
 * métodos síncronos competían por leer del mismo BufferedReader.
 */
public class ClienteRed {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;
    private volatile boolean running = false;

    /** Cola de respuestas síncronas pendientes (listener → métodos de comando) */
    private final BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();

    /** Callback: (seatId, netStatus) → llamado en hilo JavaFX al recibir ACTUALIZACION_BUTACA */
    private BiConsumer<String, String> onSeatUpdate;

    /** Callback: (segundosRestantes) → cuenta regresiva de la sesión */
    private Consumer<Long> onSessionTime;

    /** Callback: () → la sesión ha expirado (o compra completada), cerrar ventana */
    private Runnable onSessionExpired;

    /** Callback: (mensaje) → pérdida de conexión */
    private Consumer<String> onDisconnected;

    public void connect(BiConsumer<String, String> onSeatUpdate,
                        Consumer<Long> onSessionTime,
                        Runnable onSessionExpired,
                        Consumer<String> onDisconnected) throws IOException {
        this.onSeatUpdate    = onSeatUpdate;
        this.onSessionTime   = onSessionTime;
        this.onSessionExpired = onSessionExpired;
        this.onDisconnected  = onDisconnected;

        socket = new Socket(Protocolo.HOST, Protocolo.PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        running = true;

        listenerThread = new Thread(this::listenLoop, "net-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();

        System.out.println("[ClienteRed] Conectado a " + Protocolo.HOST + ":" + Protocolo.PORT);
    }

    /**
     * Hilo exclusivo de lectura del socket.
     * Clasifica cada mensaje y lo envía al canal correcto.
     */
    private void listenLoop() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                if (line.startsWith(Protocolo.ACTUALIZACION_BUTACA + Protocolo.SEP)) {
                    // Push: butaca actualizada → UI
                    String[] parts = line.split(Protocolo.SEP, 3);
                    if (parts.length == 3) {
                        String seatId = parts[1];
                        String estado = parts[2];
                        Platform.runLater(() -> {
                            if (onSeatUpdate != null) onSeatUpdate.accept(seatId, estado);
                        });
                    }
                } else if (line.startsWith(Protocolo.TIEMPO_SESION + Protocolo.SEP)) {
                    // Push: tick del contador de sesión
                    try {
                        long secs = Long.parseLong(line.substring(Protocolo.TIEMPO_SESION.length() + 1));
                        Platform.runLater(() -> {
                            if (onSessionTime != null) onSessionTime.accept(secs);
                        });
                    } catch (NumberFormatException ignored) {}
                } else if (line.equals(Protocolo.SESION_EXPIRADA)) {
                    // Push: sesión terminada (timeout o compra exitosa)
                    Platform.runLater(() -> {
                        if (onSessionExpired != null) onSessionExpired.run();
                    });
                } else {
                    // Respuesta síncrona → cola
                    responseQueue.offer(line);
                }
            }
        } catch (IOException e) {
            if (running) {
                System.out.println("[ClienteRed] Conexión perdida: " + e.getMessage());
                final Consumer<String> cb = onDisconnected;
                if (cb != null) Platform.runLater(() -> cb.accept("Conexión perdida con el servidor"));
            }
        }
    }

    /**
     * Envía un comando y espera la respuesta síncrona de la cola (10s timeout).
     * Sincronizado para que solo un comando esté pendiente a la vez.
     */
    public synchronized String sendAndWait(String command) throws IOException {
        out.println(command);
        try {
            String resp = responseQueue.poll(10, TimeUnit.SECONDS);
            if (resp == null) throw new IOException("Timeout: no hubo respuesta del servidor");
            return resp;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrumpido esperando respuesta");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // API pública
    // ─────────────────────────────────────────────────────────────────────

    public String getRoomState(String funcionId) throws IOException {
        String req = funcionId != null ? Protocolo.OBTENER_SALA + Protocolo.SEP + funcionId : Protocolo.OBTENER_SALA;
        String resp = sendAndWait(req);
        if (resp.startsWith(Protocolo.ESTADO_SALA + Protocolo.SEP)) {
            return resp.substring(Protocolo.ESTADO_SALA.length() + 1);
        }
        throw new IOException("Respuesta inesperada a OBTENER_SALA: " + resp);
    }

    public boolean selectSeat(String seatId) throws IOException {
        String resp = sendAndWait(Protocolo.SELECCIONAR + Protocolo.SEP + seatId);
        return resp.startsWith(Protocolo.OK);
    }

    /** Igual que selectSeat pero devuelve la línea cruda del servidor (OK o ERROR:msg). */
    public String selectSeatRaw(String seatId) throws IOException {
        return sendAndWait(Protocolo.SELECCIONAR + Protocolo.SEP + seatId);
    }

    public boolean deselectSeat(String seatId) throws IOException {
        String resp = sendAndWait(Protocolo.DESELECCIONAR + Protocolo.SEP + seatId);
        return resp.startsWith(Protocolo.OK);
    }

    public String bookSeats(List<String> seatIds, String dni, String funcionId) throws IOException {
        String seatsStr = String.join(Protocolo.SEP_SUB, seatIds);
        String dniStr = (dni != null && !dni.trim().isEmpty()) ? dni.trim() : "";
        String funcIdStr = funcionId != null ? funcionId : "";
        String resp = sendAndWait(Protocolo.RESERVAR + Protocolo.SEP + seatsStr + Protocolo.SEP + dniStr + Protocolo.SEP + funcIdStr);
        if (resp.startsWith(Protocolo.OK)) {
            String[] parts = resp.split(Protocolo.SEP, 2);
            return parts.length == 2 ? parts[1] : "TK-OK";
        }
        return null;
    }

    public boolean crearSala(String cineId, String nombre, int filas, int columnas, String matrizCSV) throws IOException {
        String payload = cineId + Protocolo.SEP + nombre + Protocolo.SEP + filas + Protocolo.SEP + columnas + Protocolo.SEP + matrizCSV;
        String resp = sendAndWait(Protocolo.CREAR_SALA + Protocolo.SEP + payload);
        return resp.startsWith(Protocolo.OK);
    }

    public String listarCines() throws IOException {
        String resp = sendAndWait(Protocolo.LISTAR_CINES);
        if (resp.startsWith(Protocolo.RESPUESTA_CINES + Protocolo.SEP)) {
            return resp.substring(Protocolo.RESPUESTA_CINES.length() + 1);
        }
        return "[]";
    }

    public boolean crearCine(String nombre, String direccion, String ciudad) throws IOException {
        String payload = nombre + Protocolo.SEP + direccion + Protocolo.SEP + ciudad;
        String resp = sendAndWait(Protocolo.CREAR_CINE + Protocolo.SEP + payload);
        return resp.startsWith(Protocolo.OK);
    }

    public String listarFunciones() throws IOException {
        String resp = sendAndWait(Protocolo.LISTAR_FUNCIONES);
        if (resp.startsWith(Protocolo.RESPUESTA_FUNCIONES + Protocolo.SEP)) {
            return resp.substring(Protocolo.RESPUESTA_FUNCIONES.length() + 1);
        }
        throw new IOException("Respuesta inesperada: " + resp);
    }

    public String obtenerDetalleFuncion(String funcionId) throws IOException {
        String resp = sendAndWait(Protocolo.OBTENER_DETALLE_FUNCION + Protocolo.SEP + funcionId);
        if (resp.startsWith(Protocolo.RESPUESTA_DETALLE_FUNCION + Protocolo.SEP)) {
            return resp.substring(Protocolo.RESPUESTA_DETALLE_FUNCION.length() + 1);
        }
        throw new IOException("Respuesta inesperada: " + resp);
    }

    public String listarPeliculas() throws IOException {
        String resp = sendAndWait(Protocolo.LISTAR_PELICULAS);
        if (resp.startsWith(Protocolo.RESPUESTA_PELICULAS + Protocolo.SEP)) {
            return resp.substring(Protocolo.RESPUESTA_PELICULAS.length() + 1);
        }
        throw new IOException("Respuesta inesperada: " + resp);
    }

    public String listarSalas() throws IOException {
        String resp = sendAndWait(Protocolo.LISTAR_SALAS);
        if (resp.startsWith(Protocolo.RESPUESTA_SALAS + Protocolo.SEP)) {
            return resp.substring(Protocolo.RESPUESTA_SALAS.length() + 1);
        }
        throw new IOException("Respuesta inesperada: " + resp);
    }

    public String listarReservas() throws IOException {
        String resp = sendAndWait(Protocolo.LISTAR_RESERVAS);
        if (resp.startsWith(Protocolo.RESPUESTA_RESERVAS + Protocolo.SEP)) {
            return resp.substring(Protocolo.RESPUESTA_RESERVAS.length() + 1);
        }
        return "[]";
    }

    public boolean crearPelicula(String titulo, int duracion, String clasificacion) throws IOException {
        String payload = titulo + Protocolo.SEP_SUB + duracion + Protocolo.SEP_SUB + clasificacion;
        String resp = sendAndWait(Protocolo.CREAR_PELICULA + Protocolo.SEP + payload);
        return resp.startsWith(Protocolo.OK);
    }

    public boolean crearFuncion(String salaId, String peliculaId, java.time.LocalDateTime horaInicio, com.cine.dominio.FormatoFuncion formato, double precioBase) throws IOException {
        String payload = salaId + Protocolo.SEP_SUB + peliculaId + Protocolo.SEP_SUB + horaInicio.toString() + Protocolo.SEP_SUB + formato.name() + Protocolo.SEP_SUB + precioBase;
        String resp = sendAndWait(Protocolo.CREAR_FUNCION + Protocolo.SEP + payload);
        return resp.startsWith(Protocolo.OK);
    }

    public void disconnect() {
        running = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
