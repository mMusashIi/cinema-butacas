import { useState, useRef, useEffect } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { useCinema } from '../context/CinemaContext';
import CustomDropdown from '../components/CustomDropdown';
import { usePageTitle } from '../hooks/usePageTitle';
import './WebPages.css';

export default function WebMovieDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const showtimesRef = useRef(null);
  const trailerBtnRef = useRef(null);
  const trailerCloseBtnRef = useRef(null);
  const { enabledMovies, branches } = useCinema();

  const movie = enabledMovies.find(m => m.id === parseInt(id)) || enabledMovies[0];
  usePageTitle(movie ? `${movie.title}` : 'Detalle de Película');

  const [expandedCinema, setExpandedCinema] = useState('CP Arequipa Mall Plaza');
  const [filterCity, setFilterCity]   = useState(location.state?.city || null);
  const [filterCinema, setFilterCinema] = useState(location.state?.cinema || null);
  const [filterDate, setFilterDate]   = useState(location.state?.date || null);
  const [showTrailerModal, setShowTrailerModal] = useState(false);

  const cities  = [...new Set(branches.map(b => b.city))];
  const cinemaNames = branches.map(b => b.name);
  const dates = ['Hoy Lunes', 'Martes 19', 'Miércoles 20'];

  const toggleCinema = (cinemaName) => {
    setExpandedCinema(expandedCinema === cinemaName ? null : cinemaName);
  };

  const handleScrollToShowtimes = () => {
    showtimesRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  // Manejo de foco y tecla Escape en el modal de trailer
  useEffect(() => {
    if (showTrailerModal) {
      trailerCloseBtnRef.current?.focus();
      const handleKeyDown = (e) => {
        if (e.key === 'Escape') {
          setShowTrailerModal(false);
          trailerBtnRef.current?.focus();
        }
      };
      window.addEventListener('keydown', handleKeyDown);
      return () => window.removeEventListener('keydown', handleKeyDown);
    }
  }, [showTrailerModal]);

  const closeTrailer = () => {
    setShowTrailerModal(false);
    trailerBtnRef.current?.focus();
  };

  const cinemas = [
    { name: 'CP Arequipa Mall Plaza', showtimes: [{ time: '16:50', active: false }, { time: '21:30', active: true }] },
    { name: 'CP Brasil', showtimes: [{ time: '18:00', active: true }, { time: '20:30', active: true }] },
    { name: 'CP Centro Jr. De La Unión', showtimes: [{ time: '14:20', active: false }, { time: '19:15', active: true }] },
    { name: 'CP Chiclayo Mall Aventura', showtimes: [{ time: '16:00', active: false }, { time: '21:00', active: true }] },
    { name: 'CP Chiclayo Real Plaza', showtimes: [{ time: '17:30', active: true }, { time: '22:00', active: true }] },
    { name: 'CP Comas', showtimes: [{ time: '15:10', active: false }, { time: '20:00', active: true }] }
  ];

  return (
    <div className="movie-details-page">
      
      {/* Modal de Trailer Accesible */}
      {showTrailerModal && (
        <div className="booking-success-overlay" role="presentation" onClick={closeTrailer}>
          <div
            className="booking-success-modal trailer-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="trailer-modal-title"
            onClick={e => e.stopPropagation()}
            style={{ maxWidth: '640px', width: '90%' }}
          >
            <h2 id="trailer-modal-title">Tráiler: {movie.title}</h2>
            <div style={{ position: 'relative', paddingBottom: '56.25%', height: 0, overflow: 'hidden', borderRadius: '8px', margin: '1rem 0', background: '#000' }}>
              <iframe
                title={`Tráiler oficial de ${movie.title}`}
                src="https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ?autoplay=0"
                style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', border: 0 }}
                allow="accelerometer; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
              />
            </div>
            <button
              ref={trailerCloseBtnRef}
              type="button"
              className="btn-solid"
              onClick={closeTrailer}
              style={{ width: '100%' }}
            >
              Cerrar Tráiler
            </button>
          </div>
        </div>
      )}

      {/* 1. Banner superior con trailer */}
      <div className="movie-detail-banner" style={{ backgroundImage: `url(${movie.backdrop || movie.poster})`, backgroundSize: 'cover', backgroundPosition: 'center' }}>
        <button
          ref={trailerBtnRef}
          type="button"
          className="btn-play-trailer"
          aria-haspopup="dialog"
          aria-label={`Ver el tráiler oficial de ${movie.title}`}
          onClick={() => setShowTrailerModal(true)}
        >
          <span className="play-icon" style={{display: 'flex'}} aria-hidden="true">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
              <path d="M8 5v14l11-7z"/>
            </svg>
          </span>
          <span className="play-text">Mira el trailer</span>
        </button>
      </div>

      <div className="movie-detail-content">
        
        {/* 2. Cabecera principal con título y botón de comprar */}
        <div className="movie-header-section">
          <div className="movie-title-info">
            <h1>{movie.title}</h1>
            <p className="movie-meta-tags">
              {movie.genre} | {Math.floor(movie.duration / 60)}h {movie.duration % 60}min | {movie.rating}
            </p>
          </div>
          <button type="button" className="btn-comprar-top" onClick={handleScrollToShowtimes}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{marginRight: '8px', verticalAlign: 'text-bottom'}} aria-hidden="true">
              <path d="M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z"/><path d="M13 5v2"/><path d="M13 11v2"/><path d="M13 17v2"/>
            </svg>
            Comprar
          </button>
        </div>

        {/* 3. Dos columnas: Póster y Sinopsis */}
        <div className="movie-info-cols">
          <div className="movie-info-left">
            <div className="poster-wrapper">
              <img src={movie.poster} alt={`Póster oficial de la película ${movie.title}`} />
              {movie.featured && <div className="diagonal-tag" aria-label="Estreno">Estreno</div>}
            </div>
          </div>

          <div className="movie-info-right">
            <div className="synopsis-card">
              <h2>Sinopsis</h2>
              <p className="synopsis-text">{movie.synopsis}</p>
              
              <div className="info-meta-group">
                <h3>Idioma</h3>
                <div className="lang-tags">
                  <span className="lang-badge">{movie.language === 'ES' ? 'Doblada al Español' : 'Subtitulada'}</span>
                </div>
              </div>

              <div className="info-meta-group">
                <h3>Disponible</h3>
                <p className="formats-available">{movie.format.join(', ')}</p>
              </div>
            </div>
          </div>
        </div>

        {/* 4. Sección de funciones y horarios */}
        <div className="movie-showtimes-section" ref={showtimesRef}>
          <h2 className="section-title-fun">La función perfecta para ti.</h2>

          {/* Filtros de la función */}
          <div className="showtimes-filters-bar" aria-label="Filtro de funciones">
            <CustomDropdown
              label="Por ciudad"
              placeholder="Dónde estás"
              options={cities}
              value={filterCity}
              onChange={setFilterCity}
            />
            <div className="filter-divider-showtime" aria-hidden="true"></div>
            <CustomDropdown
              label="Por cine"
              placeholder="Elige tu Cine"
              options={cinemaNames}
              value={filterCinema}
              onChange={setFilterCinema}
            />
            <div className="filter-divider-showtime" aria-hidden="true"></div>
            <CustomDropdown
              label="Por fecha"
              placeholder="Elige un día"
              options={dates}
              value={filterDate}
              onChange={setFilterDate}
            />
          </div>

          {/* Acordeón de cines */}
          <div className="cinemas-accordion">
            {cinemas.map((cine, cIdx) => {
              const isExpanded = expandedCinema === cine.name;
              const panelId = `cinema-panel-${cIdx}`;
              const btnId = `cinema-btn-${cIdx}`;
              return (
                <div key={cine.name} className="accordion-item-cinema">
                  <button
                    type="button"
                    id={btnId}
                    className="accordion-header-cinema"
                    onClick={() => toggleCinema(cine.name)}
                    aria-expanded={isExpanded}
                    aria-controls={panelId}
                    style={{ width: '100%', background: 'none', border: 'none', textAlign: 'left', cursor: 'pointer', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
                  >
                    <h3 style={{ margin: 0 }}>{cine.name}</h3>
                    <span className="arrow" aria-hidden="true">{isExpanded ? '−' : '+'}</span>
                  </button>

                  {isExpanded && (
                    <div id={panelId} className="accordion-body-cinema" role="region" aria-labelledby={btnId}>
                      <div className="room-type-tag">
                        2D REGULAR DOBLADA
                      </div>
                      
                      <div className="showtime-buttons-grid">
                        {cine.showtimes.map((st, index) => (
                          <button
                            key={index}
                            type="button"
                            className={`showtime-btn ${!st.active ? 'disabled' : ''}`}
                            disabled={!st.active}
                            aria-label={st.active ? `Función de las ${st.time} en ${cine.name}` : `Función de las ${st.time} no disponible`}
                            onClick={() => navigate('/web/compra', {
                              state: {
                                movie,
                                cinema: cine.name,
                                date: filterDate || dates[0],
                                time: st.time,
                                roomType: '2D Regular',
                              }
                            })}
                          >
                            <span className="time-lbl">{st.time}</span>
                            {st.active && (
                              <span className="chair-icon" style={{display: 'flex'}} aria-hidden="true">
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                  <path d="M4 18v3"/><path d="M20 18v3"/><path d="M5 10V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v5"/><path d="M3 10a2 2 0 0 0-2 2v3c0 1.1.9 2 2 2h18a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z"/>
                                </svg>
                              </span>
                            )}
                          </button>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>

        </div>

      </div>

    </div>
  );
}
