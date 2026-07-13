package com.cine.dominio.boletos;

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
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generador de boletas en PDF usando Apache PDFBox 3.x.
 * Clase de utilidad estática — no tiene estado propio.
 *
 * Separa completamente la presentación (PDF) de los datos (Boleto).
 * Si en el futuro se necesita un formato distinto (HTML, email, impresora
 * térmica), se crea otro generador — Boleto no cambia.
 *
 * Uso para boleta individual:
 *   byte[] pdf = GeneradorPdfBoleto.generate(ticket);
 *   GeneradorPdfBoleto.generateToFile(ticket, Path.of("boleta.pdf"));
 *
 * Uso para múltiples boletas en un solo PDF (compra de varios asientos):
 *   GeneradorPdfBoleto.generateBatch(tickets, Path.of("compra.pdf"));
 */
public final class GeneradorPdfBoleto {

    // Tipografías estándar (embebidas en PDFBox, no requieren fuentes del SO)
    private static final PDFont FONT_BOLD  = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont FONT_REG   = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_OBLIQ = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

    // Dimensiones del ticket (formato A5 apaisado — 210 × 148 mm en puntos)
    private static final PDRectangle TICKET_SIZE = new PDRectangle(595f, 420f);

    // Márgenes y posición base
    private static final float MARGIN      = 32f;
    private static final float TOP         = TICKET_SIZE.getHeight() - MARGIN;
    private static final float RIGHT_EDGE  = TICKET_SIZE.getWidth() - MARGIN;
    private static final float COL_WIDTH   = RIGHT_EDGE - MARGIN;

    // Formatos de fecha
    private static final DateTimeFormatter DATE_FMT    = DateTimeFormatter.ofPattern("EEEE d MMM yyyy", new java.util.Locale("es"));
    private static final DateTimeFormatter TIME_FMT    = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ISSUED_FMT  = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private GeneradorPdfBoleto() { /* utilidad estática */ }

    // =====================================================================
    // API pública
    // =====================================================================

