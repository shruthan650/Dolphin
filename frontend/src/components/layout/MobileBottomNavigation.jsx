import { NavLink } from 'react-router-dom';
import Icon from '../common/Icon';
import { NAVIGATION } from './navigation';

export default function MobileBottomNavigation({ role }) {
  const items = (NAVIGATION[role] || []).filter((item) => item.mobile !== false);

  return (
    <nav className="mobile-bottom-nav" aria-label="Mobile navigation">
      {items.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          className={({ isActive }) => `mobile-nav-item ${isActive ? 'active' : ''}`}
          aria-label={item.label}
          title={item.label}
        >
          <Icon name={item.icon} size={20} />
          <span>{item.mobileLabel || item.label}</span>
        </NavLink>
      ))}
    </nav>
  );
}
