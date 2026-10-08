import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { CinemaProvider } from './context/CinemaContext';
import WebLayout from './components/layout/WebLayout';
import WebHome from './pages/WebHome';
import WebAuth from './pages/WebAuth';
import WebCinemas from './pages/WebCinemas';
import WebMovies from './pages/WebMovies';
import WebBooking from './pages/WebBooking';
import WebMovieDetails from './pages/WebMovieDetails';
import WebCinemaDetails from './pages/WebCinemaDetails';

export default function App() {
  return (
    <CinemaProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Navigate to="/WebHome" replace />} />
          <Route element={<WebLayout />}>
            <Route path="/WebHome"              element={<WebHome />} />
            <Route path="/web/peliculas"        element={<WebMovies />} />
            <Route path="/web/pelicula/:id"     element={<WebMovieDetails />} />
            <Route path="/web/cines"            element={<WebCinemas />} />
            <Route path="/web/cine/:id"         element={<WebCinemaDetails />} />
            <Route path="/web/login"            element={<WebAuth />} />
            <Route path="/web/compra"           element={<WebBooking />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </CinemaProvider>
  );
}
