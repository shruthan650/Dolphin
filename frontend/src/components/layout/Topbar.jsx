import { Link } from 'react-router-dom';
import Icon from '../common/Icon';
import ThemeToggle from '../common/ThemeToggle';
import { initials } from '../../utils/format';

const PROFILE_ROUTE = {
  ADMIN: '/admin/profile',
  TEACHER: '/teacher/profile',
  STUDENT: '/student/profile',
};

export default function Topbar({ user, onMenu }) {
  const profileRoute = PROFILE_ROUTE[user?.role] || '/login';

  return (
    <header className="topbar">
      <button type="button" className="topbar-menu" onClick={onMenu} aria-label="Open menu">
        <Icon name="menu" size={20} />
      </button>
      <div className="topbar-spacer" />
      <ThemeToggle />
      <div className="topbar-user">
        <div className="topbar-user-text">
          <span className="topbar-name">{user?.name}</span>
          <span className="topbar-email">{user?.email}</span>
        </div>
        <Link to={profileRoute} className="avatar-link" aria-label="Open profile">
          <span className="avatar" aria-hidden="true">{initials(user?.name)}</span>
        </Link>
      </div>
    </header>
  );
}
