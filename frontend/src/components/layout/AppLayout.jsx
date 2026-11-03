import { useRef, useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import Sidebar from './Sidebar';
import Topbar from './Topbar';
import MobileBottomNavigation from './MobileBottomNavigation';
import MobileQuickActions from './MobileQuickActions';
import { getStudentSwipeDestination, isSwipeExcludedTarget } from '../../utils/mobileSwipeNavigation';

export default function AppLayout() {
  const { user, role, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const touchStart = useRef(null);

  const handleTouchStart = (event) => {
    if (
      role !== 'STUDENT' ||
      !window.matchMedia('(max-width: 760px)').matches ||
      event.touches.length !== 1 ||
      isSwipeExcludedTarget(event.target)
    ) {
      touchStart.current = null;
      return;
    }
    const { clientX, clientY } = event.touches[0];
    touchStart.current = { clientX, clientY };
  };

  const handleTouchEnd = (event) => {
    const start = touchStart.current;
    touchStart.current = null;
    if (!start || event.changedTouches.length !== 1 || isSwipeExcludedTarget(event.target)) return;
    const touch = event.changedTouches[0];
    const destination = getStudentSwipeDestination(
      location.pathname,
      touch.clientX - start.clientX,
      touch.clientY - start.clientY,
    );
    if (destination) navigate(destination);
  };

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
        <Topbar user={user} onMenu={() => setMenuOpen(true)} />
        <main
          className="app-content"
          id="main"
          onTouchStart={handleTouchStart}
          onTouchEnd={handleTouchEnd}
          onTouchCancel={() => {
            touchStart.current = null;
          }}
        >
          <Outlet />
        </main>
      </div>
      <MobileBottomNavigation role={role} />
      <MobileQuickActions role={role} />
    </div>
  );
}
