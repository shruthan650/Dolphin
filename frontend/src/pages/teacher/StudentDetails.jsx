import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import AdviceList from '../../components/cards/AdviceList';
import ProjectCard from '../../components/cards/ProjectCard';
import StatCard from '../../components/cards/StatCard';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import Card from '../../components/common/Card';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import LoadingState from '../../components/common/LoadingState';
import PageHeader from '../../components/common/PageHeader';
import ProfileLinks from '../../components/common/ProfileLinks';
import AdviceForm from '../../components/forms/AdviceForm';
import LeetCodeTable from '../../components/tables/LeetCodeTable';
import { useApi } from '../../hooks/useApi';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { getErrorMessage } from '../../services/api';
import { adviceService } from '../../services/adviceService';
import { teacherService } from '../../services/teacherService';
import { formatDate, initials } from '../../utils/format';

export default function StudentDetails() {
  const { id } = useParams();
  const { data, setData, loading, error, reload } = useApi(() => teacherService.student(id), [id]);
  const { user } = useAuth();
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [busy, setBusy] = useState(false);
  const navigate = useNavigate();
  const toast = useToast();

  const giveAdvice = async (payload) => {
    const created = await adviceService.give(payload);
    setData((current) => ({ ...current, advice: [created, ...current.advice] }));
    toast.success('Advice sent');
  };

  const deleteAdvice = async (item) => {
    try {
      await adviceService.remove(item.id);
      setData((current) => ({ ...current, advice: current.advice.filter((a) => a.id !== item.id) }));
      toast.success('Advice deleted');
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  const deleteStudent = async () => {
    setBusy(true);
    try {
      await teacherService.deleteStudent(id);
      toast.success(`${data.name} deleted`);
      navigate('/teacher/students', { replace: true });
    } catch (err) {
      toast.error(getErrorMessage(err));
      setBusy(false);
    }
  };

  if (loading) return <LoadingState label="Loading student…" />;
  if (error) {
    return (
      <>
        <PageHeader title="Student" backTo="/teacher/students" backLabel="Students" />
        <ErrorState
          title={error.status === 403 ? 'Access denied' : 'Unable to load student'}
          message={error.message}
          onRetry={error.status === 403 ? undefined : reload}
        />
      </>
    );
  }

  const stats = data.leetCodeStats;

  return (
    <>
      <PageHeader
        title="Student profile"
        backTo="/teacher/students"
        backLabel="Students"
        actions={
          <Button variant="danger" icon="trash" onClick={() => setConfirmDelete(true)}>
            Delete student
          </Button>
        }
      />

      <div className="profile-header">
        <span className="avatar avatar-lg">{initials(data.name)}</span>
        <div>
          <h2 className="profile-name">
            {data.name} {!data.active && <Badge tone="red">Inactive</Badge>}
          </h2>
          <p className="muted">{data.email}</p>
          <ProfileLinks githubUrl={data.githubUrl} leetCodeUrl={data.leetCodeUrl} />
          <div className="chip-row">
            {data.classes.map((c) => (
              <span key={c.id} className="chip">
                {c.className}
              </span>
            ))}
          </div>
        </div>
        <p className="profile-meta muted">Joined Dolphin {formatDate(data.joinedAt)}</p>
      </div>

      <div className="stat-grid">
        <StatCard label="Projects" value={data.projects.length} icon="folder" tone="indigo" />
        <StatCard label="Problems solved" value={stats.solved} icon="target" tone="teal" hint={`${stats.total} logged`} />
        <StatCard label="In progress / attempted" value={stats.inProgress + stats.attempted} icon="activity" tone="amber" />
        <StatCard
          label="Easy · Medium · Hard"
          value={`${stats.easySolved} · ${stats.mediumSolved} · ${stats.hardSolved}`}
          icon="code"
          tone="rose"
          hint="Solved by difficulty"
        />
      </div>

      <Card
        title="Advice"
        subtitle={`Guide ${data.name} on their projects and daily problem-solving. They see it on their dashboard.`}
      >
        {data.projects.length === 0 && data.leetCodeEntries.length === 0 ? (
          <p className="muted">You can give advice once {data.name} adds a project or logs a LeetCode problem.</p>
        ) : (
          <AdviceForm projects={data.projects} entries={data.leetCodeEntries} onSubmit={giveAdvice} />
        )}
        {data.advice.length > 0 && (
          <>
            <hr className="advice-divider" />
            <AdviceList items={data.advice} canDelete={(a) => a.teacherId === user?.id} onDelete={deleteAdvice} />
          </>
        )}
      </Card>

      <Card title="Projects" subtitle={`${data.projects.length} project${data.projects.length === 1 ? '' : 's'}`}>
        {data.projects.length === 0 ? (
          <EmptyState icon="folder" title="No projects yet" message={`${data.name} has not added any projects.`} />
        ) : (
          <div className="card-grid">
            {data.projects.map((p) => (
              <ProjectCard key={p.id} project={p} />
            ))}
          </div>
        )}
      </Card>

      <Card title="LeetCode progress" subtitle={`${stats.solved} of ${stats.total} problems solved`} padded={false}>
        {data.leetCodeEntries.length === 0 ? (
          <EmptyState icon="code" title="No problems logged" message={`${data.name} has not logged any LeetCode problems.`} />
        ) : (
          <LeetCodeTable entries={data.leetCodeEntries} />
        )}
      </Card>

      <ConfirmDialog
        open={confirmDelete}
        title="Delete student?"
        message={`${data.name}'s account will be permanently deleted, together with their projects, LeetCode entries and enrolment in every class. This cannot be undone.`}
        confirmLabel="Delete student"
        loading={busy}
        onConfirm={deleteStudent}
        onCancel={() => setConfirmDelete(false)}
      />
    </>
  );
}
