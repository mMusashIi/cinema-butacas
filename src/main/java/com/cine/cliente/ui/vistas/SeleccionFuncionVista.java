package com.cine.cliente.ui.vistas;

import com.cine.cliente.ClienteRed;
import com.cine.cliente.ui.GestorVistas;
import com.cine.dominio.*;
import com.cine.dominio.precios.ConstructorReglaPrecio;
import com.cine.dominio.precios.MotorPrecios;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class SeleccionFuncionVista extends BorderPane {

    /**
     * Bug 3: Conexión TCP única que vive durante toda la sesión de esta vista.
     * Se abre al cargar las funciones y se cierra antes de navegar a otra pantalla.
     * Evita crear múltiples sesiones en el servidor (cada conexión activa un timer de 5 min).
     */
    private ClienteRed clientePersistente;

    public SeleccionFuncionVista() {
        setStyle("-fx-background-color: #121212;");

        // Cabecera superior
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 20, 15, 20));
        header.setStyle("-fx-background-color: #1e1e1e; -fx-border-color: #333; -fx-border-width: 0 0 1 0;");

        Button btnVolver = new Button("← Volver al Menú");
        btnVolver.setStyle("-fx-cursor: hand;");
        btnVolver.setOnAction(e -> {
            // Bug 3: cerrar la conexión persistente antes de navegar.
            desconectar();
            GestorVistas.navegarA(new PantallaInicio());
        });

        Label titulo = new Label("Seleccionar Función");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        header.getChildren().addAll(btnVolver, titulo);
        setTop(header);

        VBox lista = new VBox(15);
        lista.setPadding(new Insets(20));
        lista.setAlignment(Pos.TOP_CENTER);
        setCenter(lista);

        cargarFunciones(lista);
    }

    /** Desconecta la conexión persistente de forma segura. */
    private void desconectar() {
        if (clientePersistente != null) {
            clientePersistente.disconnect();
            clientePersistente = null;
        }
    }

    private void cargarFunciones(VBox lista) {
        new Thread(() -> {
            // Bug 3: crear la única conexión persistente de esta vista.
            ClienteRed cliente = new ClienteRed();
            try {
                cliente.connect(null, null, null, null);
                // Guardar la referencia para reutilizarla en abrirPuntoVenta().
                clientePersistente = cliente;
                String json = cliente.listarFunciones();

                javafx.application.Platform.runLater(() -> {
                    lista.getChildren().clear();
                    if (json.equals("[]")) {
                        Label lblEmpty = new Label("No hay funciones programadas.");
                        lblEmpty.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
                        lista.getChildren().add(lblEmpty);
                    } else {
                        String[] funciones = json.replaceAll("^\\[|\\]$", "").split("\\},\\{");
                        for (String f : funciones) {
                            f = f.replace("{", "").replace("}", "");
                            String id = extractField(f, "id");
                            String pelicula = extractField(f, "peliculaTitulo");
                            String sala = extractField(f, "salaNombre");
                            String cine = extractField(f, "cineNombre");
                            String hora = extractField(f, "horaInicio");

                            Button btn = new Button(pelicula + "\n" + cine + " - " + sala + "\n" + hora);
                            btn.setStyle("-fx-font-size: 16px; -fx-padding: 20px; -fx-cursor: hand; -fx-background-color: #2b2b2b; -fx-text-fill: white;");
                            btn.setMaxWidth(Double.MAX_VALUE);
                            btn.setOnAction(e -> abrirPuntoVenta(id));

                            lista.getChildren().add(btn);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                // Si falló la conexión, liberar recursos y no guardar referencia.
                desconectar();
                javafx.application.Platform.runLater(() -> {
                    lista.getChildren().clear();
                    Label lblErr = new Label("Error de conexión al cargar funciones.");
                    lblErr.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");
                    lista.getChildren().add(lblErr);
                });
            }
            // Bug 3: NO desconectar aquí. La conexión se mantiene abierta
            // para reutilizarla en abrirPuntoVenta().
        }).start();
    }

    private void abrirPuntoVenta(String funcionId) {
        // En una implementación completa, pasaríamos el funcionId a PuntoVentaVista, 
        // y éste descargaría la geometría. Como PuntoVentaVista aún requiere los objetos,
        // descargamos el JSON completo aquí y lo parseamos.
        new Thread(() -> {
            // Bug 3: reutilizar la conexión persistente en lugar de crear una nueva.
            ClienteRed cliente = clientePersistente;
            if (cliente == null) {
                javafx.application.Platform.runLater(() -> {
                    new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.ERROR,
                        "No hay conexión activa con el servidor."
                    ).show();
                });
                return;
            }
            try {
                String json = cliente.obtenerDetalleFuncion(funcionId);

                // ── Parseo de la función ──────────────────────────────────────────────
                // Bug B: usar "funcionId" (no "id") y el constructor de restauración
                // para que el ID coincida exactamente con el del servidor.
                String realFuncionId = extractField(json, "funcionId");

                // ── Parseo de la sala ─────────────────────────────────────────────────
                String salaStr = extractJsonObject(json, "sala");
                String salaId     = extractField(salaStr, "id");
                String salaNombre = extractField(salaStr, "nombre");
                int filas    = Integer.parseInt(extractField(salaStr, "filas"));
                int columnas = Integer.parseInt(extractField(salaStr, "columnas"));

                String cineStr = extractJsonObject(salaStr, "cine");
                String cineId  = extractField(cineStr, "id");
                Cine cine = new Cine(
                    extractField(cineStr, "nombre"),
                    extractField(cineStr, "direccion"),
                    extractField(cineStr, "ciudad")
                );

                // Bug A+C: usar el constructor de restauración de SalaCine (con ID real
                // y matriz vacía) y colocar cada butaca con su UUID original del servidor.
                // Los slots PASILLO llegan como {id:"", t:"PASILLO"} y quedan como null.
                SalaCine sala = new SalaCine(salaId, salaNombre, cine, filas, columnas);

                String butacasArray = extractJsonArray(salaStr, "butacas");
                if (butacasArray != null && !butacasArray.isBlank()) {
                    String[] butacas = butacasArray.split("\\},\\{");
                    final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
                    for (String b : butacas) {
                        b = b.replace("{", "").replace("}", "");
                        int r  = Integer.parseInt(extractField(b, "f"));
                        int c  = Integer.parseInt(extractField(b, "c"));
                        String t  = extractField(b, "t");
                        String id = extractField(b, "id");

                        if ("PASILLO".equals(t) || id == null || id.isBlank()) {
                            // Celda vacía — dejar null (ya lo está al construir SalaCine con ID)
                            continue;
                        }
                        String rowLetter = String.valueOf(ALPHABET.charAt(r));
                        int seatNumber   = c + 1;
                        TipoButaca tipo  = TipoButaca.fromCode(t);
                        // Butaca con el UUID real del servidor → los selectSeat() coincidirán
                        sala.replaceSeat(r, c, new com.cine.dominio.Butaca(id, rowLetter, seatNumber, tipo));
                    }
                }

                // ── Parseo de la película ─────────────────────────────────────────────
                String peliStr = extractJsonObject(json, "pelicula");
                Pelicula pelicula = new Pelicula(
                    extractField(peliStr, "id"),
                    extractField(peliStr, "titulo"),
                    Integer.parseInt(extractField(peliStr, "duracion")),
                    extractField(peliStr, "clasificacion"),
                    null, null, null, null,
                    java.time.LocalDate.now(),
                    com.cine.dominio.EstadoPelicula.VENTA
                );

                LocalDateTime horaInicio = LocalDateTime.parse(extractField(json, "horaInicio"));
                FormatoFuncion formato   = FormatoFuncion.valueOf(extractField(json, "formato"));
                double precio            = Double.parseDouble(extractField(json, "precioBase"));

                // Bug B: usar el constructor de restauración de Funcion con el ID real
                // para que getRoomState(funcion.getId()) encuentre la función en el servidor.
                Funcion funcion = new Funcion(realFuncionId, sala, pelicula, horaInicio, formato, precio, false);

                // Motor Precios (hardcoded localmente por simplicidad)
                MotorPrecios motor = new MotorPrecios();
                motor.addPermanentRule(new ConstructorReglaPrecio()
                        .onDays(DayOfWeek.WEDNESDAY).discount(0.50).buildNamed("Día del espectador", "50% off"));

                // Bug 3: cerrar la conexión persistente ANTES de navegar.
                // PuntoVentaVista creará su propia conexión dedicada.
                desconectar();

                javafx.application.Platform.runLater(() -> {
                    GestorVistas.navegarA(new PuntoVentaVista(cine, sala, pelicula, funcion, motor));
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    private String extractField(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        
        if (json.charAt(start) == '"') {
            start++; // saltar comilla
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } else {
            int endComa = json.indexOf(",", start);
            int endLlave = json.indexOf("}", start);
            if (endComa == -1) endComa = Integer.MAX_VALUE;
            if (endLlave == -1) endLlave = Integer.MAX_VALUE;
            int end = Math.min(endComa, endLlave);
            return json.substring(start, end);
        }
    }
    
    private String extractJsonObject(String json, String key) {
        String search = "\"" + key + "\":{";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length() - 1; // apuntar al '{'
        
        int brackets = 1;
        for (int i = start + 1; i < json.length(); i++) {
            if (json.charAt(i) == '{') brackets++;
            if (json.charAt(i) == '}') brackets--;
            if (brackets == 0) return json.substring(start, i + 1);
        }
        return null;
    }
    
    private String extractJsonArray(String json, String key) {
        String search = "\"" + key + "\":[";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length() - 1; // apuntar al '['
        
        int brackets = 1;
        for (int i = start + 1; i < json.length(); i++) {
            if (json.charAt(i) == '[') brackets++;
            if (json.charAt(i) == ']') brackets--;
            if (brackets == 0) return json.substring(start + 1, i); // contenido sin corchetes
        }
        return null;
    }
}
