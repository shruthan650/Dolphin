export const NAVIGATION = {
  ADMIN: [
    { to: '/admin/dashboard', label: 'Dashboard', mobileLabel: 'Home', icon: 'dashboard' },
    { to: '/admin/teachers', label: 'Teachers', icon: 'teacher' },
    { to: '/admin/students', label: 'Students', icon: 'users' },
    { to: '/admin/users', label: 'Users', icon: 'users' },
  ],
  TEACHER: [
    { to: '/teacher/dashboard', label: 'Dashboard', mobileLabel: 'Home', icon: 'dashboard' },
    { to: '/teacher/classes', label: 'My Classes', mobileLabel: 'Classes', icon: 'book' },
    { to: '/teacher/students', label: 'Students', icon: 'users' },
    { to: '/teacher/progress?tab=projects', label: 'Projects', icon: 'folder', match: 'projects', mobile: false },
    { to: '/teacher/progress?tab=leetcode', label: 'LeetCode', icon: 'code', match: 'leetcode', mobile: false },
  ],
  STUDENT: [
    { to: '/student/dashboard', label: 'Dashboard', mobileLabel: 'Home', icon: 'dashboard' },
    { to: '/student/class', label: 'My Class', mobileLabel: 'Class', icon: 'book' },
    { to: '/student/projects', label: 'Projects', icon: 'folder' },
    { to: '/student/leetcode', label: 'LeetCode', icon: 'code' },
  ],
};

export const PROFILE_ROUTE = {
  ADMIN: '/admin/profile',
  TEACHER: '/teacher/profile',
  STUDENT: '/student/profile',
};
