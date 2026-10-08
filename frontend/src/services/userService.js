import api from './api';

/** The signed-in user's own account (every role). */
export const userService = {
  me: () => api.get('/users/me').then((r) => r.data),
  updateProfile: (payload) => api.put('/users/me', payload).then((r) => r.data),
  /** Returns a new session (token); older tokens stop working. */
  changeEmail: (payload) => api.put('/users/me/email', payload).then((r) => r.data),
  /** Returns a new session (token); every other session is signed out. */
  changePassword: (payload) => api.patch('/users/me/password', payload).then((r) => r.data),
  deleteAccount: (payload) => api.delete('/users/me', { data: payload }),
};
