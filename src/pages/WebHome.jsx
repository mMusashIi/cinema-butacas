import { useState } from 'react';
import './WebHome.css';
import { useNavigate } from 'react-router-dom';
import { useCinema } from '../context/CinemaContext';
import CustomDropdown from '../components/CustomDropdown';
import cineramaBanner from '../assets/CineramaBanner.png';

export default function WebHome() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('cartelera');
  const { branches, enabledMovies } = useCinema();

  const [currentSlide, setCurrentSlide] = useState(0);
  const [selectedMovie, setSelectedMovie] = useState(null);
  const [selectedCity, setSelectedCity] = useState(null);
  const [selectedCinema, setSelectedCinema] = useState(null);
  const [selectedDate, setSelectedDate] = useState(null);

  const cities = [...new Set(branches.map(b => b.city))];
  const dateOptions = ['Hoy Lunes', 'Martes 19', 'Miércoles 20'];

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

  return (
    <>
      {/* Hero Banner */}
      <section className="web-hero" aria-label="Destacados y promociones">
        <div className="hero-carousel">
          <button
            type="button"
            className="carousel-control prev"
            aria-label="Diapositiva anterior"
            onClick={() => setCurrentSlide(prev => (prev > 0 ? prev - 1 : 4))}
          >
            〈
          </button>
          <img
            src={cineramaBanner}
            alt="Cartelera y estrenos disponibles en Cinerama"
            className="hero-img"
          />
          <button
            type="button"
            className="carousel-control next"
            aria-label="Siguiente diapositiva"
            onClick={() => setCurrentSlide(prev => (prev < 4 ? prev + 1 : 0))}
          >
            〉
          </button>
          <div className="carousel-indicators" role="tablist" aria-label="Selector de diapositiva">
            {[0, 1, 2, 3, 4].map((idx) => (
              <button
                key={idx}
                type="button"
                role="tab"
                aria-label={`Ir a la diapositiva ${idx + 1}`}
                aria-selected={currentSlide === idx}
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
          
          <button type="button" className="btn-buscar" onClick={handleSearch}>Buscar</button>
        </div>
      </section>

      {/* Cartelera */}
      <section className="web-movies-section" aria-label="Catálogo de películas">
        <h2 className="section-title">Películas</h2>
        
        <div className="movies-header">
          <div className="movies-tabs" role="tablist" aria-label="Categorías de películas">
            <button 
              type="button"
              role="tab"
              aria-selected={activeTab === 'cartelera'}
              className={`tab-btn ${activeTab === 'cartelera' ? 'active' : ''}`}
              onClick={() => setActiveTab('cartelera')}
            >
              En Cartelera
            </button>
            <button 
              type="button"
              role="tab"
              aria-selected={activeTab === 'preventa'}
              className={`tab-btn ${activeTab === 'preventa' ? 'active' : ''}`}
              onClick={() => setActiveTab('preventa')}
            >
              En Preventa
            </button>
            <button 
              type="button"
              role="tab"
              aria-selected={activeTab === 'proximamente'}
              className={`tab-btn ${activeTab === 'proximamente' ? 'active' : ''}`}
              onClick={() => setActiveTab('proximamente')}
            >
              Próximamente
            </button>
          </div>
          <button type="button" className="btn-catalogo" onClick={() => navigate('/web/peliculas')}>Ver catálogo</button>
        </div>
        <div className="movies-divider" aria-hidden="true"></div>

        <div className="movies-grid">
          {enabledMovies.slice(0, 5).map(movie => (
            <a
              key={movie.id}
              href={`/web/pelicula/${movie.id}`}
              className="movie-poster"
              aria-label={`Ver detalle de ${movie.title}`}
              style={{position: 'relative'}}
            >
              <img src={movie.poster} alt={`Póster de ${movie.title}`} />
              <div className="movie-hover-overlay" aria-hidden="true">
                <span>Ver detalle ⟩</span>
              </div>
            </a>
          ))}
        </div>
      </section>
    </>
  );
}
