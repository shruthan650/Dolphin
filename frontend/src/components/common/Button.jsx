import { Link } from 'react-router-dom';
import Icon from './Icon';

/**
 * @param {'primary'|'secondary'|'ghost'|'danger'} variant
 * @param {'md'|'sm'} size
 * @param {string} to render as a router link
 */
export default function Button({
  variant = 'primary',
  size = 'md',
  icon,
  loading = false,
  disabled,
  to,
  href,
  className = '',
  children,
  type = 'button',
  ...rest
}) {
  const classes = `btn btn-${variant} btn-${size} ${!children ? 'btn-icon' : ''} ${className}`;
  const content = (
    <>
      {loading ? <span className="spinner spinner-sm" aria-hidden="true" /> : icon && <Icon name={icon} size={size === 'sm' ? 16 : 18} />}
      {children && <span>{children}</span>}
    </>
  );

  if (to) {
    return (
      <Link to={to} className={classes} {...rest}>
        {content}
      </Link>
    );
  }
  if (href) {
    return (
      <a href={href} className={classes} target="_blank" rel="noopener noreferrer" {...rest}>
        {content}
      </a>
    );
  }
  return (
    <button type={type} className={classes} disabled={disabled || loading} aria-busy={loading} {...rest}>
      {content}
    </button>
  );
}
