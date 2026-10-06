import { createContext, useCallback, useEffect, useMemo, useState } from 'react';
import { setUnauthorizedHandler, TOKEN_KEY, USER_KEY } from '../services/api';
import { authService } from '../services/authService';
import { studentService } from '../services/studentService';

export const AuthContext = createContext(null);

const toSessionUser = ({ id, name, email, role, githubUrl, leetCodeUrl }) => ({
  id,
  name,
  email,
  role,
  githubUrl: githubUrl ?? null,
  leetCodeUrl: leetCodeUrl ?? null,
});

function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY));
  } catch {
    return null;
  }
}

/**
 * Holds the session (JWT + basic user info). Only the session token is kept in localStorage;
 * all application data always comes from the backend.
 */
export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY));
  const [user, setUser] = useState(() => (localStorage.getItem(TOKEN_KEY) ? readStoredUser() : null));
  const [initializing, setInitializing] = useState(() => Boolean(localStorage.getItem(TOKEN_KEY)));
  const [sessionMessage, setSessionMessage] = useState(null);
  // True after the user clicks Logout: protected routes then must not remember the page for the next login.
  const [loggedOutManually, setLoggedOutManually] = useState(false);

  const clearSession = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setToken(null);
    setUser(null);
  }, []);

  const storeSession = useCallback((response) => {
    const sessionUser = toSessionUser({ ...response, id: response.userId });
    localStorage.setItem(TOKEN_KEY, response.token);
    localStorage.setItem(USER_KEY, JSON.stringify(sessionUser));
    setToken(response.token);
    setUser(sessionUser);
    setSessionMessage(null);
    setLoggedOutManually(false);
    return sessionUser;
  }, []);

  const login = useCallback(
    async (email, password) => storeSession(await authService.login(email, password)),
    [storeSession],
  );

  const register = useCallback(
    async (payload) => storeSession(await authService.register(payload)),
    [storeSession],
  );

  const updateProfileLinks = useCallback(async (links) => {
    const sessionUser = toSessionUser(await studentService.updateProfileLinks(links));
    localStorage.setItem(USER_KEY, JSON.stringify(sessionUser));
    setUser(sessionUser);
    return sessionUser;
  }, []);

  const logout = useCallback(() => {
    clearSession();
    setSessionMessage(null);
    setLoggedOutManually(true);
  }, [clearSession]);

  // Any 401 from the API (expired token, deactivated account) ends the session.
  useEffect(() => {
    setUnauthorizedHandler((message) => {
      clearSession();
      setSessionMessage(message || 'Your session has expired. Please sign in again.');
    });
    return () => setUnauthorizedHandler(null);
  }, [clearSession]);

  // Validate a stored token once on startup.
  useEffect(() => {
    if (!localStorage.getItem(TOKEN_KEY)) return;
    let cancelled = false;
    authService
      .me()
      .then((me) => {
        if (cancelled) return;
        const sessionUser = toSessionUser(me);
        localStorage.setItem(USER_KEY, JSON.stringify(sessionUser));
        setUser(sessionUser);
      })
      .catch((error) => {
        // Network errors keep the cached session; 401 is handled by the interceptor.
        if (!cancelled && error.response && error.response.status !== 401) clearSession();
      })
      .finally(() => !cancelled && setInitializing(false));
    return () => {
      cancelled = true;
    };
  }, [clearSession]);

  const value = useMemo(
    () => ({
      token,
      user,
      role: user?.role ?? null,
      isAuthenticated: Boolean(token && user),
      initializing,
      sessionMessage,
      loggedOutManually,
      login,
      register,
      updateProfileLinks,
      logout,
    }),
    [token, user, initializing, sessionMessage, loggedOutManually, login, register, updateProfileLinks, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
