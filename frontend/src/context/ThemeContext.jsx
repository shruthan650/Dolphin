import { createContext, useCallback, useEffect, useMemo, useState } from 'react';

export const ThemeContext = createContext(null);

/** Kept in sync with the inline script in index.html, which applies the theme before the first paint. */
export const THEME_KEY = 'dolphin.theme';

function readSavedTheme() {
  try {
    const saved = localStorage.getItem(THEME_KEY);
    return saved === 'light' || saved === 'dark' ? saved : null;
  } catch {
    return null;
  }
}

const systemPrefersDark = () => window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;

/**
 * Light/dark theme. An explicit choice is saved in localStorage (independent of the session, so it survives logout);
 * until the user chooses, the system preference is followed, including live changes to it.
 */
export function ThemeProvider({ children }) {
  const [saved, setSaved] = useState(readSavedTheme);
  const [systemDark, setSystemDark] = useState(systemPrefersDark);
  const theme = saved ?? (systemDark ? 'dark' : 'light');

  useEffect(() => {
    const query = window.matchMedia?.('(prefers-color-scheme: dark)');
    if (!query) return undefined;
    const onChange = (e) => setSystemDark(e.matches);
    query.addEventListener('change', onChange);
    return () => query.removeEventListener('change', onChange);
  }, []);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
  }, [theme]);

  const setTheme = useCallback((next) => {
    setSaved(next);
    try {
      localStorage.setItem(THEME_KEY, next);
    } catch {
      // Storage unavailable (private mode): the choice still applies for this visit.
    }
  }, []);

  const toggleTheme = useCallback(() => setTheme(theme === 'dark' ? 'light' : 'dark'), [theme, setTheme]);

  const value = useMemo(() => ({ theme, setTheme, toggleTheme }), [theme, setTheme, toggleTheme]);
  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}
