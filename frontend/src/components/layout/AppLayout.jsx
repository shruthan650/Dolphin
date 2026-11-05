import { useEffect, useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import Sidebar from './Sidebar';
import Topbar from './Topbar';
import MobileBottomNavigation from './MobileBottomNavigation';
import MobileQuickActions from './MobileQuickActions';

export default function AppLayout() {
  const { user, role, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    if (!menuOpen) return undefined;
    const closeOnEscape = (event) => {
      if (event.key === 'Escape') setMenuOpen(false);
    };
    window.addEventListener('keydown', closeOnEscape);
    return () => window.removeEventListener('keydown', closeOnEscape);
  }, [menuOpen]);

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="app-shell">
      <Sidebar
        role={role}
        open={menuOpen}
        onClose={() => setMenuOpen(false)}
        onLogout={handleLogout}
        location={location}
      />
      <div className="app-main">
        <Topbar user={user} onMenu={() => setMenuOpen(true)} menuOpen={menuOpen} />
        <main className="app-content" id="main">
          <Outlet />
        </main>
      </div>
      <MobileBottomNavigation role={role} />
      <MobileQuickActions role={role} />
    </div>
  );
}
