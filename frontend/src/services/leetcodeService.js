import api from './api';

export const leetcodeService = {
  mine: () => api.get('/leetcode/my').then((r) => r.data),
  create: (payload) => api.post('/leetcode', payload).then((r) => r.data),
  update: (id, payload) => api.put(`/leetcode/${id}`, payload).then((r) => r.data),
  remove: (id) => api.delete(`/leetcode/${id}`),
};
