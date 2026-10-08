import api from './api';

export const teacherService = {
  dashboard: () => api.get('/teacher/dashboard').then((r) => r.data),
  students: () => api.get('/teacher/students').then((r) => r.data),
  student: (id) => api.get(`/teacher/students/${id}`).then((r) => r.data),
  studentProjects: (id) => api.get(`/teacher/students/${id}/projects`).then((r) => r.data),
  studentLeetCode: (id) => api.get(`/teacher/students/${id}/leetcode`).then((r) => r.data),
  projects: () => api.get('/teacher/projects').then((r) => r.data),
  leetCode: () => api.get('/teacher/leetcode').then((r) => r.data),
};
