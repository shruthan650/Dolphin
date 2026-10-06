import { Outlet } from 'react-router-dom';
import Button from '../components/common/Button';
import ErrorState from '../components/common/ErrorState';
import { useAuth } from '../hooks/useAuth';
import { ROLE_HOME } from '../utils/format';

/**
 * Shows an "Access denied" screen to users of other roles. UX only; the backend
 * authorizes every request by role and ownership.
 */
export default function RoleRoute({ role }) {
  const { role: currentRole } = useAuth();
  if (currentRole !== role) {
    return (
      <div className="access-denied">
        <ErrorState
          title="Access denied"
          message={`This area is only available to ${role.toLowerCase()} accounts.`}
        />
        <Button to={ROLE_HOME[currentRole] || '/login'}>Go to my dashboard</Button>
      </div>
    );
  }
  return <Outlet />;
}
