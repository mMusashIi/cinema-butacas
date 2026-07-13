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

    public SeleccionFuncionVista() {
        setStyle("-fx-background-color: #121212;");

        // Cabecera superior
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 20, 15, 20));
        header.setStyle("-fx-background-color: #1e1e1e; -fx-border-color: #333; -fx-border-width: 0 0 1 0;");

        Button btnVolver = new Button("← Volver al Menú");
        btnVolver.setStyle("-fx-cursor: hand;");
        btnVolver.setOnAction(e -> GestorVistas.navegarA(new PantallaInicio()));

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

    private void cargarFunciones(VBox lista) {
        new Thread(() -> {
            ClienteRed cliente = new ClienteRed();
            try {
                // Conectar sin callbacks de actualización ya que solo queremos hacer la petición sincrónica
                cliente.connect(null, null, null, null);
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
                javafx.application.Platform.runLater(() -> {
                    lista.getChildren().clear();
                    Label lblErr = new Label("Error de conexión al cargar funciones.");
                    lblErr.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");
                    lista.getChildren().add(lblErr);
                });
            } finally {
                cliente.disconnect();
            }
        }).start();
    }

    private void abrirPuntoVenta(String funcionId) {
        // En una implementación completa, pasaríamos el funcionId a PuntoVentaVista, 
        // y éste descargaría la geometría. Como PuntoVentaVista aún requiere los objetos,
        // descargamos el JSON completo aquí y lo parseamos.
        new Thread(() -> {
            ClienteRed cliente = new ClienteRed();
            try {
                cliente.connect(null, null, null, null);
                String json = cliente.obtenerDetalleFuncion(funcionId);
                
                // Parseo manual simplificado
                String salaStr = extractJsonObject(json, "sala");
                String salaId = extractField(salaStr, "id");
                String salaNombre = extractField(salaStr, "nombre");
                int filas = Integer.parseInt(extractField(salaStr, "filas"));
                int columnas = Integer.parseInt(extractField(salaStr, "columnas"));
                
                String cineStr = extractJsonObject(salaStr, "cine");
                Cine cine = new Cine(extractField(cineStr, "nombre"), extractField(cineStr, "direccion"), extractField(cineStr, "ciudad"));
                
                SalaCine sala = new com.cine.constructor.ConstructorSala().name(salaNombre).cinema(cine).rows(filas).columns(columnas).build();
                com.cine.constructor.EditorSala editor = new com.cine.constructor.EditorSala(sala);
                
                String butacasArray = extractJsonArray(salaStr, "butacas");
                String[] butacas = butacasArray.split("\\},\\{");
                for (String b : butacas) {
                    b = b.replace("{", "").replace("}", "");
                    int f = Integer.parseInt(extractField(b, "f"));
                    int c = Integer.parseInt(extractField(b, "c"));
                    String t = extractField(b, "t");
                    editor.setSeatType(f, c, TipoButaca.fromCode(t));
                }

                String peliStr = extractJsonObject(json, "pelicula");
                Pelicula pelicula = new Pelicula(extractField(peliStr, "titulo"), Integer.parseInt(extractField(peliStr, "duracion")), extractField(peliStr, "clasificacion"));
                
                LocalDateTime horaInicio = LocalDateTime.parse(extractField(json, "horaInicio"));
                FormatoFuncion formato = FormatoFuncion.valueOf(extractField(json, "formato"));
                double precio = Double.parseDouble(extractField(json, "precioBase"));
                
                Funcion funcion = new Funcion(sala, pelicula, horaInicio, formato, precio);
                
                // Motor Precios (hardcoded localmente por simplicidad)
                MotorPrecios motor = new MotorPrecios();
                motor.addPermanentRule(new ConstructorReglaPrecio()
                        .onDays(DayOfWeek.WEDNESDAY).discount(0.50).buildNamed("Día del espectador", "50% off"));

                javafx.application.Platform.runLater(() -> {
                    GestorVistas.navegarA(new PuntoVentaVista(cine, sala, pelicula, funcion, motor));
                });

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                cliente.disconnect();
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
