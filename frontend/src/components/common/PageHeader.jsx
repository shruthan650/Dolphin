import { Link } from 'react-router-dom';
import Icon from './Icon';

export default function PageHeader({ title, subtitle, actions, backTo, backLabel = 'Back' }) {
  return (
    <div className="page-header">
      <div className="page-header-text">
        {backTo && (
          <Link to={backTo} className="back-link">
            <Icon name="arrowLeft" size={16} />
            {backLabel}
          </Link>
        )}
        <h1 className="page-title">{title}</h1>
        {subtitle && <p className="page-subtitle">{subtitle}</p>}
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </div>
  );
}
