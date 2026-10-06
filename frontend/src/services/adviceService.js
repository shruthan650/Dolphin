import api from './api';

export const adviceService = {
  /** Teacher: { targetType: 'PROJECT' | 'LEETCODE', targetId, message } */
  give: (payload) => api.post('/advice', payload).then((r) => r.data),
  remove: (id) => api.delete(`/advice/${id}`),
  /** Student: all advice given to the signed-in student, newest first. */
  mine: () => api.get('/advice/my').then((r) => r.data),
};
