import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCinema } from '../context/CinemaContext';
import { usePageTitle } from '../hooks/usePageTitle';
import './WebPages.css';

export default function WebMovies() {
  const navigate = useNavigate();
  usePageTitle('Películas');
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

  // Listas para los acordeones
  const filterOptions = useMemo(() => {
    const cities = [...new Set(branches.map(b => b.city))];
    const cinemas = branches.map(b => b.name);
    const genres = [...new Set(enabledMovies.map(m => m.genre))];
    const languages = [...new Set(enabledMovies.map(m => m.language === 'ES' ? 'Español' : 'Subtitulada'))];
    const formats = [...new Set(enabledMovies.map(m => m.format).flat())];
    const days = ['Hoy Lunes', 'Martes 19', 'Miércoles 20'];

    return { cities, cinemas, genres, languages, formats, days };
  }, [branches, enabledMovies]);

  // Filtrado de Películas
  const filteredMovies = useMemo(() => {
    return enabledMovies.filter(movie => {
      if (filters.genre && movie.genre !== filters.genre) return false;
      if (filters.language) {
        const langStr = movie.language === 'ES' ? 'Español' : 'Subtitulada';
        if (langStr !== filters.language) return false;
      }
      if (filters.format && !movie.format.includes(filters.format)) return false;
      return true;
    });
  }, [enabledMovies, filters]);

  const activePills = Object.entries(filters).filter(([_, val]) => val !== null);

  return (
    <div className="web-movies-layout">
      {/* Sidebar de Filtros */}
      <aside className="movies-sidebar" aria-label="Filtros del catálogo">
        <div className="filter-header">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <line x1="4" y1="21" x2="4" y2="14"></line>
            <line x1="4" y1="10" x2="4" y2="3"></line>
            <line x1="12" y1="21" x2="12" y2="12"></line>
            <line x1="12" y1="8" x2="12" y2="3"></line>
            <line x1="20" y1="21" x2="20" y2="16"></line>
            <line x1="20" y1="12" x2="20" y2="3"></line>
            <line x1="1" y1="14" x2="7" y2="14"></line>
            <line x1="9" y1="8" x2="15" y2="8"></line>
            <line x1="17" y1="16" x2="23" y2="16"></line>
          </svg>
          <h2 style={{ fontSize: '1.1rem', margin: 0 }}>Filtros</h2>
        </div>

        {/* Acordeón Ciudad */}
        <div className="accordion-filter">
          <button
            type="button"
            className="filter-title-btn"
            onClick={() => toggleAccordion('Ciudad')}
            aria-expanded={openAccordion === 'Ciudad'}
            aria-controls="filter-panel-ciudad"
          >
            <span>Ciudad</span> <span>{openAccordion === 'Ciudad' ? '−' : '+'}</span>
          </button>
          {openAccordion === 'Ciudad' && (
            <div id="filter-panel-ciudad" className="filter-options">
              {filterOptions.cities.map(city => (
                <button
                  type="button"
                  key={city}
                  className={`filter-option-btn ${filters.city === city ? 'active-option' : ''}`}
                  aria-pressed={filters.city === city}
                  onClick={() => setFilter('city', city)}
                >
                  {city}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Acordeón Cine */}
        <div className="accordion-filter">
          <button
            type="button"
            className="filter-title-btn"
            onClick={() => toggleAccordion('Cine')}
            aria-expanded={openAccordion === 'Cine'}
            aria-controls="filter-panel-cine"
          >
            <span>Cine</span> <span>{openAccordion === 'Cine' ? '−' : '+'}</span>
          </button>
          {openAccordion === 'Cine' && (
            <div id="filter-panel-cine" className="filter-options">
              {filterOptions.cinemas.map(cine => (
                <button
                  type="button"
                  key={cine}
                  className={`filter-option-btn ${filters.cinema === cine ? 'active-option' : ''}`}
                  aria-pressed={filters.cinema === cine}
                  onClick={() => setFilter('cinema', cine)}
                >
                  {cine}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Acordeón Día */}
        <div className="accordion-filter">
          <button
            type="button"
            className="filter-title-btn"
            onClick={() => toggleAccordion('Día')}
            aria-expanded={openAccordion === 'Día'}
            aria-controls="filter-panel-dia"
          >
            <span className={openAccordion === 'Día' ? 'active-text' : ''}>Día</span> 
            <span>{openAccordion === 'Día' ? '−' : '+'}</span>
          </button>
          {openAccordion === 'Día' && (
            <div id="filter-panel-dia" className="filter-options">
              {filterOptions.days.map(day => (
                <button
                  type="button"
                  key={day}
                  className={`filter-option-btn ${filters.day === day ? 'active-option' : ''}`}
                  aria-pressed={filters.day === day}
                  onClick={() => setFilter('day', day)}
                >
                  {day}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Acordeón Género */}
        <div className="accordion-filter">
          <button
            type="button"
            className="filter-title-btn"
            onClick={() => toggleAccordion('Género')}
            aria-expanded={openAccordion === 'Género'}
            aria-controls="filter-panel-genero"
          >
            <span>Género</span> <span>{openAccordion === 'Género' ? '−' : '+'}</span>
          </button>
          {openAccordion === 'Género' && (
            <div id="filter-panel-genero" className="filter-options">
              {filterOptions.genres.map(genre => (
                <button
                  type="button"
                  key={genre}
                  className={`filter-option-btn ${filters.genre === genre ? 'active-option' : ''}`}
                  aria-pressed={filters.genre === genre}
                  onClick={() => setFilter('genre', genre)}
                >
                  {genre}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Acordeón Idioma */}
        <div className="accordion-filter">
          <button
            type="button"
            className="filter-title-btn"
            onClick={() => toggleAccordion('Idioma')}
            aria-expanded={openAccordion === 'Idioma'}
            aria-controls="filter-panel-idioma"
          >
            <span>Idioma</span> <span>{openAccordion === 'Idioma' ? '−' : '+'}</span>
          </button>
          {openAccordion === 'Idioma' && (
            <div id="filter-panel-idioma" className="filter-options">
              {filterOptions.languages.map(lang => (
                <button
                  type="button"
                  key={lang}
                  className={`filter-option-btn ${filters.language === lang ? 'active-option' : ''}`}
                  aria-pressed={filters.language === lang}
                  onClick={() => setFilter('language', lang)}
                >
                  {lang}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Acordeón Formato */}
        <div className="accordion-filter">
          <button
            type="button"
            className="filter-title-btn"
            onClick={() => toggleAccordion('Formato')}
            aria-expanded={openAccordion === 'Formato'}
            aria-controls="filter-panel-formato"
          >
            <span>Formato</span> <span>{openAccordion === 'Formato' ? '−' : '+'}</span>
          </button>
          {openAccordion === 'Formato' && (
            <div id="filter-panel-formato" className="filter-options">
              {filterOptions.formats.map(format => (
                <button
                  type="button"
                  key={format}
                  className={`filter-option-btn ${filters.format === format ? 'active-option' : ''}`}
                  aria-pressed={filters.format === format}
                  onClick={() => setFilter('format', format)}
                >
                  {format}
                </button>
              ))}
            </div>
          )}
        </div>
      </aside>

      {/* Grilla de Películas */}
      <section className="movies-list" aria-label="Lista de películas encontradas">
        <div className="movies-top-bar">
          <h1 style={{ fontSize: '1.4rem', margin: 0 }}>
            <span className="count" aria-live="polite">{filteredMovies.length}</span> Películas encontradas
          </h1>
          {activePills.length > 0 && (
            <div className="active-filters-pills" aria-label="Filtros aplicados">
              {activePills.map(([key, val]) => (
                <button
                  type="button"
                  key={key}
                  className="pill"
                  onClick={() => removeFilter(key)}
                  aria-label={`Quitar filtro ${val}`}
                >
                  {val} <span aria-hidden="true">✕</span>
                </button>
              ))}
            </div>
          )}
        </div>
        
        <div className="movies-grid-view">
          {filteredMovies.map(movie => (
            <div key={movie.id} className="movie-grid-card">
              <a
                href={`/web/pelicula/${movie.id}`}
                className="poster-container"
                onClick={e => { e.preventDefault(); navigate(`/web/pelicula/${movie.id}`); }}
                aria-label={`Ver detalles y funciones de ${movie.title}`}
              >
                <img src={movie.poster} alt={`Póster de la película ${movie.title}`} />
                
                {movie.featured && (
                  <div className="diagonal-tag" aria-label="Estreno">Estreno</div>
                )}
                
                {/* Overlay de Hover unificado */}
                <div className="movie-hover-overlay" aria-hidden="true">
                  <span>Ver detalle &#x27E9;</span>
                </div>
              </a>
              
              <div className="movie-card-info">
                <h4>{movie.title}</h4>
                <p>{movie.genre}, {Math.floor(movie.duration / 60)}h {movie.duration % 60}min, {movie.rating}.</p>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
