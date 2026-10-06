import Icon from '../common/Icon';
import { initials } from '../../utils/format';

export default function Topbar({ user, onMenu }) {
  return (
    <header className="topbar">
      <button type="button" className="topbar-menu" onClick={onMenu} aria-label="Open menu">
        <Icon name="menu" size={20} />
      </button>
      <div className="topbar-spacer" />
      <div className="topbar-user">
        <div className="topbar-user-text">
          <span className="topbar-name">{user?.name}</span>
          <span className="topbar-email">{user?.email}</span>
        </div>
        <span className="avatar" aria-hidden="true">
          {initials(user?.name)}
        </span>
      </div>
    </header>
  );
}
