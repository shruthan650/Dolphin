import Button from '../components/common/Button';
import EmptyState from '../components/common/EmptyState';
import { useAuth } from '../hooks/useAuth';
import { ROLE_HOME } from '../utils/format';

export default function NotFound() {
  const { role } = useAuth();
  return (
    <EmptyState
      icon="alert"
      title="Page not found"
      message="The page you are looking for does not exist or you do not have access to it."
      action={<Button to={ROLE_HOME[role] || '/login'}>Go to dashboard</Button>}
    />
  );
}
