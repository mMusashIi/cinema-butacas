package com.cine.dominio.boletos;

import com.cine.dominio.Butaca;
import com.cine.dominio.Funcion;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Boleta de compra — registro inmutable de que una butaca fue vendida
 * para una función a un precio determinado en un momento preciso.
 *
 * INMUTABILIDAD: todos los campos son final. El precio pagado, los descuentos
 * aplicados, la dirección del cine y la clasificación de la película se
 * congelan en el momento de emisión — si esos datos cambian después,
 * la boleta ya emitida sigue siendo fiel al estado del sistema al momento
 * de la compra.
 *
 * buyerIdNumber es opcional: si el comprador no entrega DNI, se usa el valor
 * por defecto ANONYMOUS_ID. Sin DNI el comprador pierde derechos de gestión
 * (reembolsos, reclamos), pero la boleta sigue siendo válida para el acceso.
 *
 * Flujo de una compra:
 *   1. butaca.seleccionar()
 *   2. precio = motor.calcularPrecio(butaca, funcion)
 *   3. [usuario confirma pago]
 *   4. butaca.reservarButaca()
 *   5. new Boleto(constructor)   ← emitir boleta
 *   6. GeneradorPdfBoleto.generate(ticket)
 */
public class Boleto {

    public static final String ANONYMOUS_ID = "SIN_DNI";

    // Contador para generar referenceCode legible y único por JVM
    private static final AtomicInteger counter = new AtomicInteger(1);
    private static final DateTimeFormatter REF_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // --- Identidad ---
    private final String id;             // UUID interno
    private final String referenceCode;  // Código legible: TK-20250711-000042

    // --- Comprador ---
    private final String buyerIdNumber;  // DNI o ANONYMOUS_ID

    // --- Qué se compró ---
    private final Butaca butaca;
    private final Funcion funcion;

    // --- Precio y trazabilidad ---
    private final double originalPrice;            // Precio antes de descuentos
    private final double pricePaid;                // Precio final pagado (congelado)
    private final List<String> appliedDiscountNames; // Nombres de reglas que aplicaron

    // --- Datos del contexto congelados al momento de compra ---
    private final String cinemaAddress;       // Dirección del local (puede cambiar a futuro)
    private final String movieClassification; // Clasificación de la película (ej. "PG-13")

    // --- Tiempo ---
    private final LocalDateTime issuedAt;

    private Boleto(Builder constructor) {
        this.id = UUID.randomUUID().toString();
        this.referenceCode = generateReferenceCode();
        this.buyerIdNumber = constructor.buyerIdNumber != null && !constructor.buyerIdNumber.trim().isEmpty()
                ? constructor.buyerIdNumber.trim()
                : ANONYMOUS_ID;
        this.butaca = constructor.butaca;
        this.funcion = constructor.funcion;
        this.originalPrice = constructor.originalPrice;
        this.pricePaid = constructor.pricePaid;
        this.appliedDiscountNames = Collections.unmodifiableList(constructor.appliedDiscountNames);
        // Congelar datos de contexto en el momento de emisión
        this.cinemaAddress = constructor.funcion.getSala().getCinema().getAddress() != null
                ? constructor.funcion.getSala().getCinema().getAddress()
                : "";
        this.movieClassification = constructor.funcion.getPelicula().getClassification();
        this.issuedAt = LocalDateTime.now();
    }

    private static String generateReferenceCode() {
        String date = LocalDateTime.now().format(REF_DATE_FMT);
        return String.format("TK-%s-%06d", date, counter.getAndIncrement());
    }

    // =====================================================================
    // Builder
    // =====================================================================

    public static class Builder {
        private Butaca butaca;
        private Funcion funcion;
        private double originalPrice;
        private double pricePaid;
        private List<String> appliedDiscountNames = Collections.emptyList();
        private String buyerIdNumber;

        public Builder butaca(Butaca butaca) {
            this.butaca = butaca;
            return this;
        }

        public Builder funcion(Funcion funcion) {
            this.funcion = funcion;
            return this;
        }

        /**
         * @param originalPrice Precio base antes de descuentos (solo multiplicadores de formato/tipo)
         */
        public Builder originalPrice(double originalPrice) {
            this.originalPrice = originalPrice;
            return this;
        }

        /**
         * @param pricePaid Precio final después de aplicar todas las reglas de descuento
         */
        public Builder pricePaid(double pricePaid) {
            this.pricePaid = pricePaid;
            return this;
        }

        /**
         * @param names Lista de nombres de las reglas que redujeron el precio
         *              (ej. ["Día del espectador (-50%)", "Matiné (-20%)"])
         */
        public Builder appliedDiscountNames(List<String> names) {
            this.appliedDiscountNames = names != null ? names : Collections.emptyList();
            return this;
        }

        /**
         * DNI del comprador. Si no se provee (o es vacío), la boleta usa ANONYMOUS_ID.
         * Sin DNI el comprador pierde derechos de gestión fuera de la lógica de negocio.
         */
        public Builder buyerIdNumber(String buyerIdNumber) {
            this.buyerIdNumber = buyerIdNumber;
            return this;
        }

        public Boleto build() {
            if (butaca == null) throw new IllegalStateException("Boleto: falta configurar butaca");
            if (funcion == null) throw new IllegalStateException("Boleto: falta configurar funcion");
            if (originalPrice < 0) throw new IllegalStateException("Boleto: originalPrice no puede ser negativo");
            if (pricePaid < 0) throw new IllegalStateException("Boleto: pricePaid no puede ser negativo");
            return new Boleto(this);
        }
    }

    // =====================================================================
    // Consultas de conveniencia (para GeneradorPdfBoleto y la UI)
    // =====================================================================

    /** ¿El comprador es anónimo (no proporcionó DNI)? */
    public boolean isAnonymous() {
        return ANONYMOUS_ID.equals(buyerIdNumber);
    }

    /** ¿Se aplicó algún descuento? */
    public boolean hasDiscounts() {
        return !appliedDiscountNames.isEmpty();
    }

    /** Ahorro total: diferencia entre precio original y precio pagado */
    public double getTotalSavings() {
        return Math.max(0.0, originalPrice - pricePaid);
    }

    public String getSeatLabel()       { return butaca.getId(); }
    public String getSeatTypeName()    { return butaca.getTipo().displayName(); }
    public String getMovieTitle()      { return funcion.getPelicula().getTitulo(); }
    public String getRoomName()        { return funcion.getSala().getNombre(); }
    public String getCinemaName()      { return funcion.getSala().getCinema().getNombre(); }
    public String getFormatLabel()     { return funcion.getFormat().getDisplayName(); }

    // =====================================================================
    // Getters
    // =====================================================================

    public String getId()                        { return id; }
    public String getReferenceCode()             { return referenceCode; }
    public String getBuyerIdNumber()             { return buyerIdNumber; }
    public Butaca getButaca()                        { return butaca; }
    public Funcion getFuncion()                { return funcion; }
    public double getOriginalPrice()             { return originalPrice; }
    public double getPricePaid()                 { return pricePaid; }
    public List<String> getAppliedDiscountNames(){ return appliedDiscountNames; }
    public String getCinemaAddress()             { return cinemaAddress; }
    public String getMovieClassification()       { return movieClassification; }
    public LocalDateTime getIssuedAt()           { return issuedAt; }

    @Override
    public String toString() {
        return String.format(
            "Boleto{ref='%s', butaca='%s', pelicula='%s', paid=%.2f, buyer='%s'}",
            referenceCode, butaca.getId(), getMovieTitle(), pricePaid, buyerIdNumber
        );
    }
}
