import { useId } from 'react';

/**
 * Labelled form control with error/hint text.
 * as = 'input' | 'textarea' | 'select'
 */
export default function Input({ label, error, hint, as = 'input', className = '', children, required, ...props }) {
  const id = useId();
  const Control = as;
  const describedBy = error ? `${id}-error` : hint ? `${id}-hint` : undefined;
  return (
    <div className={`field ${error ? 'field-invalid' : ''} ${className}`}>
      {label && (
        <label htmlFor={id} className="field-label">
          {label}
          {required && <span className="field-required" aria-hidden="true"> *</span>}
        </label>
      )}
      <Control
        id={id}
        className="field-control"
        aria-invalid={Boolean(error)}
        aria-describedby={describedBy}
        required={required}
        {...props}
      >
        {children}
      </Control>
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
