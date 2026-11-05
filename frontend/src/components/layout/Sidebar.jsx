import { NavLink, useLocation } from 'react-router-dom';
import Icon from '../common/Icon';
import { NAVIGATION } from './navigation';

export default function Sidebar({ role }) {
  const items = NAVIGATION[role] || [];
  const location = useLocation();
  const currentTab = new URLSearchParams(location.search).get('tab') || 'projects';

  return (
    <aside className="sidebar" aria-label="Main navigation">
      <div className="sidebar-brand">
        <img src="/favicon.svg" alt="" width="32" height="32" />
        <div>
          <span className="brand-name">Dolphin</span>
          <span className="brand-role">{role?.toLowerCase()} portal</span>
        </div>
      </div>

      <nav className="sidebar-nav">
        {items.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) => {
              const active = item.match
                ? location.pathname === '/teacher/progress' && currentTab === item.match
                : isActive;
              return `nav-item ${active ? 'active' : ''}`;
            }}
          >
            <Icon name={item.icon} size={18} />
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>

    </aside>
  );
}
