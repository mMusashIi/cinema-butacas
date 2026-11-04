# Resumen para el Informe APF3 — Prototipo Accesible (Capítulo XI)

Este material está estructurado para ser integrado directamente en el documento formal del entregable APF3 de Interacción Hombre-Máquina.

---

## 1.3 Prototipo APF2 (El "ANTES")

- **Descripción de la versión base:** Prototipo funcional de la plataforma web de CINERAMA desarrollado con React 19 y Vite para la visualización de cartelera, consulta de cines y reserva de boletos de cine en Ica.
- **Pantallas principales:**
  - `WebHome`: Inicio con carrusel de novedades, barra de filtros rápidos y cartelera destacada.
  - `WebMovies`: Catálogo con menú lateral de filtros (ciudad, cine, día, género, idioma, formato).
  - `WebMovieDetails`: Ficha de película con sinopsis, especificaciones técnicas y acordeón de horarios por cine.
  - `WebCinemas`: Directorio de complejos cinematográficos con tarjetas fotográficas.
  - `WebCinemaDetails`: Vista de cine individual con pestañas de horarios y películas.
  - `WebAuth`: Pestañas para inicio de sesión y registro de clientes.
  - `WebBooking`: Proceso guiado de compra (selección de butacas en mapa de sala, formulario de pago con tarjeta y modal de confirmación).
- **Limitaciones de accesibilidad detectadas en la versión base:**
  - Documento etiquetado en inglés (`lang="en"`).
  - Navegación imposible mediante teclado: múltiples elementos interactivos maquetados con etiquetas genéricas `<div>` y `<p>`.
  - Ausencia de estados ARIA (`aria-expanded`, `aria-selected`, `aria-live`).
  - Indicadores de foco visual suprimidos con `outline: none`.
  - Modales sin semántica de diálogo ni redirección del foco del cursor.
- **Evidencia visual (Capturas del "ANTES"):**
  - Ubicación: `docs/accesibilidad/capturas/antes/` `[POR CONFIRMAR - captura de pantalla tomada por el alumno desde la etiqueta apf2-base]`

---

## Capítulo XI. Prototipo Accesible

### 11.1 Versión actualizada

El prototipo fue actualizado para alinearse con los estándares de accesibilidad **WCAG 2.1 nivel AA**. La versión accesible garantiza que personas con diferentes capacidades (usuarios de lectores de pantalla, personas con discapacidad motriz que navegan exclusivamente con teclado, y personas con baja visión) puedan consultar la cartelera y completar el flujo de compra de boletos de extremo a extremo sin barreras.

### 11.2 Pantallas y evidencias visuales

Las pantallas actualizadas incorporan indicadores de foco de alto contraste (`:focus-visible` en azul eléctrico), botones y enlaces semánticos con tamaño táctil adecuado, y estados visuales y textuales explícitos:
- **Pantalla de Inicio (`/WebHome`):** Carrusel con controles rotulados, botones de cartelera con texto accesible.
- **Pantalla de Películas (`/web/peliculas`):** Acordeones y opciones como botones con estado `aria-expanded` y píldoras eliminables.
- **Detalle de Película (`/web/pelicula/:id`):** Acordeones de cines tabulables y selección de horario descriptiva.
- **Flujo de Compra (`/web/compra`):** Mapa de butacas accesible por teclado con leyenda no dependiente exclusivamente del color, botón "Continuar al pago ⟩" con rol explícito y modal con foco atrapado en "Volver al Inicio".
- **Autenticación (`/web/login`):** Pestañas con roles ARIA y autocompletado en campos de entrada.

*Nota:* Las capturas de escritorio y móvil de la versión actualizada se ubican en `docs/accesibilidad/capturas/despues/`.

### 11.3 Resumen de cambios implementados por tipo de accesibilidad

1. **Accesibilidad Motora (Navegación por teclado):**
   - Sustitución de `div`, `p`, `span` y `li` interactivos por elementos semánticos `<a>` y `<button>`.
   - Soporte total de teclado (`Tab`, `Enter`, `Espacio`, `Escape`) en el componente `CustomDropdown`.
   - Adición de contornos de foco visibles (`outline: 2px solid var(--electric-blue)`) en todos los inputs y botones interactivos.
2. **Accesibilidad Visual (Lectores de pantalla y contraste):**
   - Corrección del idioma a `lang="es"` en `index.html`.
   - Inclusión de nombres accesibles (`aria-label`) en iconos de redes sociales, carrusel, enlaces de cartelera y botones de horario.
   - Textos alternativos descriptivos (`alt="Póster de..."`, `alt="Fotografía del cine..."`).
   - Modal de compra exitosa implementado con `role="dialog"`, `aria-modal="true"` y gestión programática de foco con `useRef`.
   - Anuncio de cambios de contenido dinámico (`aria-live="polite"`) en resultados de filtrado y resumen de precios.
3. **Accesibilidad Cognitiva:**
   - Adición de atributos `autoComplete` en formularios de login y registro.
   - Claridad en el botón de navegación del flujo de compra ("Continuar al pago ⟩" en lugar del genérico "Continuar ⟩").
   - Mensajes de error claros, contextualizados y no basados únicamente en color.
4. **Accesibilidad Auditiva:**
   - Verificación de ausencia de autoplay sonoro en tráilers y confirmaciones con refuerzo visual constante.

### 11.4 Enlace del prototipo

- **URL desplegada:** `[POR CONFIRMAR]` *(Si se publica en GitHub Pages o Vercel, agregar el enlace aquí; de lo contrario, indicar ejecución local mediante `npm run dev`)*.

---

## Anexo: Historial de commits de accesibilidad

```text
bc2565f a11y(auth): tabs con role=tab, autocomplete en inputs y foco visible — WCAG 4.1.2 / 1.3.5 / 2.4.7
1ed11bf a11y(dropdown): soporte de teclado, role=listbox y role=option en CustomDropdown — WCAG 2.1.1 / 4.1.2
b6fa9f7 a11y(compra): modal de exito con role=dialog y gestion de foco, boton de paso descriptivo — WCAG 4.1.2 / 2.4.3 / 2.4.6
314366a a11y(cines): tarjetas de cine como enlaces accesibles con teclado y alt descriptivo — WCAG 2.1.1 / 4.1.2 / 1.1.1
50e22af a11y(cartelera): tarjetas de pelicula como enlaces accesibles con teclado y tabs semanticas — WCAG 2.1.1 / 4.1.2
0f2b9dd a11y(detalle): acordeón de cines como button con aria-expanded, alt descriptivo y labels en botones — WCAG 2.1.1 / 4.1.2 / 1.1.1
9f636d4 a11y(peliculas): acordeones/opciones como button con aria-expanded, pills como button, tarjetas como <a>, aria-live en resultados — WCAG 2.1.1 / 4.1.2 / 4.1.3
25bb34e a11y(nav): logo/links/footer como <a>, iconos sociales con aria-label, nav con aria-label — WCAG 2.1.1 / 4.1.2 / 1.1.1
0f17738 a11y(html): corrige lang a es — WCAG 3.1.1
```
