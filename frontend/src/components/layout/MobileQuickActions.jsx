import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Icon from '../common/Icon';

const ACTIONS = {
  STUDENT: [
    { label: 'Log Problem', icon: 'code', to: '/student/leetcode', state: { mobileAction: 'log-problem' } },
    { label: 'Add Project', icon: 'folder', to: '/student/projects/create' },
  ],
  TEACHER: [
    { label: 'Add Class', icon: 'book', to: '/teacher/classes', state: { mobileAction: 'add-class' } },
  ],
  ADMIN: [
    { label: 'Add Teacher', icon: 'teacher', to: '/admin/teachers', state: { mobileAction: 'add-teacher' } },
  ],
};

export default function MobileQuickActions({ role }) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef(null);
  const fabRef = useRef(null);
  const navigate = useNavigate();
  const actions = ACTIONS[role] || [];

  useEffect(() => {
    if (!open) return undefined;
    const onPointerDown = (event) => {
      if (!rootRef.current?.contains(event.target)) setOpen(false);
    };
    const onKeyDown = (event) => {
      if (event.key === 'Escape') {
        setOpen(false);
        fabRef.current?.focus();
      }
    };
    document.addEventListener('pointerdown', onPointerDown);
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('pointerdown', onPointerDown);
      document.removeEventListener('keydown', onKeyDown);
    };
  }, [open]);

  const chooseAction = (action) => {
    setOpen(false);
    navigate(action.to, action.state ? { state: action.state } : undefined);
  };

  if (!actions.length) return null;

  return (
    <div className={`mobile-quick-actions ${open ? 'open' : ''}`} ref={rootRef}>
      <button
        ref={fabRef}
        type="button"
        className="mobile-fab"
        aria-label={open ? 'Close quick actions' : 'Open quick actions'}
        aria-expanded={open}
        aria-controls="mobile-quick-actions-menu"
        onClick={() => setOpen((value) => !value)}
      >
        <Icon name="plus" size={26} />
      </button>
      {open && (
        <div id="mobile-quick-actions-menu" className="mobile-action-menu" role="menu" aria-label="Quick actions">
          {actions.map((action) => (
            <button
              type="button"
              key={action.label}
              className="mobile-action-item"
              role="menuitem"
              onClick={() => chooseAction(action)}
            >
              <Icon name={action.icon} size={18} />
              <span>{action.label}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
