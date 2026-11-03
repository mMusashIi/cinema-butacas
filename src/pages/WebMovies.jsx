import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCinema } from '../context/CinemaContext';
import './WebPages.css';

export default function WebMovies() {
  const navigate = useNavigate();
  const { enabledMovies, branches, activeScreenings } = useCinema();

  const [openAccordion, setOpenAccordion] = useState('Día');
  const [filters, setFilters] = useState({
    city: null,
    cinema: null,
    day: null,
    genre: null,
    language: null,
    format: null
  });

  const toggleAccordion = (name) => {
    setOpenAccordion(openAccordion === name ? null : name);
  };

  const setFilter = (key, value) => {
    setFilters(prev => ({ ...prev, [key]: prev[key] === value ? null : value }));
  };

  const removeFilter = (key) => {
    setFilters(prev => ({ ...prev, [key]: null }));
  };

  // Dynamic filter options
  const filterOptions = useMemo(() => {
    const cities = [...new Set(branches.map(b => b.city))];
    const cinemas = branches.map(b => b.name);
    const genres = [...new Set(enabledMovies.map(m => m.genre))];
    const languages = [...new Set(enabledMovies.map(m => m.language === 'ES' ? 'Doblada' : 'Subtitulada'))];
    const formats = [...new Set(enabledMovies.map(m => m.format).flat())];
    const days = ['Hoy Lunes', 'Martes 19', 'Miércoles 20'];
    return { cities, cinemas, genres, languages, formats, days };
  }, [branches, enabledMovies]);

  // Filter the movies
  const filteredMovies = useMemo(() => {
    return enabledMovies.filter(movie => {
      if (filters.genre && movie.genre !== filters.genre) return false;
      if (filters.language) {
        const langStr = movie.language === 'ES' ? 'Doblada' : 'Subtitulada';
        if (langStr !== filters.language) return false;
      }
      if (filters.format && !movie.format.includes(filters.format)) return false;
      if (filters.city || filters.cinema) {
        const relevantBranches = branches.filter(b =>
          (!filters.city || b.city === filters.city) &&
          (!filters.cinema || b.name === filters.cinema)
        );
        const branchIds = relevantBranches.map(b => b.id);
        const hasScreening = activeScreenings.some(s => s.movieId === movie.id && branchIds.includes(s.branchId));
        if (!hasScreening) return false;
      }
      return true;
    });
  }, [enabledMovies, branches, activeScreenings, filters]);

  // Render active filter pills
  const activePills = Object.entries(filters).filter(([, val]) => val !== null);

  // Configuración de los acordeones de filtros
  const accordionConfig = [
    { key: 'Ciudad',  items: filterOptions.cities,    filterKey: 'city' },
    { key: 'Cine',    items: filterOptions.cinemas,   filterKey: 'cinema' },
    { key: 'Día',     items: filterOptions.days,      filterKey: 'day' },
    { key: 'Género',  items: filterOptions.genres,    filterKey: 'genre' },
    { key: 'Idioma',  items: filterOptions.languages, filterKey: 'language' },
    { key: 'Formato', items: filterOptions.formats,   filterKey: 'format' },
  ];

  return (
    <div className="web-movies-layout">
      {/* Sidebar de Filtros */}
      <aside className="movies-sidebar" aria-label="Filtros de películas">
        <h3 className="filter-header">
          <span className="icon" aria-hidden="true">≑</span> Filtrar Por:
        </h3>

        {accordionConfig.map(({ key, items, filterKey }) => (
          <div className="accordion-filter" key={key}>
            {/* <button> con aria-expanded para operabilidad con teclado — WCAG 2.1.1 / 4.1.2 */}
            <button
              type="button"
              className="filter-title filter-title-btn"
              aria-expanded={openAccordion === key}
              onClick={() => toggleAccordion(key)}
            >
              <span className={openAccordion === key ? 'active-text' : ''}>{key}</span>
              <span aria-hidden="true">{openAccordion === key ? '−' : '+'}</span>
            </button>
            {openAccordion === key && (
              <div className="filter-options" role="group" aria-label={`Opciones de ${key}`}>
                {items.map(item => (
                  /* <button> en vez de <p onClick> — WCAG 2.1.1 / 4.1.2 */
                  <button
                    key={item}
                    type="button"
                    className={`filter-option-btn${filters[filterKey] === item ? ' active-option' : ''}`}
                    aria-pressed={filters[filterKey] === item}
                    onClick={() => setFilter(filterKey, item)}
                  >
                    {item}
                  </button>
                ))}
              </div>
            )}
          </div>
        ))}
      </aside>

      {/* Grilla de Películas */}
      <main className="movies-list">
        <div className="movies-top-bar">
          {/* aria-live anuncia el cambio de resultados al filtrar — WCAG 4.1.3 */}
          <h3 aria-live="polite" aria-atomic="true">
            <span className="count">{filteredMovies.length}</span> Películas encontradas
          </h3>
          <div className="active-filters-pills">
            {activePills.map(([key, val]) => (
              /* <button> con aria-label descriptivo — WCAG 4.1.2 / 2.5.3 */
              <button
                key={key}
                type="button"
                className="pill"
                aria-label={`Eliminar filtro: ${val}`}
                onClick={() => removeFilter(key)}
              >
                {val} ✕
              </button>
            ))}
          </div>
        </div>

        <div className="movies-grid-view">
          {filteredMovies.map(movie => (
            <div key={movie.id} className="movie-grid-card">
              <div className="poster-container">
                {/* <a> en vez de <div onClick> para que sea operable con teclado — WCAG 2.1.1 / 4.1.2 */}
                <a
                  href={`/web/pelicula/${movie.id}`}
                  className="movie-poster-link"
                  aria-label={`Ver detalle de ${movie.title}`}
                >
                  <img src={movie.poster} alt={`Póster de ${movie.title}`} />
                  {movie.featured && (
                    <div className="diagonal-tag" aria-hidden="true">Estreno</div>
                  )}
                  <div className="movie-hover-overlay" aria-hidden="true">
                    <span>Ver detalle ⟩</span>
                  </div>
                </a>
              </div>

              <div className="movie-card-info">
                <h4>{movie.title}</h4>
                <p>{movie.genre}, {Math.floor(movie.duration / 60)}h {movie.duration % 60}min, {movie.rating}.</p>
              </div>
            </div>
          ))}
        </div>
      </main>
    </div>
  );
}
