import { Navigate, Route, Routes } from 'react-router-dom';
import AppLayout from './components/layout/AppLayout';
import { useAuth } from './hooks/useAuth';
import ProtectedRoute from './routes/ProtectedRoute';
import RoleRoute from './routes/RoleRoute';
import { ROLE_HOME } from './utils/format';

import Login from './pages/auth/Login';
import NotFound from './pages/NotFound';
import AccountSettings from './pages/account/AccountSettings';

import AdminDashboard from './pages/admin/AdminDashboard';
import AdminTeachers from './pages/admin/Teachers';
import AdminStudents from './pages/admin/Students';
import AdminUsers from './pages/admin/Users';

import TeacherDashboard from './pages/teacher/TeacherDashboard';
import TeacherClasses from './pages/teacher/Classes';
import ClassDetails from './pages/teacher/ClassDetails';
import TeacherStudents from './pages/teacher/Students';
import StudentDetails from './pages/teacher/StudentDetails';
import Progress from './pages/teacher/Progress';

import StudentDashboard from './pages/student/StudentDashboard';
import Profile from './pages/student/Profile';
import MyClass from './pages/student/MyClass';
import Projects from './pages/student/Projects';
import CreateProject from './pages/student/CreateProject';
import ProjectDetails from './pages/student/ProjectDetails';
import LeetCode from './pages/student/LeetCode';

function HomeRedirect() {
  const { isAuthenticated, role, initializing } = useAuth();
  if (initializing) return null;
  return <Navigate to={isAuthenticated ? ROLE_HOME[role] : '/login'} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomeRedirect />} />
      <Route path="/login" element={<Login />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/admin" element={<RoleRoute role="ADMIN" />}>
            <Route index element={<Navigate to="dashboard" replace />} />
            <Route path="dashboard" element={<AdminDashboard />} />
            <Route path="teachers" element={<AdminTeachers />} />
            <Route path="students" element={<AdminStudents />} />
            <Route path="users" element={<AdminUsers />} />
            <Route path="profile" element={<AccountSettings />} />
          </Route>

          <Route path="/teacher" element={<RoleRoute role="TEACHER" />}>
            <Route index element={<Navigate to="dashboard" replace />} />
            <Route path="dashboard" element={<TeacherDashboard />} />
            <Route path="classes" element={<TeacherClasses />} />
            <Route path="classes/:id" element={<ClassDetails />} />
            <Route path="students" element={<TeacherStudents />} />
            <Route path="students/:id" element={<StudentDetails />} />
            <Route path="progress" element={<Progress />} />
            <Route path="profile" element={<AccountSettings />} />
          </Route>

          <Route path="/student" element={<RoleRoute role="STUDENT" />}>
            <Route index element={<Navigate to="dashboard" replace />} />
            <Route path="dashboard" element={<StudentDashboard />} />
            <Route path="profile" element={<Profile />} />
            <Route path="class" element={<MyClass />} />
            <Route path="projects" element={<Projects />} />
            <Route path="projects/create" element={<CreateProject />} />
            <Route path="projects/:id" element={<ProjectDetails />} />
            <Route path="projects/:id/edit" element={<CreateProject />} />
            <Route path="leetcode" element={<LeetCode />} />
          </Route>

          <Route path="*" element={<NotFound />} />
        </Route>
      </Route>
    </Routes>
  );
}
