import { useEffect } from 'react';

/**
 * Actualiza el <title> del documento con el formato "Página | Cinerama".
 * Cumple WCAG 2.4.2: cada página tiene un título descriptivo y único.
 *
 * @param {string} title - Título específico de la ruta (en español).
 */
export function usePageTitle(title) {
  useEffect(() => {
    document.title = title ? `${title} | Cinerama` : 'Cinerama';
  }, [title]);
}

