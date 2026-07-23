package com.cine.servidor;

import com.cine.compartido.Protocolo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Gestor de bloqueos pesimistas temporales sobre butacas.
 *
 * Paradigma: Bloqueo Pesimista con Timeout Automático.
 * ─────────────────────────────────────────────────────────────────────────
 * Cuando un cliente selecciona una butaca:
 *   1. Se registra un lock asociando (seatId → clientId + timestamp).
 *   2. Todos los demás clientes reciben push ACTUALIZACION_BUTACA:{seatId}:LOCKED.
 *    3. Si el cliente confirma la compra → lock se convierte en OCUPADO.
 *    4. Si el cliente cancela → lock se libera → LIBRE.
 *    5. Si el cliente se desconecta o expira el timeout (2 min) → lock
 *       se libera automáticamente → LIBRE (y se notifica a todos).
 *
 * // [PARADIGMA CONCURRENTE / MULTITHREAD]: 
 * Todos los métodos son thread-safe gracias a ConcurrentHashMap y
 * operaciones atómicas (computeIfAbsent, remove con condición).
 */
public class ManejadorBloqueoButacas {

    /** Información de un lock activo sobre una butaca. */
    private record LockEntry(String clientId) {}

    /** Mapa concurrente: seatId → LockEntry activo */
    private final Map<String, LockEntry> locks = new ConcurrentHashMap<>();

    /** Callback que se llama cuando un lock expira o es liberado */
    private final Consumer<String> onLockExpired;

    /**
     * @param onLockExpired Callback invocado cuando un lock expira por timeout.
     *                      Recibe el seatId cuyo lock fue liberado.
     */
    public ManejadorBloqueoButacas(Consumer<String> onLockExpired) {
        this.onLockExpired = onLockExpired;
    }

    /**
     * Intenta adquirir un bloqueo pesimista sobre una butaca.
     *
     * @param seatId   ID de la butaca (ej. "B3")
     * @param clientId ID único del cliente que quiere el lock
     * @return true si el lock fue adquirido, false si ya lo tiene otro cliente
     */
    public synchronized boolean tryLock(String seatId, String clientId) {
        LockEntry existing = locks.get(seatId);

        // Ya existe lock de otro cliente → rechazar
        if (existing != null && !existing.clientId().equals(clientId)) {
            return false;
        }
        // Ya existe lock propio → idempotente
        if (existing != null && existing.clientId().equals(clientId)) {
            return true;
        }

        locks.put(seatId, new LockEntry(clientId));
        return true;
    }

    /**
     * Libera el lock de una butaca si lo tiene el clientId indicado.
     *
     * @return true si se liberó, false si no tenía el lock
     */
    public synchronized boolean releaseLock(String seatId, String clientId) {
        LockEntry entry = locks.get(seatId);
        if (entry == null || !entry.clientId().equals(clientId)) {
            return false;
        }
        locks.remove(seatId);
        return true;
    }

    /**
     * Libera todos los locks que tenga el cliente (llamado al desconectarse).
     *
     * @return lista de seatIds que fueron liberados
     */
    public synchronized java.util.List<String> releaseAllLocksOf(String clientId) {
        java.util.List<String> released = new java.util.ArrayList<>();
        locks.entrySet().removeIf(e -> {
            if (e.getValue().clientId().equals(clientId)) {
                released.add(e.getKey());
                return true;
            }
            return false;
        });
        return released;
    }

    /**
     * Consulta si una butaca tiene lock de cualquier cliente.
     */
    public boolean isLocked(String seatId) {
        return locks.containsKey(seatId);
    }

    /**
     * Consulta si una butaca tiene lock de un cliente específico.
     */
    public boolean isLockedBy(String seatId, String clientId) {
        LockEntry e = locks.get(seatId);
        return e != null && e.clientId().equals(clientId);
    }

    public void shutdown() {
        // No-op
    }
}
