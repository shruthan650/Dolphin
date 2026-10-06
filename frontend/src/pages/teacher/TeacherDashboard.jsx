import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ActivityList from '../../components/cards/ActivityList';
import ClassCard from '../../components/cards/ClassCard';
import StatCard from '../../components/cards/StatCard';
import Button from '../../components/common/Button';
import Card from '../../components/common/Card';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import LoadingState from '../../components/common/LoadingState';
import Modal from '../../components/common/Modal';
import PageHeader from '../../components/common/PageHeader';
import ClassForm from '../../components/forms/ClassForm';
import { useApi } from '../../hooks/useApi';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { classService } from '../../services/classService';
import { teacherService } from '../../services/teacherService';

async function loadDashboard() {
  const [stats, classes] = await Promise.all([teacherService.dashboard(), classService.mine()]);
  return { stats, classes };
}

export default function TeacherDashboard() {
  const { user } = useAuth();
  const { data, loading, error, reload } = useApi(loadDashboard);
  const [creating, setCreating] = useState(false);
  const toast = useToast();
  const navigate = useNavigate();

  const createClass = async (values) => {
    const created = await classService.create(values);
    toast.success(`Class created successfully. Share code ${created.classCode} with your students.`);
    setCreating(false);
    navigate(`/teacher/classes/${created.id}`);
  };

  return (
    <>
      <PageHeader
        title={`Welcome, ${user?.name?.split(' ')[0] ?? 'Teacher'}`}
        subtitle="Here's how your students are progressing"
        actions={
          <Button icon="plus" onClick={() => setCreating(true)}>
            Create class
          </Button>
        }
      />

      {loading && !data ? (
        <LoadingState variant="cards" label="Loading dashboard…" />
      ) : error ? (
        <ErrorState title="Unable to load dashboard" message={error.message} onRetry={reload} />
      ) : (
        <>
          <div className="stat-grid">
            <StatCard label="Classes" value={data.stats.totalClasses} icon="book" tone="indigo" />
            <StatCard label="Students" value={data.stats.totalStudents} icon="users" tone="teal" />
            <StatCard label="Projects" value={data.stats.totalProjects} icon="folder" tone="amber" />
            <StatCard
              label="Problems solved"
              value={data.stats.problemsSolved}
              icon="target"
              tone="rose"
              hint={`${data.stats.totalLeetCodeEntries} entries logged`}
            />
          </div>

          <div className="grid-2">
            <Card
              title="My classes"
              actions={data.classes.length > 0 && <Button variant="ghost" size="sm" to="/teacher/classes">View all</Button>}
            >
              {data.classes.length === 0 ? (
                <EmptyState
                  icon="book"
                  title="You haven't created any classes yet."
                  message="Create a class to get started."
                  action={
                    <Button icon="plus" onClick={() => setCreating(true)}>
                      Create class
                    </Button>
                  }
                />
              ) : (
                <div className="stack">
                  {data.classes.slice(0, 3).map((cls) => (
                    <ClassCard key={cls.id} cls={cls} to={`/teacher/classes/${cls.id}`} />
                  ))}
                </div>
              )}
            </Card>

            <Card title="Recent student activity" subtitle="Latest projects and LeetCode updates">
              {data.stats.recentActivity.length === 0 ? (
                <EmptyState
                  icon="activity"
                  title="No activity yet"
                  message={
                    data.stats.totalStudents === 0
                      ? 'Once students join your classes, their progress shows up here.'
                      : 'Your students have not added projects or problems yet.'
                  }
                />
              ) : (
                <ActivityList items={data.stats.recentActivity} />
              )}
            </Card>
          </div>
        </>
      )}

      <Modal open={creating} title="Create class" description="A unique class code is generated automatically." onClose={() => setCreating(false)}>
        <ClassForm onSubmit={createClass} onCancel={() => setCreating(false)} />
      </Modal>
    </>
  );
}
