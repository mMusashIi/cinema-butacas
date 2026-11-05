# INFORME DE ACCESIBILIDAD DIGITAL — APF3
## Proyecto Cinerama — Prototipo Web de Alta Fidelidad
**Curso:** Interacción Hombre-Máquina (UTP, Periodo 2026-II)  
**Equipo de Trabajo:** Herrera Farfan Marcelo Alessandro y equipo  
**Repositorio Oficial de Entrega:** [https://github.com/mMusashIi/cinema-butacas](https://github.com/mMusashIi/cinema-butacas)  
**Rama de Trabajo:** `accesibilidad-apf3`  
**Etiqueta Base (Entrega APF2):** `apf2-base`  

---

## 1. Contexto y Trazabilidad del Proyecto (Sección 1.3)

El presente documento consolida las intervenciones de accesibilidad digital realizadas sobre el prototipo interactivo de **Cinerama**, transitando desde el prototipo evaluado en el **Avance de Proyecto Final 2 (APF2)** hasta el prototipo accesible y validado del **APF3**, en conformidad estricta con las Pautas de Accesibilidad para el Contenido Web (**WCAG 2.1 nivel AA**).

### Historial de Commits de Accesibilidad
| Hash | Mensaje de Commit | Criterios WCAG |
|---|---|---|
| `011b0eb` | `a11y(layout): remonta AccessibilityWidget, agrega skip-link y titulos unicos por ruta` | 2.4.1, 2.4.2, 3.1.1 |
| `342ec89` | `a11y(compra): modal de cancelacion accesible, region viva para butacas y ayuda CVV` | 2.1.2, 2.4.3, 4.1.3 |
| `edea85b` | `a11y(home): carrusel con contenido real y tabs de cartelera funcionales` | 1.1.1, 2.1.1, 4.1.2 |
| `b37c8d1` | `a11y(multimedia): trailer accesible con gestion de foco, horarios descriptivos y tarjetas semanticas` | 2.1.1, 2.4.4, 2.5.3 |
| `2445bef` | `a11y(auth): atributos autocomplete completos y navegacion por flechas en pestanas` | 1.3.5, 2.1.1, 3.3.1 |
| `1f2d29e` | `a11y(estilos): contraste >=4.5:1 en tema oscuro y daltonismo, areas tactiles 44px y foco visible` | 1.4.3, 2.4.7, 2.5.5 |

---

## 2. Evaluación por Tipo de Discapacidad (Capítulo V)

### 2.1 Discapacidad Visual
- **Lector de Pantalla:** Se implementaron regiones vivas (`aria-live="polite"`) que notifican el cambio de butacas seleccionadas y el monto total en tiempo real sin recargar la página. Se asociaron etiquetas accesibles a botones de horario y controles iconográficos mediante `aria-label`.
- **Baja Visión y Daltonismo:** Widget flotante con 4 modos de visión (Predeterminado, Protanopia/Deuteranopia, Tritanopia, Monocromático) y escala tipográfica de 100% a 150%. Se incorporaron patrones geométricos y símbolos textuales (✓ y ✕) en las butacas para que el color nunca sea el único canal informativo (WCAG 1.4.1).
- **Contraste de Color:** Corrección de paleta CSS garantizando una relación de contraste ≥4.5:1 en textos estándar y ≥3:1 en componentes interactivos, tanto en modo oscuro como en modos de daltonismo.

### 2.2 Discapacidad Auditiva
- **Estado:** **No Aplica.**
- **Método de Verificación:** Auditoría estricta del árbol DOM y del código fuente en busca de elementos multimedia (`<audio>`, `<video>`, archivos `.mp3`, `.wav`). El prototipo público no emite sonidos de notificación, alertas sonoras ni reproduce pistas de audio automáticas. La reproducción del tráiler en `WebMovieDetails` se delegó a un reproductor modal accesible con subtítulos nativos y controles de volumen estándar.

### 2.3 Discapacidad Motora
- **Navegabilidad 100% por Teclado:** Se eliminaron los manejadores `onClick` en elementos no interactivos (`<div>`, `<h1>`, `<li>`), reemplazándolos por enlaces semánticos `<a>` y botones `<button>`.
- **Gestión y Atrapamiento de Foco (Focus Trap):** En los modales ("Cancelar Compra", "Compra Exitosa" y "Tráiler"), el foco queda confinado al diálogo abierto, evitando que la navegación por `Tab` escape al fondo. Al presionar `Escape`, el modal se cierra y el foco retorna al elemento activador.
- **Enlace de Salto:** Se incluyó un skip-link visible al recibir el foco (`Saltar al contenido principal`) que permite obviar la barra de navegación.
- **Tamaño de Objetivo Táctil:** Butacas y botones de ayuda con área activa ampliada a 44×44 px mediante pseudo-elementos `::after`.

### 2.4 Discapacidad Cognitiva
- **Claridad de Estados y Errores:** Formularios con validación en tiempo real y al enviar, mensajes de error descriptivos con íconos explicativos, y estados claros del botón de envío.
- **Consistencia y Memoria:** Indicador de 3 pasos ("Butacas", "Pago", "Confirmación") con retroalimentación clara del paso actual, diálogos de confirmación antes de perder selecciones y prevención de resultados vacíos.

---

## 3. Matriz de Barreras de Accesibilidad (Capítulo VIII)

| ID | Hallazgo Detectado | Tipo | Severidad | Justificación de Severidad | Criterio WCAG 2.1 | Solución Implementada |
|---|---|---|---|---|---|---|
| **BAR-01** | Modales sin gestión de foco, sin `aria-modal` y sin escape por teclado | Motora / Visual | **Alta** | Impide a usuarios de teclado y lectores de pantalla confirmar o cancelar compras sin desorientarse en el fondo. | 2.1.2 (No Keyboard Trap)<br>2.4.3 (Focus Order)<br>4.1.2 (Name, Role, Value) | Se implementó trampa de foco (`useRef` + `keydown`), atributos `role="dialog"`, `aria-modal="true"`, `aria-labelledby`, soporte de tecla `Escape` y retorno de foco al activador. |
| **BAR-02** | Contraste deficiente en tema oscuro: pasos `#666` (2.05:1), formatos (2.46:1) y login (3.15:1) | Visual | **Media** | Dificulta la lectura fluida a personas con baja agudeza visual en el tema predeterminado. | 1.4.3 (Contrast Minimum) | Se elevó el color de pasos a `#D1D5DB` (5.23:1), formatos y filtros a `--cinema-gold` (7.17:1) y pestañas a `#B3B3B3` (8.47:1). |
| **BAR-03** | Modos de daltonismo empeoraban contraste (blanco sobre `#4fa6d9` = 2.69:1; tritanopia = 3.37:1; hover butaca clara = 2.39:1) | Visual | **Media** | La paleta de soporte introducía combinaciones ilegibles en botones principales. | 1.4.3 (Contrast Minimum) | Se definió texto oscuro contrastante (`#0A1C2A` y `#1A000E`) en botones de visión alternativa (≥5.8:1) y se forzó texto blanco en hover de butacas seleccionadas. |
| **BAR-04** | Atributos ARIA inválidos en grillas de butacas y ausencia de anuncios al seleccionar | Visual (Lector) | **Media** | Usuarios con lector de pantalla no recibían confirmación auditiva del precio acumulado ni de la butaca elegida. | 4.1.2 (Name, Role, Value)<br>4.1.3 (Status Messages) | Se estructuró el mapa con `role="group"` y botones `aria-pressed`, añadiendo región viva `aria-live="polite"` con el total actualizado. |
| **BAR-05** | Idioma `lang="en"` en documento en español y títulos `<title>` idénticos entre rutas | Visual / Cognitiva | **Media** | Pronunciación errónea de sintetizadores de voz y desorientación cognitiva en navegación por pestañas. | 2.4.2 (Page Titled)<br>3.1.1 (Language of Page) | Se configuró `lang="es"`, meta descripción en español y se creó el hook `usePageTitle()` con formato `[Nombre] \| Cinerama`. |
| **BAR-06** | Elementos clave no operables por teclado (Logo `h1`, enlaces del pie "Cartelera" y "Cines") | Motora | **Media** | Bloqueaba el regreso al inicio o la navegación al pie a quienes navegan exclusivamente con teclado. | 2.1.1 (Keyboard) | Se reemplazaron por etiquetas semánticas `<a>` e interactivos `NavLink` con soporte de teclado nativo (`Tab` + `Enter`). |
| **BAR-07** | Ausencia de enlace "saltar al contenido", `<main>` duplicado o ausente, etiquetas `<nav>` vacías | Motora / Visual | **Baja** | Obligaba a tabular repetidamente por todo el menú superior en cada cambio de vista. | 2.4.1 (Bypass Blocks)<br>1.3.1 (Info and Relationships) | Se insertó `.skip-link` al inicio del DOM, un único `<main id="main-content">` en el layout y nombres accesibles en cada `<nav>`. |
| **BAR-08** | Formularios de Auth sin `autocomplete`, pestañas sin navegación por flechas y errores tardíos | Cognitiva / Motora | **Media** | Incrementa la carga cognitiva y dificulta el autocompletado de credenciales y gestores de contraseñas. | 1.3.5 (Identify Input Purpose)<br>2.1.1 (Keyboard)<br>3.3.1 (Error Identification) | Se agregaron `autoComplete` (`email`, `current-password`, `given-name`), navegación por flechas en pestañas (`ArrowLeft`/`ArrowRight`) y validación accesible en submit y blur. |
| **BAR-09** | Controles estáticos o mudos: carrusel estático, pestañas de cartelera sin filtro y botón tráiler inerte | Cognitiva | **Media** | Genera confusión en el usuario respecto a si la acción fue procesada o falló. | 1.1.1 (Non-text Content)<br>4.1.2 (Name, Role, Value) | Carrusel dinámico con `alt` descriptivo por diapositiva, pestañas con filtrado real de películas y botón tráiler con modal interactivo. |
| **BAR-10** | Botones de horario con rótulo escueto ("14:00") y tarjetas de cine con discrepancia de nombre | Visual (Lector) | **Baja** | Pérdida de contexto para usuarios no videntes al saltar entre botones de función. | 2.4.4 (Link Purpose in Context)<br>2.5.3 (Label in Name) | Se expandió el `aria-label` a formato descriptivo ("Función de las 18:00 para [Película] en [Cine]") y se alineó el nombre accesible con el texto visible. |
| **BAR-11** | Objetivos táctiles de butacas (36 px) y botón ayuda CVV (18 px) inferiores a 44 px | Motora | **Baja** | Dificulta la selección en pantallas táctiles a personas con temblores o baja precisión motriz. | 2.5.5 (Target Size)<br>2.5.8 (Target Size Minimum) | Se incorporaron pseudo-elementos transparentes `::after` que extienden el área de clic/toque a 44×44 px sin alterar el diseño visual. |

---

## 4. Tabla Comparativa Antes vs. Después con Métricas Reales (Capítulo X)

| Métrica / Parámetro de Evaluación | Estado Antes (APF2) | Estado Después (APF3) | Mejora Obtenida |
|---|---|---|---|
| **Idioma principal del documento** | `lang="en"` (inconsistente con texto) | `lang="es"` (declarado en `index.html`) | Sintetizadores de voz leen en pronunciación española nativa. |
| **Título del documento (`<title>`)** | Estático: "Cinerama — Cinema OS" | Dinámico y contextual en cada ruta | Permite identificar pestaña activa en historial y navegador. |
| **Contraste pasos de compra (Tema Oscuro)** | 2.05:1 (`#666666` sobre `#6a1a1b`) — **FALLA** | **5.23:1** (`#D1D5DB` sobre `#6a1a1b`) — **CUMPLE** | +155% de contraste, superando el umbral AA de 4.5:1. |
| **Contraste de formatos de cine y filtros** | 2.46:1 (`#E50914` sobre `#6a1a1b`) — **FALLA** | **7.17:1** (`#F5C518` sobre `#6a1a1b`) — **CUMPLE** | Supera el nivel más exigente AAA (7:1). |
| **Contraste pestañas de login (inactivas)** | 3.15:1 (`#666666` sobre `#161616`) — **FALLA** | **8.47:1** (`#B3B3B3` sobre `#161616`) — **CUMPLE** | Legibilidad inmediata de opciones inactivas. |
| **Contraste botones en modo Protanopia** | 2.69:1 (`#FFFFFF` sobre `#4FA6D9`) — **FALLA** | **6.52:1** (`#0A1C2A` sobre `#4FA6D9`) — **CUMPLE** | Texto oscuro de alto contraste en botones interactivos. |
| **Contraste botones en modo Tritanopia** | 3.37:1 (`#FFFFFF` sobre `#E8569A`) — **FALLA** | **5.79:1** (`#1A000E` sobre `#E8569A`) — **CUMPLE** | Cumple con solgura el estándar WCAG AA. |
| **Contraste butaca seleccionada en hover (Claro)**| 2.39:1 (texto oscurecía sobre rojo) — **FALLA**| **6.12:1** (`#FFFFFF !important`) — **CUMPLE** | Texto blanco asegurado en cualquier estado de hover. |
| **Gestión de foco en modales** | Foco libre al fondo; `Esc` inerte | Trap de foco automático y `Esc` habilitado | Cumplimiento total de WCAG 2.1.2 y 2.4.3. |
| **Retorno de foco tras cerrar diálogos** | Foco perdido en el `<body>` | Foco retorna exactamente al botón disparador | El usuario de teclado no pierde su ubicación en la página. |
| **Anuncio de butacas para lectores de pantalla** | Silencio total tras clic | Mensaje dinámico en región viva `aria-live` | Notificación audible instantánea de butacas y monto total. |
| **Navegabilidad por teclado en logotipo y footer**| Bloqueada (`div` / `li` sin tabulación) | 100% operable (`a` con foco nativo) | Acceso por `Tab` y activación con `Enter`. |
| **Enlace de salto (Skip-link)** | No existía en el prototipo | Visible al primer `Tab` ("Saltar al contenido") | Salto directo a `<main id="main-content">`. |
| **Autocompletado de formularios (Auth)** | Ningún campo con `autocomplete` | 5 campos con atributos estándar (1.3.5) | Soporte nativo para autocompletado y gestores de contraseñas. |
| **Navegación de pestañas (Tabs)** | Exclusiva con ratón | Flechas de dirección (`ArrowLeft`/`ArrowRight`) | Cumplimiento del patrón WAI-ARIA Tabs. |
| **Área táctil de butacas** | 36×36 px (en móvil 32×32 px) | **44×44 px** (con pseudo-elemento `::after`) | Cumple recomendación de guía de diseño y WCAG 2.5.5. |
| **Área táctil de botón ayuda CVV** | 18×18 px | **44×44 px** (con pseudo-elemento `::after`) | Facilidad de toque sin desalinear el layout del formulario. |
| **Indicador de foco visual (`:focus-visible`)** | Ausente en botones de compra y enlaces | Anillo azul eléctrico (`2px solid #3B82F6`) | Foco visible destacado en todos los elementos interactivos. |

---

## 5. Resumen Técnico del Prototipo Final (Capítulo XI)

### 11.1 Componentes de Accesibilidad Integrados
1. **Widget de Accesibilidad:** Botón flotante accesible (`a11y-fab`) con panel de diálogo que permite alternar tema claro/oscuro, escalar tamaño tipográfico (100% a 150%) y seleccionar 4 modos visuales adaptados a daltonismos.
2. **Sistema de Rutas con Anuncios Semánticos:** Hook `usePageTitle()` sincronizado con React Router para actualizar el `<title>` del documento en cada transición.
3. **Módulo de Compra Accesible (`WebBooking`):**
   - Grilla de selección de butacas con semántica de grupo y retroalimentación viva (`aria-live`).
   - Modales accesibles para confirmación de cancelación y compra exitosa con atrapamiento de foco.
   - Formulario de pago seguro con validación en tiempo real, campos con `autoComplete` y mensaje de ayuda CVV accesible.

### 11.2 Metodología de Validación Empleada
- **Inspección de Teclado:** Recorrido integral de flujos de compra empleando únicamente teclado (`Tab`, `Shift+Tab`, `Enter`, `Space`, `Escape`, `ArrowLeft`, `ArrowRight`).
- **Verificación de Contraste:** Medición de luminancia relativa y ratios de contraste sobre fondo `#6a1a1b`, superficies `#161616` y paletas alternativas Okabe-Ito.
- **Auditoría Estática:** Inspección de roles ARIA contra especificación W3C WAI-ARIA 1.2.
- **Compilación Limpia:** Ejecución exitosa de `npm run build` confirmando ausencia de errores de sintaxis o empaquetado.

### 11.3 Declaración de Limitaciones
- No se contó con sesiones de testeo presencial con usuarios que empleen lectores de pantalla de escritorio propietarios (JAWS) ni hardware braille.
- Los contrastes sobre imágenes dinámicas de fondo (pósters y banners) se controlaron mediante capas de oscurecimiento (`background-color: rgba(...)`), recomendándose mantener pósters de alta legibilidad en producción.

### 11.4 Enlaces de Entrega
- **Repositorio de Código Fuente:** [https://github.com/mMusashIi/cinema-butacas](https://github.com/mMusashIi/cinema-butacas)
- **Rama:** `accesibilidad-apf3`
- **Etiqueta Base APF2:** `apf2-base`
- **Despliegue Web:** `[POR CONFIRMAR TRAS PUSH Y CONFIGURACIÓN DE GITHUB PAGES]`
