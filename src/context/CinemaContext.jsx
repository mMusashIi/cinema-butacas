import { createContext, useContext, useState, useMemo } from 'react';
import { branches, movies, movieCinema, screenings } from '../data/store';

const CinemaContext = createContext();

export function CinemaProvider({ children }) {
  const [allScreenings] = useState(screenings);

  // Todas las películas activas (visión pública: sin filtro por sede)
  const enabledMovies = useMemo(() => movies, []);

  // Todas las sedes disponibles
  const activeBranches = useMemo(() => branches, []);

  // Todas las funciones activas
  const activeScreenings = useMemo(() => allScreenings, [allScreenings]);

  return (
    <CinemaContext.Provider value={{
      branches: activeBranches,
      enabledMovies,
      activeScreenings,
    }}>
      {children}
    </CinemaContext.Provider>
  );
}

export function useCinema() {
  return useContext(CinemaContext);
}
