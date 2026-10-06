import api from './api';

export const studentService = {
  dashboard: () => api.get('/student/dashboard').then((r) => r.data),
  classes: () => api.get('/student/classes').then((r) => r.data),
  updateProfileLinks: (payload) => api.put('/student/profile-links', payload).then((r) => r.data),
};
