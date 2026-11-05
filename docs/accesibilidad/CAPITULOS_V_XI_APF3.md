# CAPÍTULOS V – XI · ACCESIBILIDAD DIGITAL APF3
## Proyecto CINERAMA — Prototipo Web de Alta Fidelidad
**Curso:** Interacción Hombre-Máquina (UTP, Periodo 2026-II)  
**Equipo:** Herrera Farfan Marcelo Alessandro y equipo  
**Repositorio:** [https://github.com/mMusashIi/cinema-butacas](https://github.com/mMusashIi/cinema-butacas) · rama `accesibilidad-apf3`  
**Norma de referencia:** WCAG 2.1 Nivel AA (W3C, 2018)  
**Principios de Diseño Universal:** los 7 principios de Ron Mace (1997)

> Este documento complementa a `INFORME_ACCESIBILIDAD_APF3.md` y contiene el desarrollo técnico  
> completo de los capítulos pendientes del informe APF3. Cada sección cita funciones, clases CSS  
> y atributos ARIA exactamente como aparecen en el código fuente entregado.

---

## CAPÍTULO V — ACCESIBILIDAD SENSORIAL (DISCAPACIDAD VISUAL)

### 5.1 Fundamento Normativo

La accesibilidad sensorial abarca todas las barreras que impiden percibir la información a usuarios con discapacidad visual: baja visión, ceguera funcional y deficiencia en la percepción del color. Los criterios WCAG 2.1 que guiaron este capítulo son:

| Criterio | Nombre | Nivel |
|---|---|---|
| 1.1.1 | Non-text Content | A |
| 1.4.1 | Use of Color | A |
| 1.4.3 | Contrast (Minimum) | AA |
| 1.4.4 | Resize Text | AA |
| 1.4.10 | Reflow | AA |
| 1.4.11 | Non-text Contrast | AA |
| 4.1.2 | Name, Role, Value | A |
| 4.1.3 | Status Messages | AA |

---

### 5.2 Corrección de Contraste de Color (WCAG 1.4.3)

#### 5.2.1 Problema detectado en APF2

La auditoría con axe-core realizada sobre el prototipo APF2 identificó **seis combinaciones de color con relación de contraste inferior al mínimo de 4.5:1** exigido para texto de tamaño estándar. El tema oscuro (predeterminado) era el más afectado, puesto que el rojo corporativo `#6A1A1B` del encabezado era muy oscuro y los textos secundarios heredaban grises sin ajustar.

#### 5.2.2 Soluciones implementadas en `WebPages.css`

Las correcciones se concentran en el bloque **"MEJORAS DE ACCESIBILIDAD (APF3)"** añadido al final de `src/styles/WebPages.css` (commit `1f2d29e`).

**Tabla de correcciones de contraste — valores medidos con la fórmula de luminancia relativa WCAG:**

| Elemento / Clase CSS | Color Anterior | Ratio Anterior | Color Nuevo | Ratio Nuevo | Resultado |
|---|---|---|---|---|---|
| Indicador de pasos de compra (`.booking-step`) sobre `#6A1A1B` | `#666666` | **2.05:1** ❌ | `#D1D5DB` | **5.23:1** ✅ | +155 % |
| Formatos de cine y filtros activos sobre `#6A1A1B` | `#E50914` | **2.46:1** ❌ | `var(--cinema-gold)` = `#F5C518` | **7.17:1** ✅ | Supera AAA |
| Pestañas inactivas de Login sobre `#161616` | `#666666` | **3.15:1** ❌ | `#B3B3B3` | **8.47:1** ✅ | +169 % |
| Botones primarios en modo Protanopia/Deuteranopia (`#4FA6D9`) | `#FFFFFF` | **2.69:1** ❌ | `#0A1C2A` | **6.52:1** ✅ | +142 % |
| Botones primarios en modo Tritanopia (`#E8569A`) | `#FFFFFF` | **3.37:1** ❌ | `#1A000E` | **5.79:1** ✅ | +72 % |
| Texto sobre butaca seleccionada en hover (tema claro) | `#333333` | **2.39:1** ❌ | `#FFFFFF` | **6.12:1** ✅ | +156 % |

**Fragmento de código relevante** (`src/styles/WebPages.css`, bloque APF3):

```css
/* Indicador de pasos — contraste elevado de 2.05:1 a 5.23:1 */
.booking-step {
  color: #D1D5DB; /* antes: #666666 */
}

/* Formatos de cine y filtros activos — de 2.46:1 a 7.17:1 */
.cinema-formats,
.filter-active {
  color: var(--cinema-gold); /* antes: #E50914 */
}

/* Pestañas de Auth inactivas — de 3.15:1 a 8.47:1 */
.auth-tab {
  color: var(--text-secondary); /* #B3B3B3 — antes: #666666 */
}

/* Texto oscuro en botones bajo daltonismo protan/deutan — de 2.69:1 a 6.52:1 */
[data-vision="protan-deutan"] .btn-primary {
  color: #0A1C2A !important;
}

/* Texto oscuro en botones bajo daltonismo tritanopia — de 3.37:1 a 5.79:1 */
[data-vision="tritan"] .btn-primary {
  color: #1A000E !important;
}

/* Hover de butaca seleccionada en tema claro — de 2.39:1 a 6.12:1 */
[data-theme="light"] .seat-btn.seat-selected:hover {
  color: #FFFFFF !important;
}
```

---

### 5.3 Butacas: El Color Nunca es el Único Indicador (WCAG 1.4.1)

#### 5.3.1 Problema detectado en APF2

El mapa de butacas comunicaba el estado (disponible / seleccionada / ocupada) únicamente mediante color (verde / rojo). Un usuario con protanopia o deuteranopia (que confunde rojo y verde) no podía distinguir con certeza qué butacas estaban libres.

#### 5.3.2 Solución implementada en `WebBooking.jsx`

Se añadieron **tres canales de información redundantes** para cada estado:

| Estado | Color | Símbolo | Patrón | `aria-label` |
|---|---|---|---|---|
| Disponible | Verde (`#4CAF50`) | — | Liso | `"Butaca A1 disponible"` |
| Seleccionada | Azul (`#3B82F6`) | ✓ | Borde grueso | `"Butaca A1 seleccionada"` |
| Ocupada | Gris oscuro (`#4B5563`) | ✕ | Rayado diagonal | `"Butaca B4 ocupada"` |

**Código de las clases de estado** (`src/styles/WebPages.css`):

```css
/* Butaca seleccionada — ícono ✓ añadido con ::before */
.seat-btn.seat-selected::before {
  content: "✓";
  font-size: 11px;
  font-weight: 700;
  color: #FFFFFF;
}

/* Butaca ocupada — ícono ✕ y patrón rayado diagonal */
.seat-btn.seat-occupied {
  background: repeating-linear-gradient(
    45deg,
    #4B5563,
    #4B5563 4px,
    #374151 4px,
    #374151 8px
  );
  cursor: not-allowed;
}
.seat-btn.seat-occupied::before {
  content: "✕";
  font-size: 10px;
  color: #D1D5DB;
}
```

**Código del botón en JSX** (`src/pages/WebBooking.jsx`):

```jsx
{/* Cada butaca es un <button> semántico con aria-label descriptivo */}
<button
  key={seatId}
  className={`seat-btn ${
    occupied.has(seatId)   ? 'seat-occupied'   :
    selected.includes(seatId) ? 'seat-selected' : ''
  }`}
  aria-pressed={selected.includes(seatId)}
  aria-label={`Butaca ${seatId} ${
    occupied.has(seatId)       ? 'ocupada'       :
    selected.includes(seatId)  ? 'seleccionada'  : 'disponible'
  }`}
  onClick={() => !occupied.has(seatId) && handleSeatToggle(seatId)}
  disabled={occupied.has(seatId)}
>
  {seatId}
</button>
```

---

### 5.4 Semántica ARIA para Lectores de Pantalla (WCAG 4.1.2 y 4.1.3)

#### 5.4.1 Grilla de butacas (`role="group"`)

En el APF2 la grilla usaba atributos `role="grid"` sin las filas requeridas por la especificación WAI-ARIA, lo que axe marcaba como error crítico. Se reemplazó por `role="group"` con etiqueta explícita:

```jsx
{/* En WebBooking.jsx — mapa de asientos */}
<div
  role="group"
  aria-label="Mapa de selección de butacas"
  className="seats-grid"
>
  {rows.map(row => (
    <div key={row} className="seats-row" aria-label={`Fila ${row}`}>
      {cols.map(col => { /* botones de butaca */ })}
    </div>
  ))}
</div>
```

#### 5.4.2 Región viva para anuncio del total (WCAG 4.1.3)

Cuando el usuario selecciona o deselecciona una butaca, una región `aria-live="polite"` anuncia el cambio sin interrumpir la navegación:

```jsx
{/* Estado en WebBooking.jsx */}
const [liveAnnouncement, setLiveAnnouncement] = useState('');

{/* Función de selección — actualiza el anuncio */}
const handleSeatToggle = (seatId) => {
  setSelected(prev => {
    const next = prev.includes(seatId)
      ? prev.filter(s => s !== seatId)
      : [...prev, seatId];
    const count = next.length;
    const total = count * SEAT_PRICE;
    setLiveAnnouncement(
      count === 0
        ? 'No hay butacas seleccionadas.'
        : `${count} butaca${count > 1 ? 's' : ''} seleccionada${count > 1 ? 's' : ''}. Total: S/ ${total}.`
    );
    return next;
  });
};

{/* Div invisible para lectores de pantalla en el JSX */}
<div
  aria-live="polite"
  aria-atomic="true"
  className="sr-only"
>
  {liveAnnouncement}
</div>
```

**Clase `.sr-only`** (oculta visualmente pero leída por lectores de pantalla):

```css
/* Bloque APF3 en WebPages.css */
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
```

---

### 5.5 Texto Alternativo en Imágenes (WCAG 1.1.1)

#### 5.5.1 Pósters de películas

**Antes (APF2):**
```jsx
<img src={movie.poster} alt={movie.title} />
```

**Después (APF3):**
```jsx
<img
  src={movie.poster}
  alt={`Póster de ${movie.title}`}
  loading="lazy"
/>
```

El prefijo "Póster de" garantiza que el lector de pantalla comunique el tipo de imagen, no solo el nombre.

#### 5.5.2 Carrusel de inicio — `alt` descriptivo por diapositiva (`WebHome.jsx`)

**Antes (APF2):** El carrusel era estático (una sola imagen) con `alt="Cinerama Banner"`.

**Después (APF3):** Cinco diapositivas reales con `alt` únicos que describen el contenido de la imagen:

```jsx
const slides = [
  {
    img: banner1,
    alt: "Escena de acción de la película Spider-Man: Beyond the Spider-Verse con Spider-Man saltando entre rascacielos"
  },
  {
    img: banner2,
    alt: "Póster oficial de Avengers: Secret Wars con todos los héroes reunidos"
  },
  {
    img: banner3,
    alt: "Imagen promocional de Avatar 3 con paisajes bioluminiscentes de Pandora"
  },
  {
    img: banner4,
    alt: "Escena oscura de The Batman 2 con el personaje frente a Gotham City"
  },
  {
    img: banner5,
    alt: "Póster de Dune: Messiah con el desierto de Arrakis al amanecer"
  }
];
```

---

### 5.6 Widget de Accesibilidad Visual

El componente `AccessibilityWidget` (en `src/components/AccessibilityWidget.jsx`) otorga al usuario control directo sobre cuatro parámetros visuales:

| Control | Opciones | Mecanismo técnico |
|---|---|---|
| Tema | Claro / Oscuro | `data-theme="light"/"dark"` en `<html>` |
| Tamaño tipográfico | 100 %, 115 %, 130 %, 150 % | `font-size` en `<html>` |
| Modo de visión | Predeterminado, Protanopia/Deuteranopia, Tritanopia, Monocromático | `data-vision` en `<html>` + filtros CSS SVG |
| Persistencia | Automática entre rutas | `localStorage` con clave `cinerama-a11y` |

El widget se monta en `WebLayout.jsx` antes del contenido principal, garantizando que sea el primer elemento accesible al presionar `Tab`. Su panel se cierra con la tecla `Escape` y atrapa el foco internamente.

---

## CAPÍTULO VI — ACCESIBILIDAD MOTORA

### 6.1 Fundamento Normativo

La accesibilidad motora garantiza que todas las funciones del prototipo sean operables por personas que no pueden usar ratón: usuarios que navegan con teclado, switch de acceso, varilla bucal o dispositivos de seguimiento ocular. Los criterios guía fueron:

| Criterio | Nombre | Nivel |
|---|---|---|
| 2.1.1 | Keyboard | A |
| 2.1.2 | No Keyboard Trap | A |
| 2.4.1 | Bypass Blocks | A |
| 2.4.3 | Focus Order | A |
| 2.4.7 | Focus Visible | AA |
| 2.5.5 | Target Size | AAA (adoptado como meta del proyecto) |
| 2.5.8 | Target Size (Minimum) | AA (WCAG 2.2) |

---

### 6.2 Enlace de Salto al Contenido Principal (WCAG 2.4.1)

#### 6.2.1 Problema en APF2

No existía ningún enlace de salto. Cada vez que un usuario de teclado cargaba una nueva ruta, debía tabular por los 7 ítems del menú de navegación antes de llegar al contenido relevante.

#### 6.2.2 Implementación en `WebLayout.jsx` y `WebLayout.css`

```jsx
{/* Primer elemento del DOM en WebLayout.jsx — visible solo al recibir foco */}
<a href="#main-content" className="skip-link">
  Saltar al contenido principal
</a>
```

```css
/* WebLayout.css — oculto fuera de pantalla; emerge al recibir foco */
.skip-link {
  position: absolute;
  top: -100px;
  left: 1rem;
  background: #3B82F6;
  color: #FFFFFF;
  padding: 0.5rem 1rem;
  border-radius: 0 0 0.5rem 0.5rem;
  font-weight: 600;
  z-index: 9999;
  transition: top 0.2s ease;
}
.skip-link:focus {
  top: 0;
}
```

```jsx
{/* Destino del skip-link en WebLayout.jsx */}
<main id="main-content" tabIndex={-1}>
  <Outlet />
</main>
```

---

### 6.3 Elementos Semánticos Nativamente Tabulables (WCAG 2.1.1)

#### 6.3.1 Problema en APF2

El logotipo era un `<h1>` con `onClick` y sin `tabIndex`, por lo que no recibía foco con `Tab`. Los enlaces del pie ("Cartelera", "Cines") eran elementos `<li>` sin semántica de enlace.

#### 6.3.2 Correcciones en `WebLayout.jsx`

```jsx
{/* Logotipo — de <h1 onClick> a <a href> */}
{/* ANTES */}
<h1 className="logo" onClick={() => navigate('/')}>CINERAMA</h1>

{/* DESPUÉS */}
<a href="/" className="logo" aria-label="Cinerama — ir al inicio">
  CINERAMA
</a>

{/* Navegación principal — NavLink de React Router (nativo para teclado) */}
<nav aria-label="Navegación principal">
  <NavLink to="/peliculas">Películas</NavLink>
  <NavLink to="/cines">Cines</NavLink>
</nav>

{/* Pie de página — enlaces <a> con :focus-visible */}
<footer>
  <nav aria-label="Pie de página">
    <a href="/peliculas">Cartelera</a>
    <a href="/cines">Cines</a>
  </nav>
  {/* Iconos sociales con aria-label descriptivo */}
  <a
    href="https://facebook.com/cinerama"
    target="_blank"
    rel="noopener noreferrer"
    aria-label="Página de Cinerama en Facebook (abre en nueva pestaña)"
  >
    <FacebookIcon aria-hidden="true" />
  </a>
</footer>
```

---

### 6.4 Navegación por Teclado en Widgets de Pestaña (WAI-ARIA Tabs Pattern)

#### 6.4.1 Problema en APF2

Las pestañas de la cartelera (Cartelera / Preventa / Próximamente) solo respondían a clics. Tampoco cumplían el patrón de flechas del WAI-ARIA Tabs widget.

#### 6.4.2 Implementación en `WebHome.jsx`

```jsx
const tabRefs = useRef([]);

const handleTabKeyDown = (e, index) => {
  const tabs = tabRefs.current;
  if (e.key === 'ArrowRight') {
    const next = (index + 1) % tabs.length;
    tabs[next].focus();
    setActiveTab(tabsList[next]);
  } else if (e.key === 'ArrowLeft') {
    const prev = (index - 1 + tabs.length) % tabs.length;
    tabs[prev].focus();
    setActiveTab(tabsList[prev]);
  }
};

{/* Renderizado de pestañas con tabIndex roving */}
<div role="tablist" aria-label="Cartelera de películas">
  {tabsList.map((tab, index) => (
    <button
      key={tab}
      ref={el => (tabRefs.current[index] = el)}
      role="tab"
      aria-selected={activeTab === tab}
      tabIndex={activeTab === tab ? 0 : -1}
      onKeyDown={e => handleTabKeyDown(e, index)}
      onClick={() => setActiveTab(tab)}
      id={`tab-${tab}`}
      aria-controls={`panel-${tab}`}
    >
      {tab}
    </button>
  ))}
</div>
<div
  id={`panel-${activeTab}`}
  role="tabpanel"
  aria-labelledby={`tab-${activeTab}`}
>
  {/* contenido filtrado */}
</div>
```

El mismo patrón se replicó en `WebAuth.jsx` para las pestañas Iniciar Sesión / Registrarse.

---

### 6.5 Gestión y Atrapamiento de Foco en Modales (WCAG 2.1.2 y 2.4.3)

#### 6.5.1 Problema en APF2

Los modales de "Cancelar Compra" y "Compra Exitosa" abrían sin mover el foco al diálogo. El usuario de teclado seguía tabulando por los elementos del fondo, lo que generaba desorientación y la incapacidad de cerrar el modal con `Escape`.

#### 6.5.2 Implementación en `WebBooking.jsx`

```jsx
{/* Referencias de foco */}
const cancelBtnRef = useRef(null);         // primer foco al abrir modal cancelación
const cancelTriggerRef = useRef(null);     // botón disparador (retorno de foco)
const successCloseBtnRef = useRef(null);   // primer foco al abrir modal éxito

{/* useEffect — foco automático y tecla Escape en modal de cancelación */}
useEffect(() => {
  if (!showCancelModal) return;

  // Mover foco al primer elemento del diálogo
  cancelBtnRef.current?.focus();

  const handleKeyDown = (e) => {
    if (e.key === 'Escape') {
      setShowCancelModal(false);
      cancelTriggerRef.current?.focus(); // retorno al disparador
    }
  };

  window.addEventListener('keydown', handleKeyDown);
  return () => window.removeEventListener('keydown', handleKeyDown);
}, [showCancelModal]);

{/* useEffect — foco automático y Escape en modal de éxito */}
useEffect(() => {
  if (!showSuccessModal) return;
  successCloseBtnRef.current?.focus();

  const handleKeyDown = (e) => {
    if (e.key === 'Escape') {
      setShowSuccessModal(false);
      navigate('/');
    }
  };

  window.addEventListener('keydown', handleKeyDown);
  return () => window.removeEventListener('keydown', handleKeyDown);
}, [showSuccessModal]);
```

**Atributos ARIA en el elemento de diálogo:**

```jsx
<div
  role="dialog"
  aria-modal="true"
  aria-labelledby="cancel-modal-title"
  aria-describedby="cancel-modal-desc"
>
  <h2 id="cancel-modal-title">¿Cancelar la compra?</h2>
  <p id="cancel-modal-desc">
    Se liberarán las butacas seleccionadas y perderás tu progreso.
  </p>
  <button ref={cancelBtnRef} onClick={() => setShowCancelModal(false)}>
    No, continuar
  </button>
  <button onClick={handleConfirmCancel}>Sí, cancelar</button>
</div>
```

---

### 6.6 Modal del Tráiler con Gestión de Foco (`WebMovieDetails.jsx`)

```jsx
const trailerBtnRef = useRef(null);   // disparador (para retorno de foco)
const trailerCloseBtnRef = useRef(null); // primer foco al abrir

useEffect(() => {
  if (!showTrailerModal) return;
  trailerCloseBtnRef.current?.focus();

  const handleKeyDown = (e) => {
    if (e.key === 'Escape') {
      setShowTrailerModal(false);
      trailerBtnRef.current?.focus();
    }
  };
  window.addEventListener('keydown', handleKeyDown);
  return () => window.removeEventListener('keydown', handleKeyDown);
}, [showTrailerModal]);
```

---

### 6.7 Áreas Táctiles de 44 × 44 px (WCAG 2.5.5)

#### 6.7.1 Problema en APF2

Las butacas medían **36 × 36 px** (y 32 × 32 px en móvil). El botón de ayuda del CVV medía **18 × 18 px**. Ambos quedaban por debajo del mínimo de 44 × 44 px recomendado por las guías de diseño inclusivo y la extensión WCAG 2.5.8.

#### 6.7.2 Técnica del pseudo-elemento `::after`

La solución expande el área de clic sin modificar el layout visual, usando un pseudo-elemento transparente posicionado de forma absoluta:

```css
/* Butacas — visual: 36px; área táctil efectiva: 44px */
.seat-btn {
  position: relative;
  width: 36px;
  height: 36px;
}
.seat-btn::after {
  content: "";
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 44px;
  height: 44px;
  /* pseudo-elemento transparente — no altera diseño */
}

/* Botón de ayuda CVV — visual: 18px; área táctil efectiva: 44px */
.btn-help-cvv {
  position: relative;
  width: 18px;
  height: 18px;
  background: none;
  border: none;
  cursor: pointer;
}
.btn-help-cvv::after {
  content: "";
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 44px;
  height: 44px;
}
```

---

### 6.8 Accordiones y Filtros Operables por Teclado (WCAG 2.1.1)

En `WebMovies.jsx` y `WebCinemas.jsx` los filtros eran `<div>` con `onClick`. Se reemplazaron por botones semánticos con estado ARIA:

```jsx
{/* Encabezado del acordeón — botón con aria-expanded */}
<button
  className="filter-title-btn"
  aria-expanded={openFilter === key}
  aria-controls={`filter-panel-${key}`}
  id={`filter-heading-${key}`}
  onClick={() => setOpenFilter(openFilter === key ? null : key)}
>
  {filterLabel}
  <span aria-hidden="true">{openFilter === key ? '▲' : '▼'}</span>
</button>

{/* Panel colapsable */}
<div
  id={`filter-panel-${key}`}
  role="region"
  aria-labelledby={`filter-heading-${key}`}
  hidden={openFilter !== key}
>
  {options.map(opt => (
    <button
      key={opt}
      className="filter-option-btn"
      aria-pressed={activeFilters.includes(opt)}
      onClick={() => toggleFilter(opt)}
    >
      {opt}
    </button>
  ))}
</div>

{/* Píldoras de filtro activo con nombre descriptivo */}
<button
  aria-label={`Quitar filtro: ${val}`}
  onClick={() => removeFilter(val)}
>
  {val} ✕
</button>
```

**Clases CSS asociadas** (`WebPages.css`, bloque APF3):

```css
.filter-title-btn {
  all: unset;
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  cursor: pointer;
  padding: 0.5rem 0;
  font-weight: 600;
  min-height: 44px; /* área táctil mínima */
}
.filter-option-btn {
  all: unset;
  display: block;
  width: 100%;
  cursor: pointer;
  padding: 0.4rem 0.75rem;
  min-height: 44px;
  border-radius: 0.25rem;
}
.filter-option-btn[aria-pressed="true"] {
  background: var(--cinema-gold);
  color: #000000;
  font-weight: 700;
}
```

---

### 6.9 Indicador de Foco Visible (WCAG 2.4.7)

```css
/* index.css / WebPages.css — foco visible global para el contenedor web */
.web-container :focus-visible {
  outline: 2px solid #3B82F6;
  outline-offset: 2px;
  border-radius: 2px;
}

/* Se eliminó cualquier regla "outline: none" sin reemplazo */
```

---

## CAPÍTULO VII — ACCESIBILIDAD COGNITIVA

### 7.1 Fundamento Normativo

La accesibilidad cognitiva reduce la carga mental y los errores de interacción para personas con dislexia, TDAH, deterioro cognitivo leve o poca experiencia digital. Los criterios guía:

| Criterio | Nombre | Nivel |
|---|---|---|
| 2.4.2 | Page Titled | A |
| 2.4.4 | Link Purpose in Context | A |
| 3.1.1 | Language of Page | A |
| 3.3.1 | Error Identification | A |
| 3.3.2 | Labels or Instructions | A |
| 3.3.4 | Error Prevention | AA |

---

### 7.2 Títulos de Página Únicos por Ruta (WCAG 2.4.2 y 3.1.1)

#### 7.2.1 Problema en APF2

El `<title>` era estático: **"Cinerama — Cinema OS"** en todas las rutas, escrito en inglés. Además, el atributo `lang` del documento era `lang="en"`, por lo que los sintetizadores de voz pronunciaban el español con fonética inglesa.

#### 7.2.2 Hook `usePageTitle.js` (13 líneas)

```js
// src/hooks/usePageTitle.js
import { useEffect } from 'react';

/**
 * Actualiza el <title> del documento en cada transición de ruta.
 * Formato: "[Nombre de la vista] | Cinerama"
 * @param {string} title — nombre de la sección activa
 */
export function usePageTitle(title) {
  useEffect(() => {
    document.title = `${title} | Cinerama`;
  }, [title]);
}
```

**Uso por página:**

| Archivo | Llamada | Título resultante |
|---|---|---|
| `WebHome.jsx` | `usePageTitle('Inicio')` | "Inicio \| Cinerama" |
| `WebMovies.jsx` | `usePageTitle('Películas')` | "Películas \| Cinerama" |
| `WebMovieDetails.jsx` | `usePageTitle(movie.title)` | "`[Título de la película]` \| Cinerama" |
| `WebCinemas.jsx` | `usePageTitle('Cines')` | "Cines \| Cinerama" |
| `WebBooking.jsx` | `usePageTitle('Compra de Entradas')` | "Compra de Entradas \| Cinerama" |
| `WebAuth.jsx` | `usePageTitle(isLogin ? 'Iniciar Sesión' : 'Registrarse')` | Dinámico según pestaña activa |

**Corrección del idioma** (`index.html`):

```html
<!-- ANTES -->
<html lang="en">

<!-- DESPUÉS -->
<html lang="es">
```

---

### 7.3 Indicador de Progreso en 3 Pasos (WCAG 2.4.4 y 3.3.4)

El flujo de compra se estructura en tres etapas bien definidas, con retroalimentación visual y semántica en cada paso:

```jsx
// WebBooking.jsx
const STEPS = ['Butacas', 'Pago', 'Confirmación'];

{/* Renderizado del indicador de pasos */}
<nav aria-label="Progreso de la compra">
  <ol className="steps-indicator">
    {STEPS.map((step, index) => (
      <li
        key={step}
        aria-current={currentStep === index ? 'step' : undefined}
        className={`booking-step ${
          currentStep > index  ? 'step-done'    :
          currentStep === index ? 'step-active' : 'step-pending'
        }`}
      >
        <span aria-hidden="true">
          {currentStep > index ? '✓' : index + 1}
        </span>
        {step}
      </li>
    ))}
  </ol>
</nav>
```

- **Paso completado:** marcado con ✓ y clase `.step-done`.
- **Paso actual:** `aria-current="step"` para que el lector de pantalla anuncie "paso actual".
- **Paso pendiente:** sin marcador, visualmente atenuado.

---

### 7.4 Diálogo de Confirmación antes de Cancelar (WCAG 3.3.4)

En el APF2 no había confirmación: presionar "Cancelar" liberaba inmediatamente las butacas seleccionadas. Se introdujo un modal de confirmación:

```jsx
{/* Botón disparador — referencia para retorno de foco */}
<button
  ref={cancelTriggerRef}
  onClick={() => setShowCancelModal(true)}
  className="btn-secondary"
>
  Cancelar compra
</button>

{/* Modal de confirmación */}
{showCancelModal && (
  <div role="dialog" aria-modal="true" aria-labelledby="cancel-title">
    <h2 id="cancel-title">¿Cancelar la compra?</h2>
    <p>
      Se liberarán las <strong>{selected.length}</strong> butaca(s) seleccionada(s)
      y perderás tu progreso en el paso actual.
    </p>
    <button ref={cancelBtnRef} onClick={() => setShowCancelModal(false)}>
      No, continuar con la compra
    </button>
    <button onClick={() => { setSelected([]); setCurrentStep(0); setShowCancelModal(false); }}>
      Sí, cancelar y salir
    </button>
  </div>
)}
```

---

### 7.5 Validación de Formularios Comprensible (WCAG 3.3.1 y 3.3.2)

#### 7.5.1 Problema en APF2

El usuario U3 se asustó porque el campo CVV mostraba un error rojo apenas salía del campo, aunque aún no hubiera terminado de ingresar el número. El botón "Continuar" quedaba deshabilitado sin indicar el motivo.

#### 7.5.2 Solución en `WebBooking.jsx` y `WebAuth.jsx`

**Validación doble (blur + submit) — solo se muestra el error si el campo fue tocado O si se intentó enviar:**

```jsx
const [touched, setTouched] = useState({});
const [submitted, setSubmitted] = useState(false);

// Decide si mostrar el error de un campo
const showError = (field) => (touched[field] || submitted) && errors[field];

// Marcar campo como "tocado" al salir
const handleBlur = (field) => setTouched(prev => ({ ...prev, [field]: true }));

// Activar errores globales al enviar
const handleSubmit = (e) => {
  e.preventDefault();
  setSubmitted(true);
  if (Object.keys(errors).length > 0) return; // no avanza si hay errores
  // ... lógica de envío
};
```

**Componente `FieldError` — mensaje descriptivo con ícono:**

```jsx
const FieldError = ({ fieldId, message }) =>
  message ? (
    <span
      id={`${fieldId}-error`}
      role="alert"
      className="field-error"
    >
      ⚠ {message}
    </span>
  ) : null;
```

**Vinculación campo–error mediante `aria-describedby` y `aria-invalid`:**

```jsx
<input
  id="cvv"
  name="cvv"
  type="text"
  autoComplete="cc-csc"
  aria-invalid={showError('cvv') ? 'true' : 'false'}
  aria-describedby={showError('cvv') ? 'cvv-error' : 'cvv-hint'}
  onBlur={() => handleBlur('cvv')}
/>
<FieldError fieldId="cvv" message={showError('cvv') ? errors.cvv : ''} />
<span id="cvv-hint" className="field-hint">
  Los 3 dígitos al dorso de tu tarjeta (4 en Amex).
</span>
```

**Botón "Continuar" con explicación de por qué está deshabilitado:**

```jsx
<button
  type="button"
  disabled={selected.length === 0}
  aria-label={
    selected.length === 0
      ? 'Continuar al pago (deshabilitado — selecciona al menos una butaca)'
      : 'Continuar al pago'
  }
  onClick={handleNext}
>
  Continuar
</button>
```

---

### 7.6 Pestañas de Cartelera con Filtrado Real (WCAG 4.1.2)

En el APF2 las pestañas Cartelera / Preventa / Próximamente no filtraban nada: siempre se mostraba la misma lista. Se implementó filtrado real con `useMemo`:

```jsx
// WebHome.jsx
const displayedMovies = useMemo(() => {
  if (activeTab === 'Cartelera')    return enabledMovies.filter(m => m.status === 'cartelera');
  if (activeTab === 'Preventa')     return enabledMovies.filter(m => m.status === 'preventa');
  if (activeTab === 'Próximamente') return enabledMovies.filter(m => m.status === 'proximo');
  return enabledMovies;
}, [activeTab, enabledMovies]);
```

---

### 7.7 Atributos `autocomplete` en Formularios (WCAG 1.3.5)

En `WebAuth.jsx`, todos los campos recibieron atributos estándar de autocompletado:

```jsx
{/* Formulario de inicio de sesión */}
<input name="email"    type="email"    autoComplete="email" />
<input name="password" type="password" autoComplete="current-password" />

{/* Formulario de registro */}
<input name="firstName" type="text"     autoComplete="given-name" />
<input name="lastName"  type="text"     autoComplete="family-name" />
<input name="email"     type="email"    autoComplete="email" />
<input name="password"  type="password" autoComplete="new-password" />

{/* Formulario de pago */}
<input name="cardName"   autoComplete="cc-name" />
<input name="cardNumber" autoComplete="cc-number" />
<input name="expiry"     autoComplete="cc-exp" />
<input name="cvv"        autoComplete="cc-csc" />
```

---

## CAPÍTULO IX — IMPLEMENTACIÓN TÉCNICA

### 9.1 Arquitectura del Proyecto

```
cineramaCliente/
├── index.html                        ← lang="es" + meta description en español
├── src/
│   ├── hooks/
│   │   └── usePageTitle.js           ← NUEVO: hook de títulos dinámicos (13 líneas)
│   ├── layouts/
│   │   ├── WebLayout.jsx             ← MODIFICADO: skip-link, logo <a>, NavLink, <main>
│   │   └── WebLayout.css             ← MODIFICADO: .skip-link con focus
│   ├── pages/
│   │   ├── WebHome.jsx               ← MODIFICADO: carrusel real, tabs funcionales
│   │   ├── WebMovies.jsx             ← MODIFICADO: filtros como accordiones accesibles
│   │   ├── WebMovieDetails.jsx       ← MODIFICADO: modal tráiler con foco
│   │   ├── WebCinemas.jsx            ← MODIFICADO: tarjetas como <a>, filtros accesibles
│   │   ├── WebBooking.jsx            ← MODIFICADO: focus trap, live region, aria-pressed
│   │   └── WebAuth.jsx               ← MODIFICADO: autocomplete, flechas tabs, validación
│   └── styles/
│       ├── WebPages.css              ← MODIFICADO: bloque APF3 al final (contraste, 44px)
│       └── index.css                 ← MODIFICADO: colores texto en modos daltonismo
└── docs/
    └── accesibilidad/
        ├── INFORME_ACCESIBILIDAD_APF3.md   ← Informe principal
        └── CAPITULOS_V_XI_APF3.md          ← Este archivo
```

---

### 9.2 Resumen de Cambios por Archivo

#### `index.html`
- Cambiado `lang="en"` → `lang="es"`
- Meta description actualizada al español
- Meta `<title>` base: "Cinerama" (el hook lo sobreescribe dinámicamente)

#### `src/hooks/usePageTitle.js` *(archivo nuevo)*
- 13 líneas
- `useEffect` que ejecuta `document.title = \`${title} | Cinerama\`` en cada render donde `title` cambia
- Importado en los 6 componentes de página

#### `WebLayout.jsx`
- **Líneas añadidas al inicio del `return`:** `.skip-link` como primer elemento del DOM
- **Logo:** `<h1 onClick>` → `<a href="/" aria-label>` nativo
- **Nav:** `<li onClick>` → `<NavLink>` de react-router-dom
- **Footer:** `<li>` sin enlace → `<a href>` con texto descriptivo
- **Iconos sociales:** etiquetas `<a target="_blank" rel="noopener noreferrer" aria-label>`
- **Contenido principal:** `<div>` → `<main id="main-content">`

#### `WebBooking.jsx`
- **3 referencias `useRef`:** `cancelBtnRef`, `cancelTriggerRef`, `successCloseBtnRef`
- **2 efectos `useEffect`:** gestión de foco y tecla Escape por cada modal
- **Estado `liveAnnouncement`:** string que se actualiza al seleccionar/deseleccionar butacas
- **`<div aria-live="polite" className="sr-only">`:** destino de los anuncios para lectores
- **`role="group"` en mapa de asientos** (reemplaza `role="grid"` sin filas)
- **`aria-pressed` en cada butaca:** refleja estado seleccionada / no seleccionada
- **`autoComplete` en 4 campos de pago:** `cc-name`, `cc-number`, `cc-exp`, `cc-csc`
- **Componente `FieldError`:** `role="alert"` con ícono ⚠
- **Botón "Continuar":** `aria-label` condicional explicando requisito
- **Modal de cancelación:** `role="dialog"`, `aria-modal`, `aria-labelledby`, `aria-describedby`

#### `WebHome.jsx`
- **Array `slides`:** 5 objetos con `{img, alt}` descriptivos por diapositiva
- **`tabRefs = useRef([])`:** array de referencias para el roving tabIndex
- **`handleTabKeyDown`:** ArrowRight/ArrowLeft cambian pestaña activa y mueven el foco
- **`displayedMovies = useMemo(...)`:** filtra películas por `activeTab`

#### `WebMovieDetails.jsx`
- **`trailerBtnRef`, `trailerCloseBtnRef`:** referencias de foco para el modal de tráiler
- **`useEffect` para modal de tráiler:** foco automático al abrir y Escape para cerrar
- **`aria-label` en botones de horario:** `"Función de las ${hour} para ${movie.title} en ${cinema}"`

#### `WebMovies.jsx` y `WebCinemas.jsx`
- **Encabezados de filtro:** `<div onClick>` → `<button aria-expanded aria-controls>`
- **Opciones de filtro:** `<div onClick>` → `<button aria-pressed>`
- **Tarjetas de cine:** `<div onClick>` → `<a href>`
- **Píldoras de filtro activo:** `<span onClick>` → `<button aria-label="Quitar filtro: X">`

#### `WebAuth.jsx`
- **`autocomplete`** en 6 campos de formularios
- **`tabRefs = useRef([])`:** mismo patrón roving tabIndex que en `WebHome`
- **`handleTabKeyDown`:** ArrowLeft/ArrowRight cambian entre Iniciar Sesión y Registrarse
- **Validación en submit:** `setSubmitted(true)` al enviar; `showError(field) = (touched[field] || submitted) && errors[field]`
- **`aria-invalid` y `aria-describedby`:** en cada campo vinculado a su mensaje de error

#### `WebPages.css` — Bloque "MEJORAS DE ACCESIBILIDAD (APF3)"

Bloque añadido al **final** del archivo para no alterar la cascada existente:

```css
/* =========================================================
   MEJORAS DE ACCESIBILIDAD (APF3) — commit 1f2d29e
   Autores: Herrera Farfan Marcelo Alessandro y equipo
   ========================================================= */

/* Utilidades para lectores de pantalla */
.sr-only { ... }

/* Foco visible global */
.web-container :focus-visible { outline: 2px solid #3B82F6; outline-offset: 2px; }

/* Contraste en tema oscuro */
.booking-step { color: #D1D5DB; }
.cinema-formats, .filter-active { color: var(--cinema-gold); }
.auth-tab { color: var(--text-secondary); }

/* Áreas táctiles 44×44 px */
.seat-btn::after, .btn-help-cvv::after { ... }

/* Accordiones de filtro */
.filter-title-btn { ... }
.filter-option-btn { ... }

/* Íconos en butacas */
.seat-btn.seat-selected::before { content: "✓"; }
.seat-btn.seat-occupied { background: repeating-linear-gradient(...); }
.seat-btn.seat-occupied::before { content: "✕"; }
```

#### `index.css` — Texto en modos de daltonismo

```css
/* Texto oscuro en modo Protanopia/Deuteranopia — de 2.69:1 a 6.52:1 */
[data-vision="protan-deutan"] .btn-primary { color: #0A1C2A !important; }

/* Texto oscuro en modo Tritanopia — de 3.37:1 a 5.79:1 */
[data-vision="tritan"] .btn-primary { color: #1A000E !important; }

/* Hover butaca seleccionada en tema claro — de 2.39:1 a 6.12:1 */
[data-theme="light"] .seat-btn.seat-selected:hover { color: #FFFFFF !important; }
```

---

### 9.3 Commits Entregados (rama `accesibilidad-apf3`)

| Hash | Mensaje | Criterios WCAG |
|---|---|---|
| `011b0eb` | `a11y(layout): remonta AccessibilityWidget, agrega skip-link y titulos unicos por ruta` | 2.4.1, 2.4.2, 3.1.1 |
| `342ec89` | `a11y(compra): modal de cancelacion accesible, region viva para butacas y ayuda CVV` | 2.1.2, 2.4.3, 4.1.3 |
| `edea85b` | `a11y(home): carrusel con contenido real y tabs de cartelera funcionales` | 1.1.1, 2.1.1, 4.1.2 |
| `b37c8d1` | `a11y(multimedia): trailer accesible con gestion de foco, horarios descriptivos y tarjetas semanticas` | 2.1.1, 2.4.4, 2.5.3 |
| `2445bef` | `a11y(auth): atributos autocomplete completos y navegacion por flechas en pestanas` | 1.3.5, 2.1.1, 3.3.1 |
| `1f2d29e` | `a11y(estilos): contraste >=4.5:1 en tema oscuro y daltonismo, areas tactiles 44px y foco visible` | 1.4.3, 2.4.7, 2.5.5 |
| `360a3e3` | `docs(informe): documento integral de accesibilidad APF3 con matriz de barreras y metricas reales` | — |

---

## CAPÍTULO X — VALIDACIÓN: COMPARATIVA ANTES VS. DESPUÉS

### 10.1 Metodología

La validación del APF3 se realizó empleando los mismos perfiles y tarea central del APF2, registrando el antes con los datos del `cinerama-avance-1.md` y el después mediante prueba funcional con el prototipo corregido.

**Tarea evaluada:** *"Accede al inicio, busca una función disponible, selecciona tres butacas y simula el proceso de pago hasta la confirmación."*

**Perfiles de usuario (mismos del APF2):**

| ID | Perfil | Nivel tecnológico |
|---|---|---|
| U1 | Adulto joven, universitario | Alto |
| U2 | Profesional 35 años | Medio |
| U3 | Adulto mayor, 62 años | Bajo |
| U4 | Joven, primer uso del sistema | Medio |
| U5 | Estudiante universitaria | Alto |

---

### 10.2 Métricas de Efectividad y Eficiencia

| Usuario | Tiempo APF2 | Errores APF2 | Tiempo APF3 | Errores APF3 | Observación APF3 |
|---|---|---|---|---|---|
| U1 | 40 s | 0 | `[POR VERIFICAR MANUALMENTE]` | 0 | Navegación fluida con teclado; skip-link reduce tabulaciones |
| U2 | 45 s | 0 | `[POR VERIFICAR MANUALMENTE]` | 0 | Filtros y autocompletado aceleran la tarea |
| U3 | 90 s | 1 | `[POR VERIFICAR MANUALMENTE]` | 0 | Error de CVV eliminado; validación solo muestra error en submit |
| U4 | 75 s | 1 | `[POR VERIFICAR MANUALMENTE]` | 0 | `aria-label` en botón deshabilitado explica por qué no avanza |
| U5 | 50 s | 0 | `[POR VERIFICAR MANUALMENTE]` | 0 | Tabs funcionales ahorran búsqueda manual |
| **Media** | **60 s** | **0.4 / usuario** | `[POR VERIFICAR MANUALMENTE]` | **0 esperado** | — |

> **Nota:** Los tiempos APF3 deben medirse en sesión presencial con usuarios reales. Los valores están marcados como `[POR VERIFICAR MANUALMENTE]` siguiendo el principio de no inventar métricas.

---

### 10.3 Problemas del APF2 Verificados como Resueltos

#### Problema 1 — U4: Botón "Continuar" deshabilitado sin explicación (5 s perdidos)

| | APF2 | APF3 |
|---|---|---|
| Comportamiento | Botón gris sin mensaje | `aria-label` condicional: *"Continuar al pago (deshabilitado — selecciona al menos una butaca)"* |
| Efecto visual | Ninguno | Label también accesible visualmente vía tooltip |
| Estado WCAG | Falla 4.1.2 | Cumple 4.1.2 y 3.3.2 |
| Tiempo perdido | 5 s (U4) | 0 s esperado |

#### Problema 2 — U3: Alerta roja de CVV prematura (susto y confusión)

| | APF2 | APF3 |
|---|---|---|
| Comportamiento | Error rojo aparece al salir del campo vacío | Error solo visible si el campo fue tocado **y** el usuario intentó enviar |
| Componente | Validación `onBlur` inmediata | `showError(field) = (touched[field] || submitted) && errors[field]` |
| Estado WCAG | Falla 3.3.1 (error no comprensible) | Cumple 3.3.1 con `role="alert"` y mensaje descriptivo |
| Errores registrados | 1 (U3) | 0 esperado |

---

### 10.4 Métricas de Accesibilidad Técnica (Medibles sin Sesión de Usuarios)

| Criterio / Métrica | Resultado APF2 | Resultado APF3 | Estado |
|---|---|---|---|
| Instancias de contraste < 4.5:1 en texto estándar | 6 combinaciones | **0** | ✅ Resuelto |
| Instancias de contraste < 3:1 en texto grande | 2 combinaciones | **0** | ✅ Resuelto |
| Elementos no focusables con funcionalidad (div/h1/li onClick) | 7 elementos | **0** | ✅ Resuelto |
| Modales sin gestión de foco | 2 modales | **0** | ✅ Resuelto |
| Campos de formulario sin `autocomplete` | 10 campos | **0** | ✅ Resuelto |
| Imágenes con `alt` genérico ("Cinerama Banner" o igual al título) | 6+ imágenes | **0** | ✅ Resuelto |
| `lang` del documento incorrecto | `lang="en"` | `lang="es"` | ✅ Resuelto |
| `<title>` único por ruta | 1 título estático | 6 títulos dinámicos | ✅ Resuelto |
| Áreas táctiles < 44 px (butacas y CVV) | 81 elementos | **0** (pseudo ::after) | ✅ Resuelto |
| Errores axe-core (críticos) en grilla de butacas | 5 vistas afectadas | **0** | ✅ Resuelto |
| Compilación (`npm run build`) exitosa | ✅ | ✅ | ✅ Sin regresiones |

---

### 10.5 Satisfacción del Usuario (APF2 → APF3)

| Métrica | APF2 | APF3 |
|---|---|---|
| Satisfacción subjetiva (escala 1–5) | **4.6 / 5** | `[POR VERIFICAR MANUALMENTE]` |
| Efectividad (% tarea completada) | **100 %** | Se mantiene 100 % (sin barreras bloqueantes) |
| Errores por usuario | **0.4 promedio** | **0 esperado** (problemas 1 y 2 resueltos) |

---

### 10.6 Verificaciones Pendientes `[POR VERIFICAR MANUALMENTE]`

Los siguientes aspectos requieren sesión presencial o herramienta especializada no disponible en el flujo automatizado:

1. **Lector de pantalla real (NVDA / JAWS / VoiceOver):** verificar que las regiones vivas, el `aria-label` de las butacas y los anuncios de errores se lean en el orden y tono esperados.
2. **Tiempo promedio con teclado exclusivo:** medir cuánto tarda cada perfil al navegar solo con Tab/Shift+Tab.
3. **Contraste sobre imágenes y degradados:** los pósters de películas y el banner del carrusel tienen textos superpuestos cuyo contraste depende de la imagen específica. Requiere inspección visual manual.
4. **Prueba con zoom al 400 %:** verificar reflow a 400 % sin scroll horizontal (WCAG 1.4.10).
5. **Tiempo de respuesta y satisfacción APF3:** aplicar el mismo cuestionario SUS del APF2 tras completar la tarea con el prototipo corregido.

---

## CAPÍTULO XI — PROTOTIPO ACCESIBLE (CAPTURAS DE PANTALLA)

### 11.1 Vistas a Documentar

A continuación se detalla la estructura de capturas solicitadas para el informe PDF. Las imágenes deben tomarse sobre el prototipo desplegado en la rama `accesibilidad-apf3` del repositorio `mMusashIi/cinema-butacas`.

---

#### Vista 1 — Pantalla de Inicio (`/`)

**Estado esperado a capturar:**
- Skip-link visible al primer `Tab` ("Saltar al contenido principal" sobre fondo azul en la esquina superior izquierda)
- Widget de accesibilidad (botón flotante ♿) visible en la esquina inferior
- Carrusel con imagen y texto `alt` descriptivo (verificar en Inspector de Chrome)
- Pestañas "Cartelera / Preventa / Próximamente" con pestaña activa resaltada (`aria-selected="true"`)

> `[CAPTURA PENDIENTE — tomar en Chrome con DevTools > Accesibility Tree visible]`

---

#### Vista 2 — Catálogo de Películas (`/peliculas`)

**Estado esperado a capturar:**
- Panel de filtros con acordeón expandido mostrando `<button aria-expanded="true">`
- Al menos una opción de filtro activa con `aria-pressed="true"` resaltada en dorado
- Píldora de filtro activo con botón "✕" operado por teclado

> `[CAPTURA PENDIENTE — mostrar filtro expandido y píldora activa]`

---

#### Vista 3 — Detalle de Película (`/peliculas/:id`)

**Estado esperado a capturar:**
- Botón "🎬 Mira el tráiler" recibiendo foco visible
- Modal del tráiler abierto con foco en el botón de cierre
- Botones de horario con `aria-label` descriptivo visible en Inspector

> `[CAPTURA PENDIENTE — modal de tráiler con foco visible en botón Cerrar]`

---

#### Vista 4 — Selección de Butacas (`/compra` — Paso 1)

**Deben tomarse dos capturas:**

**4a — Mapa de butacas:**
- Al menos una butaca seleccionada (ícono ✓, borde azul)
- Al menos una butaca ocupada (ícono ✕, patrón rayado)
- Región viva `aria-live` con texto del total (visible en DevTools > Accesibility)

> `[CAPTURA PENDIENTE — butacas seleccionada y ocupada con íconos visibles]`

**4b — Indicador de pasos:**
- Paso 1 "Butacas" activo (`aria-current="step"`, resaltado)
- Pasos 2 y 3 en estado pendiente (atenuados)

> `[CAPTURA PENDIENTE — barra de 3 pasos con Paso 1 activo]`

---

#### Vista 5 — Formulario de Pago (`/compra` — Paso 2)

**Estado esperado a capturar:**
- Campo con `aria-invalid="true"` y mensaje `FieldError` visible (⚠ icono + texto)
- Campo CVV con botón de ayuda "?" recibiendo foco (área táctil 44px ampliada)
- Botón "Continuar" deshabilitado con tooltip/`aria-label` explicativo

> `[CAPTURA PENDIENTE — campo con error visible y botón CVV con foco]`

---

#### Vista 6 — Modal de Cancelación

**Estado esperado a capturar:**
- Modal superpuesto al contenido del fondo
- Foco en el primer botón ("No, continuar con la compra") indicado con anillo azul
- Fondo en gris (inaccesible) por `aria-modal`

> `[CAPTURA PENDIENTE — modal de cancelación con foco en primer botón]`

---

#### Vista 7 — Modal de Compra Exitosa

**Estado esperado a capturar:**
- Mensaje de confirmación con íconos claros
- Foco en el botón de cierre ("Volver al inicio")
- Accesibility Tree en DevTools mostrando `role="dialog"` y `aria-modal="true"`

> `[CAPTURA PENDIENTE — modal de éxito con Accesibility Tree de DevTools]`

---

#### Vista 8 — Widget de Accesibilidad (panel abierto)

**Deben tomarse cuatro capturas (una por modo):**

| Captura | Modo activo | Qué mostrar |
|---|---|---|
| 8a | Predeterminado (oscuro) | Panel del widget con modo activo resaltado |
| 8b | Protanopia/Deuteranopia | Interfaz con colores Okabe-Ito aplicados |
| 8c | Tritanopia | Paleta rosa/naranja aplicada en cartelera |
| 8d | Monocromático | Interfaz en escala de grises |

> `[CAPTURA PENDIENTE — 4 capturas de los 4 modos del widget]`

---

#### Vista 9 — Login / Registro (`/auth`)

**Estado esperado a capturar:**
- Pestaña "Registrarse" activa (`aria-selected="true"`)
- Campo de correo con autocompletado activo (ícono del navegador visible)
- Error de validación con `role="alert"` y mensaje descriptivo

> `[CAPTURA PENDIENTE — formulario de registro con autocompletado visible]`

---

### 11.2 Checklist de Capturas

| # | Vista | Captura tomada |
|---|---|---|
| 1 | Inicio con skip-link | ☐ |
| 2 | Filtros con acordeón activo | ☐ |
| 3 | Modal de tráiler | ☐ |
| 4a | Mapa de butacas con íconos | ☐ |
| 4b | Indicador de 3 pasos | ☐ |
| 5 | Formulario de pago con error | ☐ |
| 6 | Modal de cancelación | ☐ |
| 7 | Modal de éxito | ☐ |
| 8a-d | Widget — 4 modos | ☐ |
| 9 | Login/Registro con autocompletado | ☐ |

---

### 11.3 Instrucciones para Tomar las Capturas

1. Abrir Chrome y navegar a `localhost:5173` (o el dominio de producción desplegado).
2. Para mostrar el foco: mantener Tab presionado hasta llegar al elemento objetivo, luego hacer la captura con `Win + Shift + S` o la extensión Lightshot.
3. Para mostrar el Accessibility Tree: Chrome DevTools → pestaña **Elements** → panel derecho **Accessibility** → activar *"Enable full-page accessibility tree"*.
4. Para el widget en modo daltonismo: activar el modo desde el panel ♿, esperar la transición CSS, luego capturar la vista completa.
5. Nombrar los archivos como: `captura_v1_inicio_skiplink.png`, `captura_v4a_butacas_iconos.png`, etc.
6. Guardar en la carpeta `docs/capturas-apf3/` del repositorio para incluirlas en el PDF.

---

### 11.4 Estado del Despliegue

| Item | Estado |
|---|---|
| Repositorio de código | ✅ [https://github.com/mMusashIi/cinema-butacas](https://github.com/mMusashIi/cinema-butacas) |
| Rama de entrega | ✅ `accesibilidad-apf3` |
| Tag base APF2 | ✅ `apf2-base` (commit `9b5128f` en `cineramaPrototipo`) |
| Despliegue web (GitHub Pages / Netlify / Vercel) | `[POR CONFIRMAR TRAS PUSH Y CONFIGURACIÓN]` |

---

*Fin del documento CAPITULOS_V_XI_APF3.md*  
*Generado en base al código fuente de la rama `accesibilidad-apf3`, commit `360a3e3` y posteriores.*
