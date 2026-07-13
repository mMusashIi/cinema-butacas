package com.cine.servidor;

import com.cine.compartido.Protocolo;
import com.cine.servidor.bd.ConexionBD;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servidor TCP central del sistema de butacas.
 *
 * Paradigmas demostrados en esta clase:
 * ─────────────────────────────────────────────────────────────────────────
 * 1. Thread-Per-Connection: cada cliente aceptado corre en su propio hilo
 *    dentro del ExecutorService (pool con límite de 50 conexiones).
 *
 * 2. Broadcast concurrente: cuando un cliente muta un butaca, el servidor
 *    notifica en tiempo real a TODOS los clientes conectados (push).
 *    El mapa de clientes (ConcurrentHashMap) garantiza que la iteración
 *    durante el broadcast es thread-safe.
 *
 * 3. Bloqueo pesimista con timeout: delegado a ManejadorBloqueoButacas. Cuando un
 *    lock expira, el servidor llama onLockExpired que hace broadcast FREE.
 *
 * Uso:
 *   mvn exec:java -Dexec.mainClass=com.cine.servidor.ServidorCine
 *
 * El servidor escucha en el puerto 9090 (configurable en Protocolo).
 */
public class ServidorCine {

    /** Estado compartido del dominio (sala, función, precios) */
    private final EstadoServidor state = new EstadoServidor();

    /** Pool de hilos — uno por cliente conectado */
    private final ExecutorService threadPool = Executors.newFixedThreadPool(50);

    /** Mapa de clientes activos: clientId → handler */
    private final Map<String, ManejadorCliente> clients = new ConcurrentHashMap<>();

    /** Gestor de locks pesimistas — se le pasa el callback de expiración */
    private final ManejadorBloqueoButacas lockManager = new ManejadorBloqueoButacas(this::onLockExpired);

    public void start() throws IOException {
        System.out.printf("[Server] Iniciando en puerto %d…%n", Protocolo.PORT);
        System.out.printf("[Server] Sala: %s | Función: %s%n",
                state.getSala().getNombre(), state.getFuncion());
        System.out.println("[Server] Esperando clientes. Presiona Ctrl+C para detener.");

        try (ServerSocket serverSocket = new ServerSocket(Protocolo.PORT)) {
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();

                ManejadorCliente handler = new ManejadorCliente(
                        socket, state, lockManager, this::broadcast);

                clients.put(handler.getClientId(), handler);
                threadPool.execute(() -> {
                    try {
                        handler.run();
                    } finally {
                        // Eliminar del mapa al terminar
                        clients.remove(handler.getClientId());
                        System.out.printf("[Server] Clientes activos: %d%n", clients.size());
                    }
                });
            }
        }
    }

    /**
     * Envía un mensaje a todos los clientes conectados.
     *
     * @param msg             Mensaje a enviar (línea de texto)
     * @param excludeClientId Si no es null, excluye al cliente con ese ID del broadcast
     *                        (útil para no notificar al cliente que inició el cambio
     *                        sobre ciertos eventos)
     */
    private void broadcast(String msg, String excludeClientId) {
        clients.forEach((id, handler) -> {
            if (!id.equals(excludeClientId)) {
                handler.sendMessage(msg);
            }
        });
    }

    /**
     * Callback invocado por ManejadorBloqueoButacas cuando un lock expira por timeout.
     * Notifica a TODOS los clientes que la butaca volvió a FREE.
     */
    private void onLockExpired(String seatId) {
        System.out.printf("[Server] Lock expirado → broadcast FREE para %s%n", seatId);
        broadcast(Protocolo.seatUpdate(seatId, "FREE"), null);
    }

    public static void main(String[] args) throws IOException {
        // 1. Inicializar Base de Datos SQLite (Nivel 3)
        ConexionBD.inicializarEstructura();
        
        // 2. Iniciar servidor TCP
        new ServidorCine().start();
    }
}