    /**
     * Genera la boleta de un ticket como array de bytes (PDF en memoria).
     */
    public static byte[] generate(Boleto ticket) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            addTicketPage(doc, ticket);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Genera la boleta y la guarda directamente en disco.
     * Crea los directorios intermedios si no existen.
     */
    public static void generateToFile(Boleto ticket, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent() != null ? outputPath.getParent() : Path.of("."));
        Files.write(outputPath, generate(ticket));
    }

    /**
     * Genera un PDF con múltiples boletas (una por página) — para compras
     * de varios asientos en la misma transacción.
     */
    public static byte[] generateBatch(List<Boleto> tickets) throws IOException {
        if (tickets == null || tickets.isEmpty()) {
            throw new IllegalArgumentException("La lista de tickets no puede estar vacía");
        }
        try (PDDocument doc = new PDDocument()) {
            for (Boleto ticket : tickets) {
                addTicketPage(doc, ticket);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Genera múltiples boletas y las guarda en disco.
     */
    public static void generateBatchToFile(List<Boleto> tickets, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent() != null ? outputPath.getParent() : Path.of("."));
        Files.write(outputPath, generateBatch(tickets));
    }

    // =====================================================================
    // Construcción de la página
    // =====================================================================

    private static void addTicketPage(PDDocument doc, Boleto ticket) throws IOException {
        PDPage page = new PDPage(TICKET_SIZE);
        doc.addPage(page);

        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            float y = TOP;

            // --- Fondo gris oscuro del encabezado ---
            fillRect(cs, 0, TICKET_SIZE.getHeight() - 70, TICKET_SIZE.getWidth(), 70, 0.12f, 0.12f, 0.18f);

            // --- Nombre del cine (encabezado) ---
            y = TICKET_SIZE.getHeight() - 28;
            drawText(cs, FONT_BOLD, 16, ticket.getCinemaName().toUpperCase(), MARGIN, y, 1f, 1f, 1f);

            // --- Dirección del cine ---
            y -= 18;
            String address = ticket.getCinemaAddress().isEmpty() ? "" : ticket.getCinemaAddress();
            drawText(cs, FONT_REG, 8, address, MARGIN, y, 0.75f, 0.75f, 0.75f);

            // --- Código de referencia (derecha del encabezado) ---
            drawTextRight(cs, FONT_BOLD, 9, ticket.getReferenceCode(), RIGHT_EDGE,
                    TICKET_SIZE.getHeight() - 28, 0.9f, 0.7f, 0.2f);
            drawTextRight(cs, FONT_REG, 7, "REF. BOLETA", RIGHT_EDGE,
                    TICKET_SIZE.getHeight() - 40, 0.75f, 0.75f, 0.75f);

            // --- Separador ---
            y = TICKET_SIZE.getHeight() - 72;
            drawLine(cs, MARGIN, y, RIGHT_EDGE, y, 0.85f, 0.85f, 0.85f);
            y -= 16;

            // --- Datos de la función ---
            y = drawLabel(cs, "PELÍCULA", ticket.getMovieTitle()
                    + "  [" + ticket.getMovieClassification() + "]", y);
            y = drawLabel(cs, "SALA", ticket.getRoomName(), y);
            y = drawLabel(cs, "FECHA", ticket.getFuncion().getStartTime().format(DATE_FMT), y);
            y = drawLabel(cs, "HORA", ticket.getFuncion().getStartTime().format(TIME_FMT)
                    + "  —  " + ticket.getFuncion().getEndTime().format(TIME_FMT), y);
            y = drawLabel(cs, "FORMATO", ticket.getFormatLabel(), y);

            // --- Separador ---
            y -= 4;
            drawLine(cs, MARGIN, y, RIGHT_EDGE, y, 0.85f, 0.85f, 0.85f);
            y -= 14;

            // --- Datos de la butaca y comprador ---
            y = drawLabel(cs, "BUTACA", ticket.getSeatLabel() + "  (" + ticket.getSeatTypeName() + ")", y);
            y = drawLabel(cs, "DNI / ID", ticket.isAnonymous()
                    ? "No registrado — sin derechos de gestión"
                    : ticket.getBuyerIdNumber(), y);

            // --- Separador ---
            y -= 4;
            drawLine(cs, MARGIN, y, RIGHT_EDGE, y, 0.85f, 0.85f, 0.85f);
            y -= 14;

            // --- Trazabilidad de precio ---
            if (ticket.hasDiscounts()) {
                y = drawPriceLine(cs, "Precio original", ticket.getOriginalPrice(), y, false);
                for (String discount : ticket.getAppliedDiscountNames()) {
                    y = drawPriceLine(cs, discount, -(ticket.getOriginalPrice() - ticket.getPricePaid()), y, false);
                }
                drawLine(cs, MARGIN, y + 2, RIGHT_EDGE, y + 2, 0.7f, 0.7f, 0.7f);
                y -= 4;
            }

            // --- Total pagado ---
            y = drawTotalLine(cs, "TOTAL PAGADO", ticket.getPricePaid(), y);

            // --- Pie de página ---
            float footerY = MARGIN + 10;
            drawLine(cs, MARGIN, footerY + 12, RIGHT_EDGE, footerY + 12, 0.85f, 0.85f, 0.85f);
            drawText(cs, FONT_OBLIQ, 7,
                    "Emitido: " + ticket.getIssuedAt().format(ISSUED_FMT)
                    + "    ID interno: " + ticket.getId().substring(0, 8).toUpperCase() + "…",
                    MARGIN, footerY, 0.5f, 0.5f, 0.5f);
        }
    }

    // =====================================================================
    // Helpers de dibujo
    // =====================================================================

    /** Dibuja una línea etiqueta-valor en dos columnas. */
    private static float drawLabel(PDPageContentStream cs, String label, String value, float y) throws IOException {
        drawText(cs, FONT_BOLD, 8, label, MARGIN, y, 0.4f, 0.4f, 0.5f);
        drawText(cs, FONT_REG,  10, value, MARGIN + 90, y, 0.1f, 0.1f, 0.1f);
        return y - 16;
    }

    /** Dibuja una línea de precio (label + monto alineado a la derecha). */
    private static float drawPriceLine(PDPageContentStream cs, String label, double amount,
                                       float y, boolean bold) throws IOException {
        PDFont f = bold ? FONT_BOLD : FONT_REG;
        int sz = bold ? 11 : 9;
        drawText(cs, f, sz, label, MARGIN, y, 0.2f, 0.2f, 0.2f);
        String amountStr = String.format("$ %.2f", amount);
        drawTextRight(cs, f, sz, amountStr, RIGHT_EDGE, y, 0.2f, 0.2f, 0.2f);
        return y - 15;
    }

    /** Dibuja la línea del total con formato destacado. */
    private static float drawTotalLine(PDPageContentStream cs, String label, double amount, float y)
            throws IOException {
        // Fondo resaltado
        fillRect(cs, MARGIN - 4, y - 5, COL_WIDTH + 8, 22, 0.12f, 0.12f, 0.18f);
        drawText(cs, FONT_BOLD, 12, label, MARGIN + 2, y + 4, 1f, 1f, 1f);
        drawTextRight(cs, FONT_BOLD, 13, String.format("$ %.2f", amount), RIGHT_EDGE - 2, y + 4, 0.9f, 0.8f, 0.2f);
        return y - 26;
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
        float textWidth = font.getStringWidth(sanitize(text)) / 1000 * size;
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

    /** Elimina caracteres que PDFBox (Helvetica) no puede codificar en Latin-1. */
    private static String sanitize(String text) {
        if (text == null) return "";
        return text.chars()
                .filter(c -> c < 256)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
