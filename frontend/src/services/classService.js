import api from './api';

export const classService = {
  create: (payload) => api.post('/classes', payload).then((r) => r.data),
  mine: () => api.get('/classes/mine').then((r) => r.data),
  details: (id) => api.get(`/classes/${id}`).then((r) => r.data),
  update: (id, payload) => api.put(`/classes/${id}`, payload).then((r) => r.data),
  remove: (id) => api.delete(`/classes/${id}`),
  removeStudent: (classId, studentId) => api.delete(`/classes/${classId}/students/${studentId}`),
  join: (classCode) => api.post(`/classes/join/${encodeURIComponent(classCode.trim())}`).then((r) => r.data),
};
