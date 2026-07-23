package com.cine.cliente.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Genera boletas en PDF para butacas-core usando Apache PDFBox 3.x.
 *
 * Trabaja directamente con el JSON plano de la boleta (sin necesitar
 * el modelo de dominio complejo). Adaptado de GeneradorPdfBoleto
 * del proyecto base (com.cine.dominio.boletos.GeneradorPdfBoleto).
 *
 * Diseño ticket formato A5 apaisado — 595 x 420 puntos.
 * Uso:
 *   byte[] pdf = GeneradorPdfBoleta.generate(jsonBoleta);
 *   GeneradorPdfBoleta.generateToFile(jsonBoleta, Path.of("boleta.pdf"));
 */
public final class GeneradorPdfBoleta {

    // Tipografías estándar de PDFBox (no requieren fuentes del SO)
    private static final PDFont FONT_BOLD  = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont FONT_REG   = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_OBLIQ = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

    // Dimensiones del ticket (A5 apaisado)
    private static final PDRectangle TICKET_SIZE = new PDRectangle(595f, 420f);
    private static final float MARGIN     = 32f;
    private static final float TOP        = TICKET_SIZE.getHeight() - MARGIN;
    private static final float RIGHT_EDGE = TICKET_SIZE.getWidth() - MARGIN;
    private static final float COL_WIDTH  = RIGHT_EDGE - MARGIN;

    private static final DateTimeFormatter DATE_FMT   = DateTimeFormatter.ofPattern("EEEE d MMM yyyy", new Locale("es"));
    private static final DateTimeFormatter TIME_FMT   = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ISSUED_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private GeneradorPdfBoleta() { /* utilidad estática */ }

    // =========================================================================
    // API pública
    // =========================================================================

