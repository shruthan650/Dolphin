import { NavLink, useLocation } from 'react-router-dom';
import Icon from '../common/Icon';

const NAVIGATION = {
  STUDENT: [
    { to: '/student/dashboard', label: 'Home', icon: 'dashboard' },
    { to: '/student/class', label: 'My Class', icon: 'book' },
    { to: '/student/projects', label: 'Projects', icon: 'folder' },
    { to: '/student/leetcode', label: 'LeetCode', icon: 'code' },
  ],
  TEACHER: [
    { to: '/teacher/dashboard', label: 'Home', icon: 'dashboard' },
    { to: '/teacher/classes', label: 'Classes', icon: 'book' },
    { to: '/teacher/students', label: 'Students', icon: 'users' },
    { to: '/teacher/progress?tab=projects', label: 'Progress', icon: 'activity' },
  ],
  ADMIN: [
    { to: '/admin/dashboard', label: 'Home', icon: 'dashboard' },
    { to: '/admin/teachers', label: 'Teachers', icon: 'teacher' },
    { to: '/admin/students', label: 'Students', icon: 'users' },
    { to: '/admin/users', label: 'Users', icon: 'user' },
  ],
};

export default function MobileBottomNavigation({ role }) {
  const location = useLocation();
  const items = NAVIGATION[role] || [];

  return (
    <nav className="mobile-bottom-nav" aria-label="Mobile navigation">
      {items.map((item) => {
        const href = item.to.split('?')[0];
        const active = href === '/student/dashboard' || href.endsWith('/dashboard')
          ? location.pathname === href
          : location.pathname === href || location.pathname.startsWith(`${href}/`);
        return (
          <NavLink
            key={item.to}
            to={item.to}
            className={`mobile-nav-item ${active ? 'active' : ''}`}
            aria-current={active ? 'page' : undefined}
          >
            <Icon name={item.icon} size={20} />
            <span>{item.label}</span>
          </NavLink>
        );
      })}
    </nav>
  );
}
