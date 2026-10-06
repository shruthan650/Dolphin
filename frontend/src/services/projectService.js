import api from './api';

export const projectService = {
  mine: () => api.get('/projects/my').then((r) => r.data),
  get: (id) => api.get(`/projects/${id}`).then((r) => r.data),
  create: (payload) => api.post('/projects', payload).then((r) => r.data),
  update: (id, payload) => api.put(`/projects/${id}`, payload).then((r) => r.data),
  remove: (id) => api.delete(`/projects/${id}`),
};
