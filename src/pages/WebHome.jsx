import { useState, useMemo, useRef } from 'react';
import './WebHome.css';
import { useNavigate } from 'react-router-dom';
import { useCinema } from '../context/CinemaContext';
import CustomDropdown from '../components/CustomDropdown';
import cineramaBanner from '../assets/CineramaBanner.png';
import heroImg from '../assets/hero.png';
import { usePageTitle } from '../hooks/usePageTitle';

export default function WebHome() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('cartelera');
  usePageTitle('Inicio');

  const { branches, enabledMovies } = useCinema();

  const [currentSlide, setCurrentSlide] = useState(0);
  const [selectedMovie, setSelectedMovie] = useState(null);
  const [selectedCity, setSelectedCity] = useState(null);
  const [selectedCinema, setSelectedCinema] = useState(null);
  const [selectedDate, setSelectedDate] = useState(null);

  const tabRefs = {
    cartelera: useRef(null),
    preventa: useRef(null),
    proximamente: useRef(null),
  };

  const cities = [...new Set(branches.map(b => b.city))];
  const dateOptions = ['Hoy Lunes', 'Martes 19', 'Miércoles 20'];

  // Slides del carrusel promocional con contenido e imágenes reales (WCAG 1.1.1)
  const slides = useMemo(() => [
    {
      img: cineramaBanner,
      alt: 'Banner promocional de Cinerama: Estrenos y funciones de la semana en todas las salas',
      title: 'Vive el cine en Cinerama'
    },
    {
      img: enabledMovies[0]?.backdrop || cineramaBanner,
      alt: `Gran estreno: ${enabledMovies[0]?.title || 'Película destacada'} ya en cartelera`,
      title: enabledMovies[0]?.title || 'Gran Estreno'
    },
    {
      img: heroImg,
      alt: 'Promoción de dulcería: Combos de canchita y gaseosa con descuento para socios',
      title: 'Combos de Dulcería'
    },
    {
      img: enabledMovies[1]?.backdrop || cineramaBanner,
      alt: `Preventa exclusiva: ${enabledMovies[1]?.title || 'Preventa'} — Asegura tus mejores asientos`,
      title: enabledMovies[1]?.title || 'Preventa Exclusiva'
    },
    {
      img: enabledMovies[2]?.backdrop || cineramaBanner,
      alt: `Próximo estreno en cartelera: ${enabledMovies[2]?.title || 'Próximamente'}`,
      title: enabledMovies[2]?.title || 'Próximamente'
    }
  ], [enabledMovies]);

  const handleSearch = () => {
    let movieId;
    if (selectedMovie) {
      const movie = enabledMovies.find(m => m.title === selectedMovie);
      if (movie) movieId = movie.id;
    }
    
    if (movieId) {
      navigate(`/web/pelicula/${movieId}`, {
        state: {
          city: selectedCity,
          cinema: selectedCinema,
          date: selectedDate
        }
      });
    } else {
      navigate('/web/peliculas');
    }
  };

  const TABS = ['cartelera', 'preventa', 'proximamente'];
  const TAB_LABELS = {
    cartelera: 'En Cartelera',
    preventa: 'En Preventa',
    proximamente: 'Próximamente',
  };

  const handleTabKeyDown = (e, currentTab) => {
    const idx = TABS.indexOf(currentTab);
    let nextTab = null;
    if (e.key === 'ArrowRight') {
      e.preventDefault();
      nextTab = TABS[(idx + 1) % TABS.length];
    } else if (e.key === 'ArrowLeft') {
      e.preventDefault();
      nextTab = TABS[(idx - 1 + TABS.length) % TABS.length];
    }
    if (nextTab) {
      setActiveTab(nextTab);
      tabRefs[nextTab]?.current?.focus();
    }
  };

  // Filtrado real según la pestaña seleccionada (WCAG 4.1.2)
  const displayedMovies = useMemo(() => {
    if (activeTab === 'preventa') {
      const filtered = enabledMovies.filter(m => m.featured);
      return filtered.length > 0 ? filtered : enabledMovies.slice(1, 4);
    }
    if (activeTab === 'proximamente') {
      const filtered = enabledMovies.filter(m => !m.featured);
      return filtered.length > 0 ? filtered.slice(2, 6) : enabledMovies.slice(0, 3);
    }
    return enabledMovies.slice(0, 5);
  }, [enabledMovies, activeTab]);

  return (
    <>
      {/* Hero Banner */}
      <section className="web-hero" aria-label="Banner promocional principal">
        <div className="hero-carousel">
          <button
            type="button"
            className="carousel-control prev"
            aria-label="Ver imagen anterior del carrusel"
            onClick={() => setCurrentSlide(prev => (prev > 0 ? prev - 1 : slides.length - 1))}
          >
            &#x27E8;
          </button>
          
          <img
            src={slides[currentSlide].img}
            alt={slides[currentSlide].alt}
            className="hero-img"
          />

          <button
            type="button"
            className="carousel-control next"
            aria-label="Ver siguiente imagen del carrusel"
            onClick={() => setCurrentSlide(prev => (prev < slides.length - 1 ? prev + 1 : 0))}
          >
            &#x27E9;
          </button>
          
          <div className="carousel-indicators" role="tablist" aria-label="Selector de imagen del carrusel">
            {slides.map((s, idx) => (
              <button
                key={idx}
                type="button"
                role="tab"
                aria-selected={currentSlide === idx}
                aria-label={`Mostrar diapositiva ${idx + 1} de ${slides.length}: ${s.title}`}
                className={`dot ${currentSlide === idx ? 'active' : ''}`}
                onClick={() => setCurrentSlide(idx)}
              />
            ))}
          </div>
        </div>
      </section>

      {/* Buscador de Funciones */}
      <section className="web-search-bar" aria-label="Buscador de funciones">
        <div className="search-filters">
          <CustomDropdown
            label="Por Película"
            placeholder="¿Qué quieres ver?"
            options={enabledMovies.map(m => m.title)}
            value={selectedMovie}
            onChange={setSelectedMovie}
          />
          
          <div className="filter-divider" aria-hidden="true"></div>
          
          <CustomDropdown
            label="Por Ciudad"
            placeholder="¿Dónde?"
            options={cities}
            value={selectedCity}
            onChange={setSelectedCity}
          />
          
          <div className="filter-divider" aria-hidden="true"></div>
          
          <CustomDropdown
            label="Por Cine"
            placeholder="¿En qué cine?"
            options={branches.map(b => b.name)}
            value={selectedCinema}
            onChange={setSelectedCinema}
          />
          
          <div className="filter-divider" aria-hidden="true"></div>
          
          <CustomDropdown
            label="Por Fecha"
            placeholder="Elige un día"
            options={dateOptions}
            value={selectedDate}
            onChange={setSelectedDate}
          />
          
          <button type="button" className="btn-buscar" onClick={handleSearch}>
            Buscar
          </button>
        </div>
      </section>

      {/* Cartelera */}
      <section className="web-movies-section" aria-label="Películas en cartelera">
        <h2 className="section-title">Películas</h2>
        
        <div className="movies-header">
          <div className="movies-tabs" role="tablist" aria-label="Filtro de cartelera">
            {TABS.map(tab => (
              <button
                key={tab}
                ref={tabRefs[tab]}
                type="button"
                role="tab"
                id={`tab-${tab}`}
                aria-selected={activeTab === tab}
                aria-controls="panel-cartelera"
                tabIndex={activeTab === tab ? 0 : -1}
                className={`tab-btn ${activeTab === tab ? 'active' : ''}`}
                onClick={() => setActiveTab(tab)}
                onKeyDown={e => handleTabKeyDown(e, tab)}
              >
                {TAB_LABELS[tab]}
              </button>
            ))}
          </div>
          <button type="button" className="btn-catalogo" onClick={() => navigate('/web/peliculas')}>
            Ver catálogo
          </button>
        </div>
        <div className="movies-divider" aria-hidden="true"></div>

        <div
          id="panel-cartelera"
          role="tabpanel"
          aria-labelledby={`tab-${activeTab}`}
          className="movies-grid"
        >
          {displayedMovies.map(movie => (
            <a
              key={movie.id}
              href={`/web/pelicula/${movie.id}`}
              className="movie-poster"
              aria-label={`Ver detalle y funciones de ${movie.title}`}
              onClick={e => { e.preventDefault(); navigate(`/web/pelicula/${movie.id}`); }}
              style={{position: 'relative'}}
            >
              <img src={movie.poster} alt={`Póster de ${movie.title}`} />
              <div className="movie-hover-overlay" aria-hidden="true">
                <span>Ver detalle &#x27E9;</span>
              </div>
            </a>
          ))}
        </div>
      </section>
    </>
  );
}
