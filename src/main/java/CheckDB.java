import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.UUID;

public class CheckDB {
    public static void main(String[] args) {
        String url = "jdbc:sqlite:cine.db";
        try (Connection conn = DriverManager.getConnection(url)) {
            
            // Ver si hay funciones
            String funcionId = null;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id FROM funciones LIMIT 1")) {
                if (rs.next()) {
                    funcionId = rs.getString("id");
                }
            }
            System.out.println("Funcion encontrada: " + funcionId);
            
            if (funcionId == null) {
                System.out.println("No hay funciones para hacer una reserva de prueba.");
                return;
            }

            // Ver si hay reservas
            boolean tieneReservas = false;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as c FROM reserva_butacas")) {
                if (rs.next() && rs.getInt("c") > 0) {
                    tieneReservas = true;
                    System.out.println("La BD ya tiene " + rs.getInt("c") + " butacas reservadas.");
                } else {
                    System.out.println("La BD NO tiene butacas reservadas.");
                }
            }
            
            if (!tieneReservas) {
                System.out.println("Insertando butaca B4 (id: B4) de prueba...");
                String rId = UUID.randomUUID().toString();
                
                try (PreparedStatement pst1 = conn.prepareStatement(
                        "INSERT INTO reservas(id, funcion_id, codigo_referencia, dni_comprador, fecha_compra) VALUES(?,?,?,?,?)")) {
                    pst1.setString(1, rId);
                    pst1.setString(2, funcionId);
                    pst1.setString(3, "TEST-BOOKING-001");
                    pst1.setString(4, "00000000");
                    pst1.setString(5, java.time.LocalDateTime.now().toString());
                    pst1.executeUpdate();
                }
                
                try (PreparedStatement pst2 = conn.prepareStatement(
                        "INSERT INTO reserva_butacas(reserva_id, butaca_id) VALUES(?,?)")) {
                    pst2.setString(1, rId);
                    pst2.setString(2, "B4");
                    pst2.executeUpdate();
                }
                System.out.println("Butaca B4 insertada con éxito.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
