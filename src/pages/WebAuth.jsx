import { useState, useRef } from 'react';
import './WebPages.css';
import { useNavigate } from 'react-router-dom';
import { usePageTitle } from '../hooks/usePageTitle';

export default function WebAuth() {
  const [isLogin, setIsLogin] = useState(true);
  const navigate = useNavigate();
  usePageTitle(isLogin ? 'Iniciar Sesión' : 'Registrarse');

  const [formData, setFormData] = useState({
    firstname: '',
    lastname: '',
    email: '',
    password: '',
    passwordConfirm: ''
  });
  const [touched, setTouched] = useState({});
  const [submitted, setSubmitted] = useState(false);

  const loginTabRef = useRef(null);
  const registerTabRef = useRef(null);

  const errors = {};
  if (!formData.email) {
    errors.email = 'El correo electrónico es requerido.';
  } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
    errors.email = 'Ingresa un correo electrónico válido.';
  }

  if (!formData.password) {
    errors.password = 'La contraseña es requerida.';
  } else if (formData.password.length < 6) {
    errors.password = 'La contraseña debe tener al menos 6 caracteres.';
  }

  if (!isLogin) {
    if (!formData.firstname.trim()) {
      errors.firstname = 'Ingresa tu nombre.';
    }
    if (!formData.lastname.trim()) {
      errors.lastname = 'Ingresa tu apellido.';
    }
    if (formData.password !== formData.passwordConfirm) {
      errors.passwordConfirm = 'Las contraseñas no coinciden.';
    }
  }

  const markTouched = (field) => setTouched(prev => ({ ...prev, [field]: true }));

  const handleChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleTabKeyDown = (e) => {
    if (e.key === 'ArrowRight' || e.key === 'ArrowLeft') {
      e.preventDefault();
      const nextIsLogin = !isLogin;
      setIsLogin(nextIsLogin);
      setTimeout(() => {
        if (nextIsLogin) {
          loginTabRef.current?.focus();
        } else {
          registerTabRef.current?.focus();
        }
      }, 0);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    setSubmitted(true);
    if (Object.keys(errors).length > 0) {
      return;
    }
    if (isLogin) {
      navigate('/dashboard'); 
    } else {
      navigate('/WebHome');
    }
  };

  const showError = (field) => (touched[field] || submitted) && errors[field];

  return (
    <div className="web-page-center">
      <div className="auth-card">
        <div className="auth-tabs" role="tablist" aria-label="Opciones de autenticación">
          <button 
            ref={loginTabRef}
            role="tab"
            id="tab-login"
            aria-selected={isLogin}
            aria-controls="panel-auth"
            tabIndex={isLogin ? 0 : -1}
            className={`auth-tab ${isLogin ? 'active' : ''}`} 
            onClick={() => setIsLogin(true)}
            onKeyDown={handleTabKeyDown}
            type="button"
          >
            Iniciar Sesión
          </button>
          <button 
            ref={registerTabRef}
            role="tab"
            id="tab-register"
            aria-selected={!isLogin}
            aria-controls="panel-auth"
            tabIndex={!isLogin ? 0 : -1}
            className={`auth-tab ${!isLogin ? 'active' : ''}`} 
            onClick={() => setIsLogin(false)}
            onKeyDown={handleTabKeyDown}
            type="button"
          >
            Registrarse
          </button>
        </div>

        <form
          id="panel-auth"
          role="tabpanel"
          aria-labelledby={isLogin ? 'tab-login' : 'tab-register'}
          className="auth-form"
          onSubmit={handleSubmit}
          noValidate
        >
          {!isLogin && (
            <div className="form-row">
              <div className="form-group">
                <label htmlFor="auth-firstname">Nombre</label>
                <input
                  id="auth-firstname"
                  type="text"
                  autoComplete="given-name"
                  placeholder="Tu nombre"
                  value={formData.firstname}
                  onChange={e => handleChange('firstname', e.target.value)}
                  onBlur={() => markTouched('firstname')}
                  aria-invalid={Boolean(showError('firstname'))}
                  aria-describedby={showError('firstname') ? 'error-firstname' : undefined}
                  required
                />
                {showError('firstname') && (
                  <span id="error-firstname" className="field-error" role="alert">
                    {errors.firstname}
                  </span>
                )}
              </div>
              <div className="form-group">
                <label htmlFor="auth-lastname">Apellido</label>
                <input
                  id="auth-lastname"
                  type="text"
                  autoComplete="family-name"
                  placeholder="Tu apellido"
                  value={formData.lastname}
                  onChange={e => handleChange('lastname', e.target.value)}
                  onBlur={() => markTouched('lastname')}
                  aria-invalid={Boolean(showError('lastname'))}
                  aria-describedby={showError('lastname') ? 'error-lastname' : undefined}
                  required
                />
                {showError('lastname') && (
                  <span id="error-lastname" className="field-error" role="alert">
                    {errors.lastname}
                  </span>
                )}
              </div>
            </div>
          )}

          <div className="form-group">
            <label htmlFor="auth-email">Correo Electrónico</label>
            <input
              id="auth-email"
              type="email"
              autoComplete="email"
              placeholder="correo@ejemplo.com"
              value={formData.email}
              onChange={e => handleChange('email', e.target.value)}
              onBlur={() => markTouched('email')}
              aria-invalid={Boolean(showError('email'))}
              aria-describedby={showError('email') ? 'error-email' : undefined}
              required
            />
            {showError('email') && (
              <span id="error-email" className="field-error" role="alert">
                {errors.email}
              </span>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="auth-password">Contraseña</label>
            <input
              id="auth-password"
              type="password"
              autoComplete={isLogin ? 'current-password' : 'new-password'}
              placeholder="••••••••"
              value={formData.password}
              onChange={e => handleChange('password', e.target.value)}
              onBlur={() => markTouched('password')}
              aria-invalid={Boolean(showError('password'))}
              aria-describedby={showError('password') ? 'error-password' : undefined}
              required
            />
            {showError('password') && (
              <span id="error-password" className="field-error" role="alert">
                {errors.password}
              </span>
            )}
          </div>

          {!isLogin && (
            <div className="form-group">
              <label htmlFor="auth-password-confirm">Confirmar Contraseña</label>
              <input
                id="auth-password-confirm"
                type="password"
                autoComplete="new-password"
                placeholder="••••••••"
                value={formData.passwordConfirm}
                onChange={e => handleChange('passwordConfirm', e.target.value)}
                onBlur={() => markTouched('passwordConfirm')}
                aria-invalid={Boolean(showError('passwordConfirm'))}
                aria-describedby={showError('passwordConfirm') ? 'error-password-confirm' : undefined}
                required
              />
              {showError('passwordConfirm') && (
                <span id="error-password-confirm" className="field-error" role="alert">
                  {errors.passwordConfirm}
                </span>
              )}
            </div>
          )}

          <button type="submit" className="btn-auth">
            {isLogin ? 'Iniciar Sesión' : 'Registrarse'}
          </button>
        </form>
      </div>
    </div>
  );
}
