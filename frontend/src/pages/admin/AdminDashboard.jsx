import { useState } from 'react';
import StatCard from '../../components/cards/StatCard';
import Button from '../../components/common/Button';
import Card from '../../components/common/Card';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import LoadingState from '../../components/common/LoadingState';
import Modal from '../../components/common/Modal';
import PageHeader from '../../components/common/PageHeader';
import TeacherForm from '../../components/forms/TeacherForm';
import UsersTable from '../../components/tables/UsersTable';
import { useApi } from '../../hooks/useApi';
import { useToast } from '../../hooks/useToast';
import { adminService } from '../../services/adminService';

export default function AdminDashboard() {
  const { data, loading, error, reload } = useApi(adminService.dashboard);
  const [creating, setCreating] = useState(false);
  const toast = useToast();

  const createTeacher = async (values) => {
    const teacher = await adminService.createTeacher(values);
    toast.success(`Teacher created successfully: ${teacher.name}`);
    setCreating(false);
    reload();
  };

  return (
    <>
      <PageHeader
        title="Admin dashboard"
        subtitle="System-wide overview of Dolphin"
        actions={
          <Button icon="plus" onClick={() => setCreating(true)}>
            Create teacher
          </Button>
        }
      />

      {loading && !data ? (
        <LoadingState variant="cards" count={6} label="Loading dashboard…" />
      ) : error ? (
        <ErrorState title="Unable to load dashboard" message={error.message} onRetry={reload} />
      ) : (
        <>
          <div className="stat-grid">
            <StatCard label="Teachers" value={data.totalTeachers} icon="teacher" tone="indigo" hint={`${data.activeTeachers} active`} />
            <StatCard label="Students" value={data.totalStudents} icon="users" tone="teal" />
            <StatCard label="Classes" value={data.totalClasses} icon="book" tone="amber" />
            <StatCard label="Projects" value={data.totalProjects} icon="folder" tone="rose" />
            <StatCard label="LeetCode entries" value={data.totalLeetCodeEntries} icon="code" tone="slate" hint={`${data.problemsSolved} solved`} />
            <StatCard label="Total users" value={data.totalUsers} icon="user" tone="indigo" />
          </div>

          <Card title="Recently added users" subtitle="Newest accounts across all roles" padded={false}>
            {data.recentUsers.length === 0 ? (
              <EmptyState icon="users" title="No users yet" />
            ) : (
              <UsersTable users={data.recentUsers} showRole classLabel={null} />
            )}
          </Card>
        </>
      )}

      <Modal open={creating} title="Create teacher" description="The new account will have the Teacher role." onClose={() => setCreating(false)}>
        <TeacherForm onSubmit={createTeacher} onCancel={() => setCreating(false)} />
      </Modal>
    </>
  );
}
