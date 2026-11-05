import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useCinema } from '../context/CinemaContext';
import CustomDropdown from '../components/CustomDropdown';
import { usePageTitle } from '../hooks/usePageTitle';
import './WebPages.css';

export default function WebCinemaDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { branches, enabledMovies } = useCinema();

  const cinema = branches.find(b => b.id === parseInt(id)) || branches[0];
  usePageTitle(cinema ? `${cinema.name}` : 'Cine');

  const [activeTab, setActiveTab] = useState('Horarios');
  const [selectedDate, setSelectedDate] = useState('Hoy Miércoles 20');

  const dates = ['Hoy Lunes', 'Martes 19', 'Miércoles 20'];

  // Demo showtimes mapped for movies
  const getShowtimes = () => {
    return [
      { time: '17:10', active: false },
      { time: '19:30', active: true },
      { time: '21:50', active: true }
    ];
  };

  const handleTabKeyDown = (e) => {
    if (e.key === 'ArrowRight' || e.key === 'ArrowLeft') {
      e.preventDefault();
      setActiveTab(prev => prev === 'Horarios' ? 'Películas' : 'Horarios');
    }
  };

  return (
    <div className="cinema-details-page">
      <div className="cinema-details-container">

        <div className="cinema-details-header">
          <h1 className="cinema-title-blue">{cinema.name}</h1>
          <div className="cinema-header-tabs" role="tablist" aria-label="Secciones del cine">
            <button
              role="tab"
              id="tab-cinema-horarios"
              aria-selected={activeTab === 'Horarios'}
              aria-controls="panel-cinema-horarios"
              tabIndex={activeTab === 'Horarios' ? 0 : -1}
              className={`cinema-tab-btn ${activeTab === 'Horarios' ? 'active' : ''}`}
              onClick={() => setActiveTab('Horarios')}
              onKeyDown={handleTabKeyDown}
            >
              Horarios
            </button>
            <button
              role="tab"
              id="tab-cinema-peliculas"
              aria-selected={activeTab === 'Películas'}
              aria-controls="panel-cinema-peliculas"
              tabIndex={activeTab === 'Películas' ? 0 : -1}
              className={`cinema-tab-btn ${activeTab === 'Películas' ? 'active' : ''}`}
              onClick={() => setActiveTab('Películas')}
              onKeyDown={handleTabKeyDown}
            >
              Películas
            </button>
          </div>
        </div>

        {activeTab === 'Horarios' && (
          <div
            id="panel-cinema-horarios"
            role="tabpanel"
            aria-labelledby="tab-cinema-horarios"
            className="cinema-horarios-view"
          >
            <div className="cinema-horarios-controls">
              <CustomDropdown
                label="Fecha"
                placeholder="Elige un día"
                options={dates}
                value={selectedDate}
                onChange={setSelectedDate}
              />
            </div>

            <div className="cinema-movies-list">
              {enabledMovies.slice(0, 3).map(movie => (
                <div key={movie.id} className="cinema-movie-list-item">

                  <div className="cinema-movie-poster-col">
                    <div className="poster-wrapper">
                      <img src={movie.poster} alt={`Póster de ${movie.title}`} />
                      {movie.featured && <div className="diagonal-tag" aria-label="Estreno">Estreno</div>}
                    </div>
                  </div>

                  <div className="cinema-movie-info-col">
                    <div className="movie-card-info">
                      <h4>{movie.title}</h4>
                      <p>{movie.genre}, {Math.floor(movie.duration / 60)}h {movie.duration % 60}min, {movie.rating}.</p>
                    </div>

                    <div className="room-type-tag">
                      2D REGULAR DOBLADA
                    </div>

                    <div className="showtime-buttons-grid cinema-showtimes">
                      {getShowtimes().map((st, index) => (
                        <button
                          key={index}
                          type="button"
                          className={`showtime-btn ${!st.active ? 'disabled' : ''}`}
                          disabled={!st.active}
                          aria-label={st.active ? `Función de las ${st.time} para ${movie.title}` : `Función de las ${st.time} no disponible`}
                          onClick={() => navigate('/web/compra', {
                            state: {
                              movie,
                              cinema: cinema.name,
                              date: selectedDate,
                              time: st.time,
                              roomType: '2D Regular',
                            }
                          })}
                        >
                          <span className="time-lbl">{st.time}</span>
                          {st.active && (
                            <span className="chair-icon" style={{ display: 'flex' }} aria-hidden="true">
                              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M4 18v3" /><path d="M20 18v3" /><path d="M5 10V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v5" /><path d="M3 10a2 2 0 0 0-2 2v3c0 1.1.9 2 2 2h18a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z" />
                              </svg>
                            </span>
                          )}
                        </button>
                      ))}
                    </div>
                  </div>

                </div>
              ))}
            </div>
          </div>
        )}

        {activeTab === 'Películas' && (
          <div
            id="panel-cinema-peliculas"
            role="tabpanel"
            aria-labelledby="tab-cinema-peliculas"
            className="cinema-peliculas-view"
          >
            <div className="movies-grid">
              {enabledMovies.map(movie => (
                <a
                  key={movie.id}
                  href={`/web/pelicula/${movie.id}`}
                  className="movie-poster"
                  onClick={e => { e.preventDefault(); navigate(`/web/pelicula/${movie.id}`); }}
                  style={{ position: 'relative' }}
                  aria-label={`Ver detalles de ${movie.title}`}
                >
                  <img src={movie.poster} alt={`Póster de ${movie.title}`} />
                  {movie.featured && <div className="diagonal-tag" aria-label="Estreno">Estreno</div>}
                  <div className="movie-hover-overlay" aria-hidden="true">
                    <span>Ver detalle &#x27E9;</span>
                  </div>
                </a>
              ))}
            </div>
          </div>
        )}

      </div>
    </div>
  );
}
