package com.cine.cliente;

import com.cine.compartido.Protocolo;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ClienteRed {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;
    private volatile boolean running = false;
    private final BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();

    private BiConsumer<String, String> onSeatUpdate;
    private Consumer<String> onDisconnected;

    public void setOnSeatUpdate(BiConsumer<String, String> onSeatUpdate) {
        this.onSeatUpdate = onSeatUpdate;
    }

    public void setCallbacks(BiConsumer<String, String> onSeatUpdate,
                             Consumer<String> onDisconnected) {
        this.onSeatUpdate     = onSeatUpdate;
        this.onDisconnected   = onDisconnected;
    }

    public void connect(BiConsumer<String, String> onSeatUpdate,
                        Consumer<String> onDisconnected) throws IOException {
        setCallbacks(onSeatUpdate, onDisconnected);
        doConnect();
    }

    // // [RUTINA]: Abre (o reabre) la conexión TCP con el servidor.
    private void doConnect() throws IOException {
        // Cerrar recursos anteriores de forma segura
        running = false;
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}
        responseQueue.clear();

        socket = new Socket(Protocolo.HOST, Protocolo.PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        running = true;

        listenerThread = new Thread(this::listenLoop, "net-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
        System.out.println("[ClienteRed] Conectado a " + Protocolo.HOST + ":" + Protocolo.PORT);
    }

    // // [RUTINA]: Reconecta el socket sin necesidad de pasar callbacks de nuevo.
    public void reconnect() throws IOException {
        doConnect();
    }

    private void listenLoop() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                if (line.startsWith(Protocolo.ACTUALIZACION_BUTACA + Protocolo.SEP)) {
                    String[] parts = line.split(Protocolo.SEP, 3);
                    if (parts.length == 3) {
                        String seatId = parts[1];
                        String estado = parts[2];
                        Platform.runLater(() -> {
                            if (onSeatUpdate != null) onSeatUpdate.accept(seatId, estado);
                        });
                    }
                } else {
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

    // --- Funciones del cliente ---

    public String getRoomState(String funcionId, String fechaStr) throws IOException {
        String payload = funcionId + Protocolo.SEP_SUB + fechaStr;
        String resp = sendAndWait(Protocolo.OBTENER_SALA + Protocolo.SEP + payload);
        if (resp.startsWith(Protocolo.ESTADO_SALA + Protocolo.SEP)) {
            return resp.substring(Protocolo.ESTADO_SALA.length() + 1);
        }
        throw new IOException("Respuesta inesperada: " + resp);
    }

    /**
     * Solicita el conteo de butacas para una función específica.
     * @return int[] { total, libres, ocupados }, o null si hay error.
     */
    public int[] getConteoButacas(String funcionId, String fechaStr) throws IOException {
        String payload = funcionId + Protocolo.SEP_SUB + fechaStr;
        String resp = sendAndWait(Protocolo.CONTEO_BUTACAS + Protocolo.SEP + payload);
        if (resp.startsWith(Protocolo.RESPUESTA_CONTEO + Protocolo.SEP)) {
            String datos = resp.substring(Protocolo.RESPUESTA_CONTEO.length() + 1);
            String[] partes = datos.split("\\|");
            return new int[] {
                Integer.parseInt(partes[0]),  // total
                Integer.parseInt(partes[1]),  // libres
                Integer.parseInt(partes[2])   // ocupados
            };
        }
        throw new IOException("Respuesta inesperada al pedir conteo: " + resp);
    }

    public boolean selectSeat(String lockId) throws IOException {
        String resp = sendAndWait(Protocolo.SELECCIONAR + Protocolo.SEP + lockId);
        return resp.startsWith(Protocolo.OK);
    }

    public boolean deselectSeat(String lockId) throws IOException {
        String resp = sendAndWait(Protocolo.DESELECCIONAR + Protocolo.SEP + lockId);
        return resp.startsWith(Protocolo.OK);
    }

    public String bookSeats(List<String> lockIds, java.time.LocalDate fechaReservada, com.cine.dominio.MetodoPago metodoPago, String dni) throws IOException {
        String seatsStr = String.join(Protocolo.SEP_SUB, lockIds);
        String payload = seatsStr + "|" + fechaReservada.toString() + "|" + metodoPago.name() + "|" + (dni != null && !dni.isBlank() ? dni : "noDNI");
        String resp = sendAndWait(Protocolo.RESERVAR + Protocolo.SEP + payload);
        if (resp.startsWith(Protocolo.OK)) {
            String[] parts = resp.split(Protocolo.SEP, 2);
            return parts.length == 2 ? parts[1] : "OK";
        }
        return null; // o lanzar error
    }
    
    public String listarBoletas(String funcionId) throws IOException {
        String resp = sendAndWait(Protocolo.LISTAR_BOLETAS + Protocolo.SEP + funcionId);
        if (resp.startsWith(Protocolo.RESPUESTA_BOLETAS + Protocolo.SEP)) {
            return resp.substring(Protocolo.RESPUESTA_BOLETAS.length() + 1);
        }
        return "[]";
    }

    public String listarTodasLasBoletas() throws IOException {
        String resp = sendAndWait(Protocolo.LISTAR_TODAS_BOLETAS);
        if (resp.startsWith(Protocolo.RESPUESTA_BOLETAS + Protocolo.SEP)) {
            return resp.substring(Protocolo.RESPUESTA_BOLETAS.length() + 1);
        }
        return "[]";
    }

    public boolean cancelarBoleta(String boletaId) throws IOException {
        String resp = sendAndWait(Protocolo.CANCELAR_BOLETA + Protocolo.SEP + boletaId);
        return resp.startsWith(Protocolo.OK);
    }

    public String listarSalas() throws IOException {
        String resp = sendAndWait(Protocolo.LISTAR_SALAS);
        if (resp.startsWith(Protocolo.RESPUESTA_SALAS + Protocolo.SEP)) {
            return resp.substring(Protocolo.RESPUESTA_SALAS.length() + 1);
        }
        return "[]";
    }

    public String obtenerSalaConfig(String salaId) throws IOException {
        String resp = sendAndWait(Protocolo.OBTENER_SALA_CONFIG + Protocolo.SEP + salaId);
        if (resp.startsWith(Protocolo.RESPUESTA_SALA_CONFIG + Protocolo.SEP)) {
            return resp.substring(Protocolo.RESPUESTA_SALA_CONFIG.length() + 1);
        }
        return null;
    }

    public String listarPeliculas() throws IOException {
        String resp = sendAndWait(Protocolo.LISTAR_PELICULAS);
        if (resp.startsWith(Protocolo.RESPUESTA_PELICULAS + Protocolo.SEP)) {
            return resp.substring(Protocolo.RESPUESTA_PELICULAS.length() + 1);
        }
        return "[]";
    }

    public String listarFunciones(String fechaStr) throws IOException {
        String resp = sendAndWait(Protocolo.LISTAR_FUNCIONES + Protocolo.SEP + fechaStr);
        if (resp.startsWith(Protocolo.RESPUESTA_FUNCIONES + Protocolo.SEP)) {
            return resp.substring(Protocolo.RESPUESTA_FUNCIONES.length() + 1);
        }
        return "[]";
    }

    public boolean crearSala(String id, String nombre, int filas, int columnas, String matrizCSV) throws IOException {
        String payload = (id == null ? "" : id) + Protocolo.SEP + nombre + Protocolo.SEP + filas + Protocolo.SEP + columnas + Protocolo.SEP + matrizCSV;
        String resp = sendAndWait(Protocolo.CREAR_SALA + Protocolo.SEP + payload);
        return resp.startsWith(Protocolo.OK);
    }

    public boolean crearPelicula(String titulo, int duracion) throws IOException {
        String payload = titulo + Protocolo.SEP + duracion;
        String resp = sendAndWait(Protocolo.CREAR_PELICULA + Protocolo.SEP + payload);
        return resp.startsWith(Protocolo.OK);
    }

    public boolean crearFuncion(String salaId, String peliculaId, LocalDateTime horaInicio) throws IOException {
        String payload = salaId + Protocolo.SEP + peliculaId + Protocolo.SEP + horaInicio.toString();
        String resp = sendAndWait(Protocolo.CREAR_FUNCION + Protocolo.SEP + payload);
        return resp.startsWith(Protocolo.OK);
    }

    public void disconnect() {
        running = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
