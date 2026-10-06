import { useCallback, useEffect, useRef, useState } from 'react';
import { getErrorMessage } from '../services/api';

/**
 * Runs an async loader and tracks loading / error / data so every page renders
 * consistent loading, error and empty states.
 *
 * @param {() => Promise<any>} loader
 * @param {any[]} deps re-run when these change
 */
export function useApi(loader, deps = []) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const loaderRef = useRef(loader);
  loaderRef.current = loader;

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setData(await loaderRef.current());
    } catch (err) {
      setError({ message: getErrorMessage(err), status: err.response?.status });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  return { data, setData, loading, error, reload: load };
}
