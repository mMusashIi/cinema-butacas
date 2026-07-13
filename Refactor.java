import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Refactor {
    public static void main(String[] args) throws IOException {
        Path srcDir = Paths.get("src/main/java/com/cinema");
        Path destDir = Paths.get("src/main/java/com/cine");

        // 1. Rename the base package directory
        if (Files.exists(srcDir)) {
            Files.move(srcDir, destDir, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Renamed com.cinema to com.cine");
        }

        Map<String, String> replacements = new LinkedHashMap<>();

        // Paquetes
        replacements.put("\\bcom\\.cinema\\b", "com.cine");
        replacements.put("\\bbuilder\\b", "constructor");
        replacements.put("\\bclient\\b", "cliente");
        replacements.put("\\bdomain\\b", "dominio");
        replacements.put("\\bserver\\b", "servidor");
        replacements.put("\\bshared\\b", "compartido");
        replacements.put("\\bui\\b", "ui");
        replacements.put("\\bcomponents\\b", "componentes");
        replacements.put("\\batoms\\b", "atomos");
        replacements.put("\\bmolecules\\b", "moleculas");
        replacements.put("\\borganisms\\b", "organismos");
        replacements.put("\\bpricing\\b", "precios");
        replacements.put("\\brules\\b", "reglas");
        replacements.put("\\bticketing\\b", "boletos");

        // Clases de Dominio
        replacements.put("\\bCinema\\b", "Cine");
        replacements.put("\\bCinemaRoom\\b", "SalaCine");
        replacements.put("\\bMovie\\b", "Pelicula");
        replacements.put("\\bMovieAvailability\\b", "DisponibilidadPelicula");
        replacements.put("\\bMovieStatus\\b", "EstadoPelicula");
        replacements.put("\\bSeat\\b", "Butaca");
        replacements.put("\\bSeatStatus\\b", "EstadoButaca");
        replacements.put("\\bSeatType\\b", "TipoButaca");
        replacements.put("\\bShowtime\\b", "Funcion");
        replacements.put("\\bShowtimeFormat\\b", "FormatoFuncion");
        
        replacements.put("\\bNamedPricingRule\\b", "ReglaPrecioNombrada");
        replacements.put("\\bPricingEngine\\b", "MotorPrecios");
        replacements.put("\\bPricingRule\\b", "ReglaPrecio");
        replacements.put("\\bPricingRuleBuilder\\b", "ConstructorReglaPrecio");
        
        replacements.put("\\bAccessibleSeatDiscountRule\\b", "ReglaDescuentoAccesible");
        replacements.put("\\bEarlyBirdRule\\b", "ReglaReservaAnticipada");
        replacements.put("\\bPresaleDiscountRule\\b", "ReglaDescuentoPreventa");
        replacements.put("\\bWednesdayDiscountRule\\b", "ReglaDescuentoMiercoles");
        
        replacements.put("\\bTicket\\b", "Boleto");
        replacements.put("\\bTicketPdfGenerator\\b", "GeneradorPdfBoleto");
        
        // Servidor y Cliente
        replacements.put("\\bCinemaServer\\b", "ServidorCine");
        replacements.put("\\bClientHandler\\b", "ManejadorCliente");
        replacements.put("\\bSeatLockManager\\b", "ManejadorBloqueoButacas");
        replacements.put("\\bServerState\\b", "EstadoServidor");
        replacements.put("\\bProtocol\\b", "Protocolo");
        
        replacements.put("\\bNetworkClient\\b", "ClienteRed");
        replacements.put("\\bCinemaApp\\b", "AplicacionCine");
        replacements.put("\\bSeatButton\\b", "BotonButaca");
        replacements.put("\\bLegendItem\\b", "ItemLeyenda");
        replacements.put("\\bSelectedSeatRow\\b", "FilaButacaSeleccionada");
        replacements.put("\\bHeaderPanel\\b", "PanelCabecera");
        replacements.put("\\bSeatGridPanel\\b", "PanelCuadriculaButacas");
        replacements.put("\\bSelectionPanel\\b", "PanelSeleccion");
        
        replacements.put("\\bRoomBuilder\\b", "ConstructorSala");
        replacements.put("\\bRoomEditor\\b", "EditorSala");

        // Métodos y Variables (Cuidado con el orden para no pisarse)
        replacements.put("\\bnetworkClient\\b", "clienteRed");
        replacements.put("\\bseatGridPanel\\b", "panelCuadriculaButacas");
        replacements.put("\\bselectionPanel\\b", "panelSeleccion");
        replacements.put("\\bheaderPanel\\b", "panelCabecera");
        replacements.put("\\bseatButton\\b", "botonButaca");
        
        replacements.put("\\bengine\\b", "motor");
        replacements.put("\\bshowtime\\b", "funcion");
        replacements.put("\\bmovie\\b", "pelicula");
        replacements.put("\\bseat\\b", "butaca");
        replacements.put("\\bseats\\b", "butacas");
        replacements.put("\\broom\\b", "sala");
        replacements.put("\\bstatus\\b", "estado");
        replacements.put("\\btype\\b", "tipo");

        // Getters/Setters y métodos específicos
        replacements.put("\\bgetMovie\\b", "getPelicula");
        replacements.put("\\bgetShowtime\\b", "getFuncion");
        replacements.put("\\bgetSeat\\b", "getButaca");
        replacements.put("\\bgetRoom\\b", "getSala");
        replacements.put("\\bgetStatus\\b", "getEstado");
        replacements.put("\\bgetType\\b", "getTipo");
        replacements.put("\\bgetBasePrice\\b", "getPrecioBase");
        replacements.put("\\bgetName\\b", "getNombre");
        replacements.put("\\bgetTitle\\b", "getTitulo");
        replacements.put("\\bgetDuration\\b", "getDuracion");
        replacements.put("\\bgetPriceMultiplier\\b", "getMultiplicadorPrecio");
        replacements.put("\\bisAccessible\\b", "isAccesible");
        replacements.put("\\bgetId\\b", "getId");
        replacements.put("\\bgetRow\\b", "getFila");
        replacements.put("\\bgetNumber\\b", "getNumero");
        replacements.put("\\bgetAllSeats\\b", "getTodasLasButacas");
        replacements.put("\\bbookSeat\\b", "reservarButaca");
        replacements.put("\\brelease\\b", "liberar");
        replacements.put("\\bselect\\b", "seleccionar");
        replacements.put("\\bcalculatePrice\\b", "calcularPrecio");
        replacements.put("\\bapply\\b", "aplicar");
        
        replacements.put("\\bconnectToServer\\b", "conectarAlServidor");
        replacements.put("\\bsyncRoomStateFromServer\\b", "sincronizarEstadoSalaDesdeServidor");
        replacements.put("\\bhandleSeatClicked\\b", "manejarClicButaca");
        replacements.put("\\bhandleSeatRemove\\b", "manejarRemoverButaca");
        replacements.put("\\bhandleCancel\\b", "manejarCancelar");
        replacements.put("\\bhandleConfirm\\b", "manejarConfirmar");
        replacements.put("\\brefreshVisuals\\b", "actualizarVisuales");
        replacements.put("\\bupdateTimer\\b", "actualizarTemporizador");
        replacements.put("\\bhandleNetworkUpdate\\b", "manejarActualizacionRed");
        
        // Protocolo
        replacements.put("\\bSESSION_TIME\\b", "TIEMPO_SESION");
        replacements.put("\\bSESSION_EXPIRED\\b", "SESION_EXPIRADA");
        replacements.put("\\bSESSION_DURATION_MS\\b", "DURACION_SESION_MS");
        replacements.put("\\bSEAT_UPDATE\\b", "ACTUALIZACION_BUTACA");
        replacements.put("\\bGET_ROOM\\b", "OBTENER_SALA");
        replacements.put("\\bROOM_STATE\\b", "ESTADO_SALA");
        replacements.put("\\bSELECT\\b", "SELECCIONAR");
        replacements.put("\\bDESELECT\\b", "DESELECCIONAR");
        replacements.put("\\bBOOK\\b", "RESERVAR");
        replacements.put("\\bWHEELCHAIR\\b", "SILLA_RUEDAS");
        replacements.put("\\bCOUPLE\\b", "PAREJA");

        try (Stream<Path> paths = Files.walk(destDir)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".java"))
                 .forEach(path -> {
                     try {
                         String content = new String(Files.readAllBytes(path));
                         for (Map.Entry<String, String> entry : replacements.entrySet()) {
                             content = content.replaceAll(entry.getKey(), entry.getValue());
                         }
                         Files.write(path, content.getBytes());
                         
                         // Rename file if class name changed
                         String fileName = path.getFileName().toString();
                         String newFileName = fileName;
                         for (Map.Entry<String, String> entry : replacements.entrySet()) {
                             newFileName = newFileName.replaceAll(entry.getKey(), entry.getValue());
                         }
                         if (!fileName.equals(newFileName)) {
                             Files.move(path, path.resolveSibling(newFileName));
                             System.out.println("Renamed file: " + fileName + " -> " + newFileName);
                         }
                     } catch (Exception e) {
                         e.printStackTrace();
                     }
                 });
        }
        
        // Update POM
        Path pom = Paths.get("pom.xml");
        if (Files.exists(pom)) {
            String content = new String(Files.readAllBytes(pom));
            content = content.replaceAll("com\\.cinema\\.client\\.ui\\.CinemaApp", "com.cine.cliente.ui.AplicacionCine");
            content = content.replaceAll("com\\.cinema\\.server\\.CinemaServer", "com.cine.servidor.ServidorCine");
            Files.write(pom, content.getBytes());
            System.out.println("Updated pom.xml");
        }
        
        // Also rename directories based on the package changes!
        renameDir("src/main/java/com/cine/builder", "src/main/java/com/cine/constructor");
        renameDir("src/main/java/com/cine/client", "src/main/java/com/cine/cliente");
        renameDir("src/main/java/com/cine/domain", "src/main/java/com/cine/dominio");
        renameDir("src/main/java/com/cine/server", "src/main/java/com/cine/servidor");
        renameDir("src/main/java/com/cine/shared", "src/main/java/com/cine/compartido");
        
        renameDir("src/main/java/com/cine/cliente/ui/components", "src/main/java/com/cine/cliente/ui/componentes");
        renameDir("src/main/java/com/cine/cliente/ui/componentes/atoms", "src/main/java/com/cine/cliente/ui/componentes/atomos");
        renameDir("src/main/java/com/cine/cliente/ui/componentes/molecules", "src/main/java/com/cine/cliente/ui/componentes/moleculas");
        renameDir("src/main/java/com/cine/cliente/ui/componentes/organisms", "src/main/java/com/cine/cliente/ui/componentes/organismos");
        
        renameDir("src/main/java/com/cine/dominio/pricing", "src/main/java/com/cine/dominio/precios");
        renameDir("src/main/java/com/cine/dominio/precios/rules", "src/main/java/com/cine/dominio/precios/reglas");
        renameDir("src/main/java/com/cine/dominio/ticketing", "src/main/java/com/cine/dominio/boletos");
        
        System.out.println("Done!");
    }
    
    private static void renameDir(String from, String to) throws IOException {
        Path pFrom = Paths.get(from);
        Path pTo = Paths.get(to);
        if (Files.exists(pFrom)) {
            Files.move(pFrom, pTo, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Renamed dir: " + from + " -> " + to);
        }
    }
}
