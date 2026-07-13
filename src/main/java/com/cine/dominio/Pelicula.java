package com.cine.dominio;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Representa una película dentro del catálogo del sistema.
 * Entidad con identidad propia y ciclo de vida comercial controlado
 * por EstadoPelicula (PROXIMO → PREVENTA → VENTA → RETIRADO).
 *
 * Regla de negocio: la progresión de estados es secuencial en el flujo
 * normal. RETIRADO puede ocurrir desde cualquier estado.
 * Pelicula NO conoce en qué salas se proyecta ni en qué funciones aparece —
 * eso lo administran SalaCine, Funcion y DisponibilidadPelicula.
 */
public class Pelicula {

    private final String id;
    private String title;
    private int durationMinutes;
    private String classification;
    private String genre;
    private String synopsis;
    private String posterPath;
    private String originalLanguage;
    private LocalDate releaseDate;    // Requerido antes de pasar a PREVENTA
    private EstadoPelicula estado;

    /**
     * Constructor principal. El estado inicial siempre es PROXIMO.
     *
     * @param title           Título de la película (obligatorio)
     * @param durationMinutes Duración en minutos (debe ser positivo)
     * @param classification  Clasificación por edad (ej. "PG-13", "R")
     */
    public Pelicula(String title, int durationMinutes, String classification) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("El título (title) no puede estar vacío");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("La duración (durationMinutes) debe ser un valor positivo");
        }
        if (classification == null || classification.trim().isEmpty()) {
            throw new IllegalArgumentException("La clasificación (classification) no puede estar vacía");
        }

        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.durationMinutes = durationMinutes;
        this.classification = classification;
        this.estado = EstadoPelicula.PROXIMO; // Siempre nace en PROXIMO
    }

    /**
     * Constructor para restaurar desde la base de datos.
     */
    public Pelicula(String id, String title, int durationMinutes, String classification, 
                    String genre, String synopsis, String posterPath, String originalLanguage, 
                    LocalDate releaseDate, EstadoPelicula estado) {
        this.id = id;
        this.title = title;
        this.durationMinutes = durationMinutes;
        this.classification = classification;
        this.genre = genre;
        this.synopsis = synopsis;
        this.posterPath = posterPath;
        this.originalLanguage = originalLanguage;
        this.releaseDate = releaseDate;
        this.estado = estado;
    }

    // --- Métodos de ciclo de vida (transiciones de estado) ---

    /**
     * Avanza de PROXIMO a PREVENTA (opcional).
     * Precondición: releaseDate debe estar definido antes de llamar este método.
     * PREVENTA es opcional — se puede ir directo a VENTA desde PROXIMO usando startSale().
     */
    public void startPresale() {
        if (this.estado != EstadoPelicula.PROXIMO) {
            throw new IllegalStateException(
                String.format("No se puede iniciar preventa: la película '%s' está en estado %s", title, estado)
            );
        }
        if (this.releaseDate == null) {
            throw new IllegalStateException(
                String.format("No se puede iniciar preventa de '%s' sin una fecha de estreno definida", title)
            );
        }
        this.estado = EstadoPelicula.PREVENTA;
    }

    /**
     * Avanza a VENTA. Acepta tanto PROXIMO como PREVENTA como estado origen —
     * PREVENTA es opcional en el flujo.
     * Si viene desde PROXIMO, exige que releaseDate esté definido (mismo requisito
     * que startPresale, para no llegar a VENTA sin fecha de estreno).
     */
    public void startSale() {
        if (this.estado != EstadoPelicula.PREVENTA && this.estado != EstadoPelicula.PROXIMO) {
            throw new IllegalStateException(
                String.format("No se puede iniciar venta: la película '%s' está en estado %s", title, estado)
            );
        }
        // Si salta desde PROXIMO directamente, exigir fecha de estreno igual
        if (this.estado == EstadoPelicula.PROXIMO && this.releaseDate == null) {
            throw new IllegalStateException(
                String.format("No se puede poner en venta '%s' sin una fecha de estreno definida", title)
            );
        }
        this.estado = EstadoPelicula.VENTA;
    }

    /**
     * Retira la película del catálogo.
     * Disponible desde cualquier estado — no requiere secuencia previa.
     */
    public void retire() {
        if (this.estado == EstadoPelicula.RETIRADO) {
            throw new IllegalStateException(
                String.format("La película '%s' ya está retirada", title)
            );
        }
        this.estado = EstadoPelicula.RETIRADO;
    }

    // --- Consultas de negocio (delegando en EstadoPelicula) ---

    /**
     * Indica si se puede crear una función (Funcion) para esta película.
     * Funcion consulta este método, no el enum directamente.
     */
    public boolean canHaveShowtime() {
        return estado.canCreateShowtime();
    }

    /**
     * Indica si la película debe mostrarse en la cartelera o listado general.
     */
    public boolean isVisibleInBillboard() {
        return estado.isVisibleInBillboard();
    }

    // --- Getters ---

    public String getId() {
        return id;
    }

    public String getTitulo() {
        return title;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public String getClassification() {
        return classification;
    }

    public String getGenre() {
        return genre;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public String getOriginalLanguage() {
        return originalLanguage;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public EstadoPelicula getEstado() {
        return estado;
    }

    // --- Setters de campos editables ---

    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("El título (title) no puede estar vacío");
        }
        this.title = title;
    }

    public void setDurationMinutes(int durationMinutes) {
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("La duración (durationMinutes) debe ser un valor positivo");
        }
        this.durationMinutes = durationMinutes;
    }

    public void setClassification(String classification) {
        if (classification == null || classification.trim().isEmpty()) {
            throw new IllegalArgumentException("La clasificación (classification) no puede estar vacía");
        }
        this.classification = classification;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public void setPosterPath(String posterPath) {
        this.posterPath = posterPath;
    }

    public void setOriginalLanguage(String originalLanguage) {
        this.originalLanguage = originalLanguage;
    }

    /**
     * Establece la fecha de estreno.
     * Requerida antes de llamar a startPresale().
     */
    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    @Override
    public String toString() {
        return String.format("Pelicula{id='%s', title='%s', estado=%s, duration=%dmin}",
                id, title, estado, durationMinutes);
    }
}
