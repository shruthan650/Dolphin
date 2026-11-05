import { Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import Sidebar from './Sidebar';
import Topbar from './Topbar';
import MobileBottomNavigation from './MobileBottomNavigation';
import MobileQuickActions from './MobileQuickActions';

export default function AppLayout() {
  const { user, role, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="app-shell">
      <Sidebar role={role} />
      <div className="app-main">
        <Topbar user={user} onLogout={handleLogout} />
        <main className="app-content" id="main">
          <Outlet />
        </main>
      </div>
      <MobileBottomNavigation role={role} />
      <MobileQuickActions role={role} />
    </div>
  );
}
