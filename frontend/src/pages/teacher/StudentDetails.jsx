import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import AdviceList from '../../components/cards/AdviceList';
import ProjectCard from '../../components/cards/ProjectCard';
import StatCard from '../../components/cards/StatCard';
import Badge from '../../components/common/Badge';
import Icon from '../../components/common/Icon';
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
import { classService } from '../../services/classService';
import { teacherService } from '../../services/teacherService';
import { formatDate, initials } from '../../utils/format';

export default function StudentDetails() {
  const { id } = useParams();
  const { data, setData, loading, error, reload } = useApi(() => teacherService.student(id), [id]);
  const { user } = useAuth();
  const [removingFrom, setRemovingFrom] = useState(null);
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

  /** Removes the student from one of this teacher's classes; their data of that class is deleted. */
  const removeFromClass = async () => {
    setBusy(true);
    try {
      await classService.removeStudent(removingFrom.id, id);
      toast.success(`${data.name} removed from ${removingFrom.className}`);
      const remaining = data.classes.filter((c) => c.id !== removingFrom.id);
      setRemovingFrom(null);
      if (remaining.length === 0) navigate('/teacher/students', { replace: true });
      else reload();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
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
      <PageHeader title="Student profile" backTo="/teacher/students" backLabel="Students" />

      <div className="profile-header">
        <span className="avatar avatar-lg">{initials(data.name)}</span>
        <div className="profile-header-text">
          <h2 className="profile-name">
            {data.name} {!data.active && <Badge tone="red">Inactive</Badge>}
          </h2>
          <p className="muted break-anywhere">{data.email}</p>
          <ProfileLinks githubUrl={data.githubUrl} leetCodeUrl={data.leetCodeUrl} />
          <div className="chip-row">
            {data.classes.map((c) => (
              <span key={c.id} className="chip chip-removable">
                {c.className}
                <button
                  type="button"
                  aria-label={`Remove ${data.name} from ${c.className}`}
                  title={`Remove from ${c.className}`}
                  onClick={() => setRemovingFrom(c)}
                >
                  <Icon name="x" size={14} />
                </button>
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
              <ProjectCard key={p.id} project={p} classLabel={p.className ?? null} />
            ))}
          </div>
        )}
      </Card>

      <Card title="LeetCode progress" subtitle={`${stats.solved} of ${stats.total} problems solved`} padded={false}>
        {data.leetCodeEntries.length === 0 ? (
          <EmptyState icon="code" title="No problems logged" message={`${data.name} has not logged any LeetCode problems.`} />
        ) : (
          <LeetCodeTable entries={data.leetCodeEntries} classLabel={(e) => e.className} />
        )}
      </Card>

      <ConfirmDialog
        open={Boolean(removingFrom)}
        title="Remove student from class?"
        message={`${data.name} will be removed from ${removingFrom?.className}. Their projects, LeetCode records and advice in this class will be permanently deleted. Their account and their data in other classes are not affected.`}
        confirmLabel="Remove from class"
        loading={busy}
        onConfirm={removeFromClass}
        onCancel={() => setRemovingFrom(null)}
      />
    </>
  );
}
