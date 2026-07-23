package com.cine.servidor;

import com.cine.dominio.*;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class PersistenciaManager {
    private static final String FILE_PATH = "database.txt";

    public static void guardar(EstadoServidor estado) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_PATH))) {
            writer.println("---SALAS---");
            for (SalaCine s : estado.getSalasMap().values()) {
                // id;nombre;filas;columnas;matriz
                StringBuilder matriz = new StringBuilder();
                for (int r = 0; r < s.getTotalRows(); r++) {
                    for (int c = 0; c < s.getTotalColumns(); c++) {
                        Butaca b = s.getButaca(r, c);
                        if (b == null) matriz.append("null");
                        else matriz.append(b.getFila()).append(",").append(b.getNumero()).append(",").append(b.getTipo().code());
                        matriz.append("|");
                    }
                }
                writer.println(s.getId() + ";" + s.getNombre() + ";" + s.getTotalRows() + ";" + s.getTotalColumns() + ";" + matriz.toString());
            }

            writer.println("---PELICULAS---");
            for (Pelicula p : estado.getPeliculasMap().values()) {
                writer.println(p.getId() + ";" + p.getNombre() + ";" + p.getDuracionMinutos());
            }

            writer.println("---FUNCIONES---");
            for (Funcion f : estado.getFuncionesMap().values()) {
                writer.println(f.getId() + ";" + f.getPelicula().getId() + ";" + f.getSala().getId() + ";" + f.getHoraInicio().toString());
            }

            writer.println("---BOLETAS---");
            for (Boleta b : estado.getBoletasMap().values()) {
                String asientos = String.join(",", b.getAsientos());
                writer.println(b.getId() + ";" + b.getFuncionId() + ";" + b.getPeliculaNombre() + ";" + b.getSalaNombre() + ";" + 
                               b.getHoraFuncion().toString() + ";" + b.getFechaReservada().toString() + ";" + 
                               b.getMetodoPago().name() + ";" + asientos + ";" + b.getDni() + ";" + b.getFechaEmision().toString() + ";" + b.getEstado().name());
            }
        } catch (IOException e) {
            System.err.println("[Persistencia] Error al guardar database: " + e.getMessage());
        }
    }

    public static void cargar(EstadoServidor estado) {
        File f = new File(FILE_PATH);
        if (!f.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            String line;
            String seccion = "";
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("---")) {
                    seccion = line;
                    continue;
                }
                String[] parts = line.split(";", -1);
                try {
                    switch (seccion) {
                        case "---SALAS---":
                            // id;nombre;filas;columnas;matriz
                            SalaCine sala = new SalaCine(parts[0], parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                            String[] celda = parts[4].split("\\|");
                            int idx = 0;
                            for (int r = 0; r < sala.getTotalRows(); r++) {
                                for (int c = 0; c < sala.getTotalColumns(); c++) {
                                    if (idx < celda.length && !celda[idx].equals("null")) {
                                        String[] bData = celda[idx].split(",");
                                        sala.replaceSeat(r, c, new Butaca(bData[0], Integer.parseInt(bData[1]), TipoButaca.fromCode(bData[2])));
                                    } else {
                                        sala.clearCell(r, c);
                                    }
                                    idx++;
                                }
                            }
                            estado.getSalasMap().put(sala.getId(), sala);
                            break;
                        case "---PELICULAS---":
                            Pelicula p = new Pelicula(parts[0], parts[1], Integer.parseInt(parts[2]));
                            estado.getPeliculasMap().put(p.getId(), p);
                            break;
                        case "---FUNCIONES---":
                            Pelicula pel = estado.getPeliculasMap().get(parts[1]);
                            SalaCine sal = estado.getSalasMap().get(parts[2]);
                            if (pel != null && sal != null) {
                                Funcion fun = new Funcion(parts[0], pel, sal, LocalDateTime.parse(parts[3]));
                                estado.getFuncionesMap().put(fun.getId(), fun);
                            }
                            break;
                        case "---BOLETAS---":
                            List<String> asientos = Arrays.asList(parts[7].split(","));
                            Boleta b = new Boleta(parts[0], parts[1], parts[2], parts[3], LocalDateTime.parse(parts[4]), 
                                                  LocalDate.parse(parts[5]), MetodoPago.valueOf(parts[6]), asientos, 
                                                  parts[8], LocalDateTime.parse(parts[9]), EstadoBoleta.valueOf(parts[10]));
                            estado.getBoletasMap().put(b.getId(), b);
                            break;
                    }
                } catch (Exception ex) {
                    System.err.println("[Persistencia] Error parseando línea: " + line + " - " + ex.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[Persistencia] Error al cargar database: " + e.getMessage());
        }
    }
}
