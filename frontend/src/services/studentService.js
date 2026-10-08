import api from './api';

export const studentService = {
  dashboard: () => api.get('/student/dashboard').then((r) => r.data),
  classes: () => api.get('/student/classes').then((r) => r.data),
  /** Leaves a class; the student's projects and LeetCode entries of that class are permanently deleted. */
  leaveClass: (classId) => api.delete(`/student/classes/${classId}`),
  updateProfileLinks: (payload) => api.put('/student/profile-links', payload).then((r) => r.data),
};
