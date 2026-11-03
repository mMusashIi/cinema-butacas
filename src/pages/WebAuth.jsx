import { useState } from 'react';
import './WebPages.css';
import { useNavigate } from 'react-router-dom';

export default function WebAuth() {
  const [isLogin, setIsLogin] = useState(true);
  const navigate = useNavigate();

  const handleSubmit = (e) => {
    e.preventDefault();
    if (isLogin) {
      navigate('/dashboard'); 
    } else {
      navigate('/web');
    }
  };

  return (
    <div className="web-page-center">
      <div className="auth-card">
        <div className="auth-tabs" role="tablist" aria-label="Opciones de autenticación">
          <button 
            type="button"
            role="tab"
            aria-selected={isLogin}
            className={`auth-tab ${isLogin ? 'active' : ''}`} 
            onClick={() => setIsLogin(true)}
          >
            Iniciar Sesión
          </button>
          <button 
            type="button"
            role="tab"
            aria-selected={!isLogin}
            className={`auth-tab ${!isLogin ? 'active' : ''}`} 
            onClick={() => setIsLogin(false)}
          >
            Registrarse
          </button>
        </div>

        <form 
          className="auth-form" 
          onSubmit={handleSubmit}
          aria-label={isLogin ? 'Formulario de inicio de sesión' : 'Formulario de registro'}
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
                  required 
                />
              </div>
              <div className="form-group">
                <label htmlFor="auth-lastname">Apellido</label>
                <input 
                  id="auth-lastname" 
                  type="text" 
                  autoComplete="family-name" 
                  placeholder="Tu apellido" 
                  required 
                />
              </div>
            </div>
          )}

          <div className="form-group">
            <label htmlFor="auth-email">Correo electrónico</label>
            <input 
              id="auth-email" 
              type="email" 
              autoComplete="email" 
              placeholder="correo@ejemplo.com" 
              required 
            />
          </div>

          <div className="form-group">
            <label htmlFor="auth-password">Contraseña</label>
            <input 
              id="auth-password" 
              type="password" 
              autoComplete={isLogin ? 'current-password' : 'new-password'} 
              placeholder="••••••••" 
              required 
            />
          </div>

          {!isLogin && (
            <div className="form-group">
              <label htmlFor="auth-password-confirm">Confirmar contraseña</label>
              <input 
                id="auth-password-confirm" 
                type="password" 
                autoComplete="new-password" 
                placeholder="••••••••" 
                required 
              />
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
