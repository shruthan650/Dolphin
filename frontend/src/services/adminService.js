import api from './api';

export const adminService = {
  dashboard: () => api.get('/admin/dashboard').then((r) => r.data),
  teachers: () => api.get('/admin/teachers').then((r) => r.data),
  students: () => api.get('/admin/students').then((r) => r.data),
  users: () => api.get('/admin/users').then((r) => r.data),
  createTeacher: (payload) => api.post('/admin/teachers', payload).then((r) => r.data),
  setTeacherActive: (id, active) => api.patch(`/admin/teachers/${id}/status`, { active }).then((r) => r.data),
  deleteTeacher: (id) => api.delete(`/admin/teachers/${id}`),
};
