# Registro de Cambios: Antes y Después — Accesibilidad (APF3)

Este documento detalla cada mejora implementada en el prototipo accesible de **CINERAMA**, siguiendo la estructura definida en la guía de auditoría.

---

### Mejora 1 — Declaración de idioma principal del documento
- **Tipo:** Visual (lector de pantalla)
- **ANTES:** El elemento raíz en `index.html` declaraba `<html lang="en">`, a pesar de que toda la plataforma está redactada en español.
- **BARRERA:** Los lectores de pantalla sintetizaban la voz utilizando el diccionario y reglas fonéticas del inglés, resultando en una pronunciación ininteligible para usuarios con discapacidad visual.
- **SOLUCIÓN:** Se corrigió a `<html lang="es">`.
- **DESPUÉS:** Los motores de síntesis de voz cargan correctamente el paquete fonético en español.
- **Archivos:** [`index.html`](file:///c:/Users/maxeh/Desktop/uni/cineramaCliente/index.html)
- **Commit:** `0f17738` — `a11y(html): corrige lang a es — WCAG 3.1.1`

---

### Mejora 2 — Accesibilidad por teclado en navegación principal y pie de página
- **Tipo:** Motora / Visual
- **ANTES:** El logotipo "CINERAMA" y los enlaces del pie de página estaban maquetados como `<div>` y `<li>` con eventos `onClick`. Los iconos de redes sociales eran simples elementos `<svg>`.
- **BARRERA:** No era posible tabular hacia el logo para volver al inicio ni acceder a los enlaces informativos del footer sin un puntero físico (mouse).
- **SOLUCIÓN:** 
  - Se convirtió el logo en `<a className="web-brand" href="/WebHome" aria-label="Cinerama — ir al inicio">`.
  - Se añadieron enlaces `<a>` accesibles a las opciones del footer.
  - Se envolvieron los iconos de redes sociales en `<a target="_blank" rel="noopener noreferrer">` con etiquetas `aria-label="Síguenos en [Red]"`.
  - Se rotuló el contenedor `<nav>` con `aria-label="Navegación principal"`.
- **DESPUÉS:** Todos los controles son navegables con la tecla `Tab` y anuncian su destino claramente.
- **Archivos:** [`src/components/layout/WebLayout.jsx`](file:///c:/Users/maxeh/Desktop/uni/cineramaCliente/src/components/layout/WebLayout.jsx), [`src/components/layout/WebLayout.css`](file:///c:/Users/maxeh/Desktop/uni/cineramaCliente/src/components/layout/WebLayout.css)
- **Commit:** `25bb34e` — `a11y(nav): logo/links/footer como <a>, iconos sociales con aria-label, nav con aria-label — WCAG 2.1.1 / 4.1.2 / 1.1.1`

---

### Mejora 3 — Operabilidad de filtros de películas y anuncios de cambios dinámicos
- **Tipo:** Motora / Visual
- **ANTES:** En `/web/peliculas`, los títulos de acordeones eran `<div onClick>` y las opciones de filtrado eran párrafos `<p onClick>`. Las píldoras de filtros activos eran `<span onClick>`.
- **BARRERA:** Un usuario que utilizaba teclado no podía expandir ningún acordeón ni activar ningún filtro, imposibilitando la búsqueda por ciudad, cine, género o formato.
- **SOLUCIÓN:** 
  - Títulos de acordeón convertidos a `<button type="button">` con atributo `aria-expanded`.
  - Opciones de filtro sustituidas por `<button type="button">` con `aria-pressed`.
  - Píldoras de filtro activo transformadas en botones con `aria-label="Eliminar filtro: [valor]"`.
  - Encabezado de conteo de resultados enriquecido con `aria-live="polite"` y `aria-atomic="true"`.
- **DESPUÉS:** Se puede filtrar íntegramente usando solo el teclado, y el lector de pantalla anuncia de inmediato la cantidad de películas resultantes.
- **Archivos:** [`src/pages/WebMovies.jsx`](file:///c:/Users/maxeh/Desktop/uni/cineramaCliente/src/pages/WebMovies.jsx), [`src/pages/WebPages.css`](file:///c:/Users/maxeh/Desktop/uni/cineramaCliente/src/pages/WebPages.css)
- **Commit:** `9f636d4` — `a11y(peliculas): acordeones/opciones como button con aria-expanded, pills como button, tarjetas como <a>, aria-live en resultados — WCAG 2.1.1 / 4.1.2 / 4.1.3`

---

### Mejora 4 — Acordeón de cines y selección de horarios en detalle de película
- **Tipo:** Motora / Visual
- **ANTES:** En `/web/pelicula/:id`, el encabezado de cada cine era un `<div>` no tabulable. Los botones de horarios no tenían una descripción textual que indicara su estado de disponibilidad.
- **BARRERA:** El usuario no podía abrir las salas de su preferencia para seleccionar una función mediante teclado.
- **SOLUCIÓN:** 
  - Cabecera del acordeón convertida en `<button type="button" aria-expanded={isExpanded}>`.
  - Botones de horarios con `aria-label="Función disponible/no disponible a las [Hora]"`.
  - Texto alternativo descriptivo en el póster principal.
- **DESPUÉS:** Navegación fluida por teclado para explorar sedes y seleccionar horarios.
- **Archivos:** [`src/pages/WebMovieDetails.jsx`](file:///c:/Users/maxeh/Desktop/uni/cineramaCliente/src/pages/WebMovieDetails.jsx), [`src/pages/WebPages.css`](file:///c:/Users/maxeh/Desktop/uni/cineramaCliente/src/pages/WebPages.css)
- **Commit:** `0f2b9dd` — `a11y(detalle): acordeón de cines como button con aria-expanded, alt descriptivo y labels en botones — WCAG 2.1.1 / 4.1.2 / 1.1.1`

---

### Mejora 5 — Enlaces semánticos en tarjetas de cartelera y carrusel accesible
- **Tipo:** Motora / Visual
- **ANTES:** En la página de Inicio y en Detalle de Cine, las tarjetas de películas eran `<div onClick>` y el carrusel usaba caracteres planos `〈` y `〉` sin etiquetas, con indicadores tipo `<span onClick>`.
- **BARRERA:** Imposibilidad de seleccionar películas con teclado y carrusel confuso para lectores de pantalla.
- **SOLUCIÓN:** 
  - Tarjetas de películas convertidas en enlaces semánticos `<a>` con `aria-label="Ver detalle de [Título]"`.
  - Controles del carrusel rotulados con `aria-label="Diapositiva anterior"` y `aria-label="Siguiente diapositiva"`.
  - Indicadores de diapositiva (`dots`) transformados en botones tabulables con rol y estado `aria-selected`.
- **DESPUÉS:** Las películas son fácilmente alcanzables mediante `Tab` y `Enter` con contorno de foco visual visible.
- **Archivos:** [`src/pages/WebHome.jsx`](file:///c:/Users/maxeh/Desktop/uni/cineramaCliente/src/pages/WebHome.jsx), [`src/pages/WebHome.css`](file:///c:/Users/maxeh/Desktop/uni/cineramaCliente/src/pages/WebHome.css), [`src/pages/WebCinemaDetails.jsx`](file:///c:/Users/maxeh/Desktop/uni/cineramaCliente/src/pages/WebCinemaDetails.jsx)
- **Commit:** `50e22af` — `a11y(cartelera): tarjetas de pelicula como enlaces accesibles con teclado y tabs semanticas — WCAG 2.1.1 / 4.1.2`

---

### Mejora 6 — Tarjetas de sedes y complejos de cine
- **Tipo:** Motora / Visual
- **ANTES:** En `/web/cines`, las tarjetas de cada sede utilizaban eventos `onClick` sobre divisiones no semánticas.
- **BARRERA:** Bloqueo de acceso a la cartelera de cada cine para usuarios que no usan ratón.
- **SOLUCIÓN:** Envoltura en elementos `<a>` con `aria-label="Ver cartelera de [Nombre], ubicado en [Dirección]"` y texto alternativo descriptivo en fotografías.
- **DESPUÉS:** Enlace semántico claro y alcanzable con teclado.
- **Archivos:** [`src/pages/WebCinemas.jsx`](file:///c:/Users/maxeh/Desktop/uni/cineramaCliente/src/pages/WebCinemas.jsx), [`src/pages/WebPages.css`](file:///c:/Users/maxeh/Desktop/uni/cineramaCliente/src/pages/WebPages.css)
- **Commit:** `314366a` — `a11y(cines): tarjetas de cine como enlaces accesibles con teclado y alt descriptivo — WCAG 2.1.1 / 4.1.2 / 1.1.1`

---

### Mejora 7 — Modal de confirmación accesible y navegación entre pasos de compra
- **Tipo:** Visual / Motora / Cognitiva
- **ANTES:** Tras efectuar el pago en `/web/compra`, el modal emergente de "¡Compra Exitosa!" era un simple `<div>` superpuesto sin rol de diálogo. El foco permanecía en el botón del formulario de pago previo. El botón de continuar en butacas decía únicamente "Continuar ⟩".
- **BARRERA:** Los lectores de pantalla no anunciaban la aparición del modal; el usuario con discapacidad visual quedaba en la incertidumbre de si la transacción se había completado.
- **SOLUCIÓN:** 
  - Asignación de `role="dialog"`, `aria-modal="true"`, `aria-labelledby="modal-success-title"` y `aria-describedby="modal-success-desc"`.
  - Enfoque programático automático sobre el botón de acción principal ("Volver al Inicio") mediante `useRef` y `useEffect`.
  - Renombrado del botón de transición a "Continuar al pago ⟩" con `aria-label="Continuar al paso de pago"`.
  - Rotulación del `<aside>` de resumen de compra con `aria-label="Resumen de la compra"`.
- **DESPUÉS:** Al culminar el pago, el lector de pantalla notifica inmediatamente el éxito de la reserva y sitúa el cursor en el control de retorno.
- **Archivos:** [`src/pages/WebBooking.jsx`](file:///c:/Users/maxeh/Desktop/uni/cineramaCliente/src/pages/WebBooking.jsx)
- **Commit:** `b6fa9f7` — `a11y(compra): modal de exito con role=dialog y gestion de foco, boton de paso descriptivo — WCAG 4.1.2 / 2.4.3 / 2.4.6`

---

### Mejora 8 — Accesibilidad y navegación por teclado en desplegables (`CustomDropdown`)
- **Tipo:** Motora / Visual
- **ANTES:** El componente personalizado de desplegable no permitía navegar ni seleccionar elementos usando el teclado una vez desplegado.
- **BARRERA:** En la barra de búsqueda rápida del Home y filtros de detalle, usuarios de teclado quedaban atrapados o no podían escoger ciudad ni fecha.
- **SOLUCIÓN:** Se dotó de `role="listbox"`, `role="option"`, `tabIndex={0}`, y controladores de teclado (`Enter`, `Espacio`, `Escape`).
- **DESPUÉS:** Desplegables totalmente operables sin mouse.
- **Archivos:** [`src/components/CustomDropdown.jsx`](file:///c:/Users/maxeh/Desktop/uni/cineramaCliente/src/components/CustomDropdown.jsx), [`src/pages/WebPages.css`](file:///c:/Users/maxeh/Desktop/uni/cineramaCliente/src/pages/WebPages.css)
- **Commit:** `1ed11bf` — `a11y(dropdown): soporte de teclado, role=listbox y role=option en CustomDropdown — WCAG 2.1.1 / 4.1.2`

---

### Mejora 9 — Formulario de autenticación accesible y foco visible global
- **Tipo:** Visual / Cognitiva / Motora
- **ANTES:** Las pestañas de Login y Registro no anunciaban su estado activo a tecnologías de apoyo; los campos carecían de sugerencias de autocompletado y los formularios anulaban el contorno de foco con `outline: none`.
- **BARRERA:** Dificultad para saber qué formulario estaba visible y pérdida de orientación visual del elemento enfocado.
- **SOLUCIÓN:** 
  - Implementación de roles ARIA de pestañas (`tablist`, `tab`, `aria-selected`).
  - Adición de atributos `autoComplete` estándar (`email`, `current-password`, `new-password`, `given-name`, `family-name`).
  - Regla CSS global para `:focus-visible` con contorno de alto contraste (azul eléctrico #3B82F6, 2px) en inputs y botones.
- **DESPUÉS:** Claridad total al enfocar campos e interacción simplificada para recordar credenciales.
- **Archivos:** [`src/pages/WebAuth.jsx`](file:///c:/Users/maxeh/Desktop/uni/cineramaCliente/src/pages/WebAuth.jsx), [`src/pages/WebPages.css`](file:///c:/Users/maxeh/Desktop/uni/cineramaCliente/src/pages/WebPages.css)
- **Commit:** `bc2565f` — `a11y(auth): tabs con role=tab, autocomplete en inputs y foco visible — WCAG 4.1.2 / 1.3.5 / 2.4.7`
