import { useId, useState } from 'react';
import Icon from './Icon';

/**
 * Labelled form control with error/hint text.
 * as = 'input' | 'textarea' | 'select'
 */
export default function Input({ label, error, hint, as = 'input', className = '', children, required, type, ...props }) {
  const id = useId();
  const Control = as;
  const [passwordVisible, setPasswordVisible] = useState(false);
  const isPassword = as === 'input' && type === 'password';
  const describedBy = error ? `${id}-error` : hint ? `${id}-hint` : undefined;

  const control = (
    <Control
      id={id}
      className="field-control"
      aria-invalid={Boolean(error)}
      aria-describedby={describedBy}
      required={required}
      {...props}
      {...(type ? { type: isPassword && passwordVisible ? 'text' : type } : {})}
    >
      {children}
    </Control>
  );

  return (
    <div className={`field ${error ? 'field-invalid' : ''} ${className}`}>
      {label && (
        <label htmlFor={id} className="field-label">
          {label}
          {required && <span className="field-required" aria-hidden="true"> *</span>}
        </label>
      )}
      {isPassword ? (
        <div className="password-control">
          {control}
          <button
            type="button"
            className="password-toggle"
            aria-label={passwordVisible ? 'Hide password' : 'Show password'}
            aria-pressed={passwordVisible}
            title={passwordVisible ? 'Hide password' : 'Show password'}
            onMouseDown={(event) => event.preventDefault()}
            onClick={() => setPasswordVisible((visible) => !visible)}
          >
            <Icon name={passwordVisible ? 'eyeOff' : 'eye'} size={18} />
          </button>
        </div>
      ) : (
        control
      )}
      {error ? (
        <p id={`${id}-error`} className="field-error">
          {error}
        </p>
      ) : (
        hint && (
          <p id={`${id}-hint`} className="field-hint">
            {hint}
          </p>
        )
      )}
    </div>
  );
}
