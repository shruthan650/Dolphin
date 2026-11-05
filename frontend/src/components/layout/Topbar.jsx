import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import Icon from '../common/Icon';
import ThemeToggle from '../common/ThemeToggle';
import { initials } from '../../utils/format';
import { PROFILE_ROUTE } from './navigation';

export default function Topbar({ user, onLogout }) {
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef(null);
  const triggerRef = useRef(null);
  const profileRoute = PROFILE_ROUTE[user?.role];

  useEffect(() => {
    if (!menuOpen) return undefined;
    menuRef.current?.querySelector('.avatar-menu-item')?.focus();

    const closeOnPointerDown = (event) => {
      if (!menuRef.current?.contains(event.target)) setMenuOpen(false);
    };
    const closeOnEscape = (event) => {
      if (event.key === 'Escape') {
        setMenuOpen(false);
        triggerRef.current?.focus();
      }
    };
    document.addEventListener('pointerdown', closeOnPointerDown);
    document.addEventListener('keydown', closeOnEscape);
    return () => {
      document.removeEventListener('pointerdown', closeOnPointerDown);
      document.removeEventListener('keydown', closeOnEscape);
    };
  }, [menuOpen]);

  return (
    <header className="topbar">
      <div className="topbar-spacer" />
      <ThemeToggle />
      <div className="topbar-user">
        <div className="topbar-user-text">
          <span className="topbar-name">{user?.name}</span>
          <span className="topbar-email">{user?.email}</span>
        </div>
        <div className="avatar-menu" ref={menuRef}>
          <button
            ref={triggerRef}
            type="button"
            className="avatar-link"
            aria-label="Open account menu"
            aria-haspopup="menu"
            aria-expanded={menuOpen}
            aria-controls="account-menu"
            onClick={() => setMenuOpen((open) => !open)}
          >
            <span className="avatar" aria-hidden="true">{initials(user?.name)}</span>
          </button>
          {menuOpen && (
            <div
              id="account-menu"
              className="avatar-menu-dropdown"
              role="menu"
              aria-label="Account menu"
              onKeyDown={(event) => {
                const items = [...event.currentTarget.querySelectorAll('.avatar-menu-item')];
                const current = items.indexOf(document.activeElement);
                let next;
                if (event.key === 'ArrowDown') next = (current + 1) % items.length;
                else if (event.key === 'ArrowUp') next = (current - 1 + items.length) % items.length;
                else if (event.key === 'Home') next = 0;
                else if (event.key === 'End') next = items.length - 1;
                else return;
                event.preventDefault();
                items[next]?.focus();
              }}
            >
              <Link
                to={profileRoute}
                className="avatar-menu-item"
                role="menuitem"
                onClick={() => setMenuOpen(false)}
              >
                <Icon name="user" size={18} />
                <span>Profile</span>
              </Link>
              <button
                type="button"
                className="avatar-menu-item avatar-menu-logout"
                role="menuitem"
                onClick={() => {
                  setMenuOpen(false);
                  onLogout();
                }}
              >
                <Icon name="logout" size={18} />
                <span>Logout</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
