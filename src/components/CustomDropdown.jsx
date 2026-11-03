import { useState, useRef, useEffect } from 'react';

export default function CustomDropdown({ label, placeholder, options, value, onChange }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);
  const dropdownId = `dd-${label.toLowerCase().replace(/[^a-z0-9]/g, '-')}`;

  useEffect(() => {
    const handler = (e) => {
      if (ref.current && !ref.current.contains(e.target)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const handleKeyDown = (e) => {
    if (e.key === 'Escape') {
      setOpen(false);
    }
  };

  const handleItemKeyDown = (e, opt) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      onChange(opt);
      setOpen(false);
    }
  };

  return (
    <div className="custom-dd-wrapper" ref={ref} onKeyDown={handleKeyDown}>
      <button 
        id={dropdownId}
        type="button"
        className={`custom-dd-btn ${open ? 'open' : ''}`} 
        onClick={() => setOpen(o => !o)}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-label={`${label}: ${value || placeholder}`}
      >
        <div className="custom-dd-inner">
          <span className="custom-dd-label">{label}</span>
          <span className="custom-dd-value">{value || placeholder}</span>
        </div>
        <svg 
          className={`custom-dd-chevron ${open ? 'rotated' : ''}`} 
          width="14" 
          height="14" 
          viewBox="0 0 24 24" 
          fill="none" 
          stroke="currentColor" 
          strokeWidth="2.5"
          aria-hidden="true"
        >
          <polyline points="6 9 12 15 18 9"/>
        </svg>
      </button>
      {open && (
        <ul 
          className="custom-dd-menu" 
          role="listbox" 
          aria-labelledby={dropdownId}
        >
          {value && (
            <li 
              role="option"
              aria-selected={false}
              tabIndex={0}
              className="custom-dd-item clear-item" 
              onClick={() => { onChange(null); setOpen(false); }}
              onKeyDown={(e) => handleItemKeyDown(e, null)}
            >
              — Todos —
            </li>
          )}
          {options.map(opt => (
            <li
              key={opt}
              role="option"
              aria-selected={value === opt}
              tabIndex={0}
              className={`custom-dd-item ${value === opt ? 'selected' : ''}`}
              onClick={() => { onChange(opt); setOpen(false); }}
              onKeyDown={(e) => handleItemKeyDown(e, opt)}
            >
              {opt}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