    /**
     * Genera la boleta como array de bytes (PDF en memoria).
     * @param jsonBoleta JSON completo de la boleta del servidor
     */
    public static byte[] generate(String jsonBoleta) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            addTicketPage(doc, jsonBoleta);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Genera la boleta y la guarda directamente en disco.
     * Crea los directorios intermedios si no existen.
     * @param jsonBoleta JSON completo de la boleta del servidor
     * @param outputPath Ruta destino del archivo .pdf
     */
    public static void generateToFile(String jsonBoleta, Path outputPath) throws IOException {
        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }
        Files.write(outputPath, generate(jsonBoleta));
    }

    // =========================================================================
    // Construcción de la página
    // =========================================================================

    private static void addTicketPage(PDDocument doc, String json) throws IOException {
        PDPage page = new PDPage(TICKET_SIZE);
        doc.addPage(page);

        // --- Extracción de campos del JSON ---
        String id           = extractField(json, "id");
        String pelicula     = nvl(extractField(json, "pelicula"), "N/A");
        String sala         = nvl(extractField(json, "sala"),     "N/A");
        String horaRaw      = extractField(json, "hora");
        String fechaReserva = nvl(extractField(json, "fechaReservada"), "");
        String metodoPago   = nvl(extractField(json, "metodoPago"), "N/A");
        String dni          = nvl(extractField(json, "dni"),        "noDNI");
        String asientosRaw  = nvl(extractField(json, "asientos"), "");
        String emisionRaw   = extractField(json, "fechaEmision");

        // Formatear hora y fecha (viene como ISO: 2026-07-25T18:00)
        String hora  = "";
        String fecha = fechaReserva;
        if (horaRaw != null && horaRaw.contains("T")) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(horaRaw);
                hora  = ldt.format(TIME_FMT);
                fecha = ldt.format(DATE_FMT);
            } catch (Exception e) {
                int tIdx = horaRaw.indexOf('T');
                hora = horaRaw.substring(tIdx + 1, Math.min(horaRaw.length(), tIdx + 6));
            }
        }

        // Formatear fecha de emisión
        String emision = "";
        if (emisionRaw != null) {
            try {
                emision = LocalDateTime.parse(emisionRaw).format(ISSUED_FMT);
            } catch (Exception e) {
                emision = emisionRaw.replace("T", " ").substring(0, Math.min(emisionRaw.length(), 16));
            }
        }

        // Asientos: extraer solo la parte legible (tras el último '_')
        String asientos = Arrays.stream(asientosRaw.split(","))
                .filter(s -> !s.isBlank())
                .map(s -> s.contains("_") ? s.substring(s.lastIndexOf('_') + 1) : s)
                .collect(Collectors.joining("  "));

        // ID corto para referencia visual
        String idCorto = (id != null && id.length() >= 8)
                ? id.substring(0, 8).toUpperCase()
                : nvl(id, "N/A");

        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

            // === HEADER OSCURO ===
            fillRect(cs, 0, TICKET_SIZE.getHeight() - 70, TICKET_SIZE.getWidth(), 70, 0.12f, 0.12f, 0.18f);

            float y = TICKET_SIZE.getHeight() - 28;
            drawText(cs, FONT_BOLD, 16, "CINE BUTACAS CORE", MARGIN, y, 1f, 1f, 1f);
            y -= 18;
            drawText(cs, FONT_REG, 8, "Comprobante Oficial de Compra", MARGIN, y, 0.75f, 0.75f, 0.75f);

            // Referencia en esquina superior derecha
            drawTextRight(cs, FONT_BOLD, 9, "REF-" + idCorto, RIGHT_EDGE,
                    TICKET_SIZE.getHeight() - 28, 0.9f, 0.7f, 0.2f);
            drawTextRight(cs, FONT_REG, 7, "NO. BOLETA", RIGHT_EDGE,
                    TICKET_SIZE.getHeight() - 40, 0.75f, 0.75f, 0.75f);

            // === SEPARADOR ===
            y = TICKET_SIZE.getHeight() - 72;
            drawLine(cs, MARGIN, y, RIGHT_EDGE, y, 0.85f, 0.85f, 0.85f);
            y -= 16;

            // === DATOS DE LA FUNCIÓN ===
            y = drawLabel(cs, "PELICULA", pelicula, y);
            y = drawLabel(cs, "SALA",     sala,     y);
            y = drawLabel(cs, "FECHA",    fecha,    y);
            y = drawLabel(cs, "HORA",     hora,     y);

            // === SEPARADOR ===
            y -= 4;
            drawLine(cs, MARGIN, y, RIGHT_EDGE, y, 0.85f, 0.85f, 0.85f);
            y -= 14;

            // === DATOS DE LA COMPRA ===
            y = drawLabel(cs, "DNI",      dni,        y);
            y = drawLabel(cs, "ASIENTOS", asientos,   y);
            y = drawLabel(cs, "PAGO",     metodoPago, y);

            // === SEPARADOR ===
            y -= 4;
            drawLine(cs, MARGIN, y, RIGHT_EDGE, y, 0.85f, 0.85f, 0.85f);
            y -= 14;

            // === CONFIRMACIÓN (barra destacada, sin precio) ===
            fillRect(cs, MARGIN - 4, y - 5, COL_WIDTH + 8, 22, 0.12f, 0.12f, 0.18f);
            drawText(cs, FONT_BOLD, 12, "COMPRA CONFIRMADA", MARGIN + 2, y + 4, 1f, 1f, 1f);
            drawTextRight(cs, FONT_BOLD, 12, "OK", RIGHT_EDGE - 2, y + 4, 0.3f, 0.9f, 0.3f);

            // === PIE DE PÁGINA ===
            float footerY = MARGIN + 10;
            drawLine(cs, MARGIN, footerY + 12, RIGHT_EDGE, footerY + 12, 0.85f, 0.85f, 0.85f);
            drawText(cs, FONT_OBLIQ, 7,
                    "Emitido: " + emision + "    ID: " + idCorto + "...",
                    MARGIN, footerY, 0.5f, 0.5f, 0.5f);
        }
    }

    // =========================================================================
    // Helpers de dibujo (copiados del patrón de GeneradorPdfBoleto original)
    // =========================================================================

    /** Dibuja una fila etiqueta-valor en dos columnas. */
    private static float drawLabel(PDPageContentStream cs, String label, String value, float y)
            throws IOException {
        drawText(cs, FONT_BOLD, 8, label, MARGIN, y, 0.4f, 0.4f, 0.5f);
        drawText(cs, FONT_REG, 10, value, MARGIN + 90, y, 0.1f, 0.1f, 0.1f);
        return y - 16;
    }

    private static void drawText(PDPageContentStream cs, PDFont font, int size,
                                  String text, float x, float y,
                                  float r, float g, float b) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(r, g, b);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(text));
        cs.endText();
    }

    private static void drawTextRight(PDPageContentStream cs, PDFont font, int size,
                                       String text, float rightX, float y,
                                       float r, float g, float b) throws IOException {
        float textWidth = font.getStringWidth(sanitize(text)) / 1000f * size;
        drawText(cs, font, size, text, rightX - textWidth, y, r, g, b);
    }

    private static void drawLine(PDPageContentStream cs,
                                  float x1, float y1, float x2, float y2,
                                  float r, float g, float b) throws IOException {
        cs.setStrokingColor(r, g, b);
        cs.setLineWidth(0.5f);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    private static void fillRect(PDPageContentStream cs,
                                   float x, float y, float w, float h,
                                   float r, float g, float b) throws IOException {
        cs.setNonStrokingColor(r, g, b);
        cs.addRect(x, y, w, h);
        cs.fill();
    }

    /**
     * Elimina caracteres fuera de Latin-1 que Helvetica no puede codificar.
     * Mismo enfoque que el GeneradorPdfBoleto original.
     */
    private static String sanitize(String text) {
        if (text == null) return "";
        return text.chars()
                .filter(c -> c < 256)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    /** Extrae el valor de un campo del JSON plano. */
    private static String extractField(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        if (start < json.length() && json.charAt(start) == '"') {
            start++;
            int end = json.indexOf("\"", start);
            return end >= 0 ? json.substring(start, end) : json.substring(start);
        } else {
            int endComa  = json.indexOf(",", start);
            int endLlave = json.indexOf("}", start);
            if (endComa  == -1) endComa  = Integer.MAX_VALUE;
            if (endLlave == -1) endLlave = Integer.MAX_VALUE;
            int end = Math.min(endComa, endLlave);
            return end != Integer.MAX_VALUE ? json.substring(start, end).trim() : json.substring(start).trim();
        }
    }

    private static String nvl(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
