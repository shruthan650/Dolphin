import { NavLink } from 'react-router-dom';
import Icon from '../common/Icon';

const NAV = {
  ADMIN: [
    { to: '/admin/dashboard', label: 'Dashboard', icon: 'dashboard' },
    { to: '/admin/teachers', label: 'Teachers', icon: 'teacher' },
    { to: '/admin/students', label: 'Students', icon: 'users' },
    { to: '/admin/users', label: 'Users', icon: 'users' },
    { to: '/admin/profile', label: 'Profile', icon: 'user' },
  ],
  TEACHER: [
    { to: '/teacher/dashboard', label: 'Dashboard', icon: 'dashboard' },
    { to: '/teacher/classes', label: 'My Classes', icon: 'book' },
    { to: '/teacher/students', label: 'Students', icon: 'users' },
    { to: '/teacher/progress?tab=projects', label: 'Projects', icon: 'folder', match: 'projects' },
    { to: '/teacher/progress?tab=leetcode', label: 'LeetCode', icon: 'code', match: 'leetcode' },
    { to: '/teacher/profile', label: 'Profile', icon: 'user' },
  ],
  STUDENT: [
    { to: '/student/dashboard', label: 'Dashboard', icon: 'dashboard' },
    { to: '/student/class', label: 'My Class', icon: 'book' },
    { to: '/student/projects', label: 'Projects', icon: 'folder' },
    { to: '/student/leetcode', label: 'LeetCode', icon: 'code' },
    { to: '/student/profile', label: 'Profile', icon: 'user' },
  ],
};

export default function Sidebar({ role, open, onClose, onLogout, location }) {
  const items = NAV[role] || [];
  const currentTab = new URLSearchParams(location.search).get('tab') || 'projects';

  return (
    <>
      <div className={`sidebar-overlay ${open ? 'visible' : ''}`} onClick={onClose} aria-hidden="true" />
      <aside className={`sidebar ${open ? 'open' : ''}`} aria-label="Main navigation">
        <div className="sidebar-brand">
          <img src="/favicon.svg" alt="" width="32" height="32" />
          <div>
            <span className="brand-name">Dolphin</span>
            <span className="brand-role">{role?.toLowerCase()} portal</span>
          </div>
          <button type="button" className="sidebar-close" onClick={onClose} aria-label="Close menu">
            <Icon name="x" size={18} />
          </button>
        </div>

        <nav className="sidebar-nav">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              onClick={onClose}
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

        <button type="button" className="nav-item nav-logout" onClick={onLogout}>
          <Icon name="logout" size={18} />
          <span>Logout</span>
        </button>
      </aside>
    </>
  );
}
