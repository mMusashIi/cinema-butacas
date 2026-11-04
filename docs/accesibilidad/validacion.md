# Validación de Accesibilidad — Cinerama (APF3)

Este documento registra los métodos de evaluación, los criterios analizados y los resultados obtenidos tras la implementación de las mejoras de accesibilidad en el prototipo web.

---

## 1. Procedimiento de verificación

Para validar el prototipo actualizado se aplicaron los siguientes métodos y herramientas:
1. **Inspección de código estático y semántica HTML5 / WAI-ARIA:** verificación de landmarks (`header`, `nav`, `main`, `aside`, `footer`), roles ARIA, estados (`aria-expanded`, `aria-pressed`, `aria-selected`, `aria-current`), atributos `lang` y textos alternativos `alt`.
2. **Navegación manual exclusiva por teclado:** prueba del flujo completo sin ratón (Inicio → Ver detalle de película → Elegir horario → Selección de butacas → Formulario de pago → Modal de confirmación) empleando las teclas `Tab`, `Shift+Tab`, `Enter`, `Espacio` y `Escape`.
3. **Prueba de compilación de producción:** ejecución de `npm run build` con Vite para garantizar la integridad funcional y sintáctica del proyecto.
4. **Verificación de responsividad y foco visible:** revisión en emulación de pantalla de escritorio y móvil con foco visible mediante contorno azul de alto contraste.

---

## 2. Criterios evaluados

- **WCAG 1.1.1 Contenido no textual:** textos alternativos descriptivos en carteles, fotografías y nombres accesibles en botones e iconos sociales.
- **WCAG 1.3.1 Información y relaciones:** jerarquía correcta de encabezados, landmarks y estructura semántica.
- **WCAG 1.3.5 Identificación del propósito de la entrada:** autocompletado en campos de inicio de sesión y registro.
- **WCAG 2.1.1 Teclado:** operabilidad total sin mouse en enlaces, filtros, acordeones, modales y botones.
- **WCAG 2.4.3 Orden del foco:** preservación de una secuencia lógica de lectura y transferencia automática del foco al modal de éxito.
- **WCAG 2.4.7 Foco visible:** indicador `:focus-visible` de alto contraste en controles interactivos.
- **WCAG 2.5.3 Etiqueta en el nombre:** concordancia entre las etiquetas visibles y los nombres accesibles (`aria-label`).
- **WCAG 3.1.1 Idioma de la página:** atributo `lang="es"` en el elemento raíz `<html>`.
- **WCAG 4.1.2 Nombre, función, valor:** roles correctos (`dialog`, `listbox`, `option`, `tab`, `tablist`).
- **WCAG 4.1.3 Mensajes de estado:** anuncios en vivo (`aria-live="polite"`) en resultados de filtrado y totales de compra.

---

## 3. Matriz de resultados cuantitativos y cualitativos

| Criterio | Antes (`apf2-base`) | Después (`accesibilidad-apf3`) | Resultado |
|---|---|---|---|
| **Puntaje Lighthouse (Accesibilidad)** | `[POR CONFIRMAR]` | `[POR CONFIRMAR]` | `[POR CONFIRMAR]` |
| **Errores detectados por axe / WAVE** | `[POR CONFIRMAR]` | `[POR CONFIRMAR]` | `[POR CONFIRMAR]` |
| **Contraste del texto principal** | `[POR CONFIRMAR]` | `[POR CONFIRMAR]` | `[POR CONFIRMAR]` |
| **Navegación completa solo con teclado** | **Bloqueada** (filtros, tarjetas de cartelera, sedes y modal no recibían foco) | **100% operable** en el flujo completo de exploración y compra | **Cumple** |
| **Tamaño de los asientos (px)** | 36 × 36 px (escritorio), 32 × 32 px (móvil) | 36 × 36 px (escritorio), 32 × 32 px (móvil) | **Cumple WCAG 2.2 AA (mín. 24px)** |
| **Comprensión del mensaje de error** | `[POR CONFIRMAR]` | `[POR CONFIRMAR]` | `[POR CONFIRMAR]` |
| **Tiempo de la tarea "comprar un boleto"** | `[POR CONFIRMAR]` | `[POR CONFIRMAR]` | `[POR CONFIRMAR]` |

---

## 4. Instrucciones para mediciones que requieren intervención humana

Para completar las métricas pendientes marcadas como `[POR CONFIRMAR]`, el equipo debe realizar lo siguiente:

1. **Medición con Lighthouse:**
   - Iniciar la aplicación con `npm run dev` en Chrome.
   - Abrir DevTools (`F12`), seleccionar la pestaña **Lighthouse**, desmarcar todas las opciones excepto **Accessibility** y ejecutar el análisis en la versión base (`git checkout apf2-base`) y en la versión final (`git checkout accesibilidad-apf3`).
   - Guardar las capturas de pantalla en `docs/accesibilidad/capturas/`.

2. **Medición de contraste (Colour Contrast Analyser / DevTools):**
   - Utilizar el cuentagotas de Chrome DevTools para medir el contraste del texto sobre el fondo burdeos de la página y registrar el valor numérico (ej. `7.2:1`).

3. **Prueba de usuario con cronómetro (Tiempo de tarea y comprensión):**
   - Pedir a un compañero que complete la tarea: *"Comprar 2 entradas para la película en cartelera"*.
   - Medir el tiempo en segundos desde la página de inicio hasta el modal de compra exitosa.
   - Aplicar una breve pregunta de escala Likert (1 al 5) sobre la claridad de los mensajes de error.
