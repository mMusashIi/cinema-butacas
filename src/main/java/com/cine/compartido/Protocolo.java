package com.cine.compartido;

/**
 * Contrato de comunicación TCP entre ServidorCine y ClienteRed.
 *
 * Formato de mensajes (líneas de texto terminadas en '\n'):
 *
 * CLIENTE → SERVIDOR
 * ─────────────────────────────────────────────────────────
 *  OBTENER_SALA                         → Solicita snapshot completo de la sala
 *  SELECCIONAR:{seatId}                  → Intenta bloqueo pesimista (temporal lock)
 *  DESELECCIONAR:{seatId}                → Libera el bloqueo propio sobre la butaca
 *  RESERVAR:{seatId1},{seatId2}:{dni}   → Confirma compra de 1..N butacas
 *  PING                             → Keep-alive
 *
 * SERVIDOR → CLIENTE (respuestas síncronas)
 * ─────────────────────────────────────────────────────────
 *  ESTADO_SALA:{json}                → JSON del snapshot completo de la sala
 *  OK                               → Operación exitosa
 *  ERROR:{mensaje}                  → Operación rechazada con motivo
 *  PONG                             → Respuesta a PING
 *
 * SERVIDOR → TODOS LOS CLIENTES (push asíncrono)
 * ─────────────────────────────────────────────────────────
 *  ACTUALIZACION_BUTACA:{seatId}:{estado}    → Un asiento cambió de estado
 *
 * Estados de butaca que circulan por red (EstadoButaca.name()):
 *   FREE | SELECTED | LOCKED | BOOKED | BROKEN
 *
 * LOCKED es un estado de red que aparece cuando otro cliente
 * tiene un bloqueo pesimista temporal sobre esa butaca.
 */
public final class Protocolo {

    private Protocolo() { /* Solo constantes */ }

    // ── Comandos Cliente → Servidor ──────────────────────────────────────
    public static final String OBTENER_SALA   = "OBTENER_SALA";
    public static final String SELECCIONAR     = "SELECCIONAR";
    public static final String DESELECCIONAR   = "DESELECCIONAR";
    public static final String RESERVAR       = "RESERVAR";
    public static final String PING       = "PING";
    
    // CRUD Commands
    public static final String LISTAR_FUNCIONES = "LISTAR_FUNCIONES";
    public static final String OBTENER_DETALLE_FUNCION = "OBTENER_DETALLE_FUNCION";
    public static final String LISTAR_CINES = "LISTAR_CINES";
    public static final String LISTAR_PELICULAS = "LISTAR_PELICULAS";
    public static final String LISTAR_SALAS = "LISTAR_SALAS";
    public static final String CREAR_PELICULA = "CREAR_PELICULA";
    public static final String CREAR_FUNCION = "CREAR_FUNCION";
    public static final String CREAR_SALA = "CREAR_SALA";
    public static final String CREAR_CINE = "CREAR_CINE";
    public static final String LISTAR_RESERVAS = "LISTAR_RESERVAS";

    // ── Respuestas Servidor → Cliente ─────────────────────────────────────
    public static final String ESTADO_SALA   = "ESTADO_SALA";
    public static final String OK           = "OK";
    public static final String ERROR        = "ERROR";
    public static final String PONG         = "PONG";
    public static final String ACTUALIZACION_BUTACA  = "ACTUALIZACION_BUTACA";
    
    // CRUD Responses
    public static final String RESPUESTA_FUNCIONES = "RESPUESTA_FUNCIONES";
    public static final String RESPUESTA_DETALLE_FUNCION = "RESPUESTA_DETALLE_FUNCION";
    public static final String RESPUESTA_CINES = "RESPUESTA_CINES";
    public static final String RESPUESTA_PELICULAS = "RESPUESTA_PELICULAS";
    public static final String RESPUESTA_SALAS = "RESPUESTA_SALAS";
    public static final String RESPUESTA_RESERVAS = "RESPUESTA_RESERVAS";

    // ── Separadores ───────────────────────────────────────────────────────
    public static final String SEP     = ":";  // separador principal
    public static final String SEP_SUB = ",";  // separador secundario (lista de butacas)

    // ── Estado de red para lock pesimista ─────────────────────────────────
    /** El cliente que tiene el lock ve la butaca como SELECTED.
     *  Los demás clientes la ven como LOCKED. */
    public static final String NET_LOCKED = "LOCKED";

    // ── Mensajes de sesión por cliente ───────────────────────────────────────
    /** El servidor envía el tiempo restante de sesión cada segundo. */
    public static final String TIEMPO_SESION    = "TIEMPO_SESION";
    /** El servidor envía este mensaje cuando la sesión del cliente expira. */
    public static final String SESION_EXPIRADA = "SESION_EXPIRADA";
    /** Duración máxima de una sesión de cliente en milisegundos (5 min). */
    public static final long   DURACION_SESION_MS = 300_000L;

    // ── Configuración de red ──────────────────────────────────────────────
    public static final int    PORT              = 9090;
    public static final String HOST              = "localhost";

    // ── Helpers de construcción de mensajes ───────────────────────────────

    public static String seatUpdate(String seatId, String estado) {
        return ACTUALIZACION_BUTACA + SEP + seatId + SEP + estado;
    }

    public static String error(String msg) {
        return ERROR + SEP + msg;
    }

    public static String roomState(String json) {
        return ESTADO_SALA + SEP + json;
    }

    /** Helper antiguo para compatibilidad interna. */
    public static String estadoSala(String json) {
        return roomState(json);
    }
}
