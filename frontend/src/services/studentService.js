import api from './api';

export const studentService = {
  dashboard: () => api.get('/student/dashboard').then((r) => r.data),
  classes: () => api.get('/student/classes').then((r) => r.data),
  /** Leaves a class; the student's projects and LeetCode entries of that class are permanently deleted. */
  leaveClass: (classId) => api.delete(`/student/classes/${classId}`),
  updateProfileLinks: (payload) => api.put('/student/profile-links', payload).then((r) => r.data),
  publicProfileVisibility: () => api.get('/student/public-profile-visibility').then((r) => r.data),
  updatePublicProfileVisibility: (isPublic) =>
    api.put('/student/public-profile-visibility', { isPublic }).then((r) => r.data),
  publicDirectory: (search, page, size = 12) =>
    api.get('/students/public', { params: { search, page, size } }).then((r) => r.data),
  publicProfile: (profileId) => api.get(`/students/${profileId}/public-profile`).then((r) => r.data),
};
