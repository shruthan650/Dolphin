import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import LoadingState from '../components/common/LoadingState';
import { needsProfileLinks } from '../utils/validation';

/** Requires a signed-in user. UX only; the backend enforces authentication on every request. */
export default function ProtectedRoute() {
  const { isAuthenticated, user, initializing, loggedOutManually } = useAuth();
  const location = useLocation();

  if (initializing) {
    return (
      <div className="fullscreen-center">
        <LoadingState label="Checking your session…" />
      </div>
    );
  }
  if (!isAuthenticated) {
    // Remember the page only when the session ended involuntarily (expired token, deep link), never after an
    // explicit logout — otherwise the next person to sign in would be sent to the previous user's page.
    return <Navigate to="/login" replace state={loggedOutManually ? null : { from: location }} />;
  }
  if (needsProfileLinks(user)) {
    // Students must add their GitHub/LeetCode profiles (on the login page) before using the app.
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  return <Outlet />;
}
