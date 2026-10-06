import axios from 'axios';

export const TOKEN_KEY = 'dolphin.token';
export const USER_KEY = 'dolphin.user';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
});

let unauthorizedHandler = null;

/** AuthContext registers a callback so a 401 anywhere logs the user out. */
export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler;
}

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const isLoginCall = error.config?.url?.includes('/auth/login');
    if (status === 401 && !isLoginCall && unauthorizedHandler) {
      unauthorizedHandler(error.response?.data?.message);
    }
    return Promise.reject(error);
  },
);

/** Turns any axios error into a user-facing message. */
export function getErrorMessage(error, fallback = 'Something went wrong. Please try again.') {
  if (!error) return fallback;
  if (!error.response) {
    return 'Unable to reach the server. Check that the backend is running.';
  }
  const { status, data } = error.response;
  if (data?.message && data.message !== 'Validation failed') return data.message;
  if (data?.errors) return Object.values(data.errors)[0];
  switch (status) {
    case 401:
      return 'Your session has expired. Please sign in again.';
    case 403:
      return 'You do not have permission to do that.';
    case 404:
      return 'The requested item was not found.';
    case 409:
      return 'This conflicts with existing data.';
    case 500:
      return 'The server ran into a problem. Please try again.';
    default:
      return fallback;
  }
}

/** Field-level validation errors returned by the backend, keyed by field name. */
export function getFieldErrors(error) {
  return error?.response?.data?.errors || {};
}

export default api;
