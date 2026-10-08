import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ProjectCard from '../../components/cards/ProjectCard';
import Button from '../../components/common/Button';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import LoadingState from '../../components/common/LoadingState';
import PageHeader from '../../components/common/PageHeader';
import { useApi } from '../../hooks/useApi';
import { useToast } from '../../hooks/useToast';
import { getErrorMessage } from '../../services/api';
import { projectService } from '../../services/projectService';
import { studentService } from '../../services/studentService';

export default function Projects() {
  const { data, setData, loading, error, reload } = useApi(projectService.mine);
  const classes = useApi(studentService.classes);
  const [classFilter, setClassFilter] = useState('ALL');
  const [deleting, setDeleting] = useState(null);
  const [busy, setBusy] = useState(false);
  const toast = useToast();
  const navigate = useNavigate();

  const classNames = useMemo(
    () => Object.fromEntries((classes.data || []).map((c) => [c.id, c.className])),
    [classes.data],
  );
  const filtered = useMemo(
    () =>
      (data || []).filter(
        (p) => classFilter === 'ALL' || (classFilter === 'NONE' ? !p.classId : p.classId === classFilter),
      ),
    [data, classFilter],
  );

  const confirmDelete = async () => {
    setBusy(true);
    try {
      await projectService.remove(deleting.id);
      setData((current) => current.filter((p) => p.id !== deleting.id));
      toast.success(`"${deleting.title}" deleted`);
      setDeleting(null);
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <PageHeader
        title="My projects"
        subtitle="Showcase what you've built"
        actions={
          <Button icon="plus" to="/student/projects/create">
            Create project
          </Button>
        }
      />

      {loading ? (
        <LoadingState label="Loading projects…" />
      ) : error ? (
        <ErrorState title="Unable to load projects." message={error.message} onRetry={reload} />
      ) : data.length === 0 ? (
        <EmptyState
          icon="folder"
          title="No projects yet."
          message="Create your first project."
          action={
            <Button icon="plus" to="/student/projects/create">
              Create project
            </Button>
          }
        />
      ) : (
        <>
          {(classes.data?.length > 1 || data.some((p) => !p.classId)) && (
            <div className="toolbar">
              <select
                className="field-control field-control-sm"
                value={classFilter}
                onChange={(e) => setClassFilter(e.target.value)}
                aria-label="Filter by class"
              >
                <option value="ALL">All classes</option>
                {(classes.data || []).map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.className}
                  </option>
                ))}
                <option value="NONE">Unassigned</option>
              </select>
            </div>
          )}
          {filtered.length === 0 ? (
            <EmptyState icon="search" title="No projects in this class" />
          ) : (
            <div className="card-grid">
              {filtered.map((p) => (
                <ProjectCard
                  key={p.id}
                  project={p}
                  classLabel={classNames[p.classId] ?? null}
                  viewTo={`/student/projects/${p.id}`}
                  onEdit={() => navigate(`/student/projects/${p.id}/edit`)}
                  onDelete={() => setDeleting(p)}
                />
              ))}
            </div>
          )}
        </>
      )}

      <ConfirmDialog
        open={Boolean(deleting)}
        title="Delete project?"
        message={`"${deleting?.title}" will be permanently deleted.`}
        confirmLabel="Delete"
        loading={busy}
        onConfirm={confirmDelete}
        onCancel={() => setDeleting(null)}
      />
    </>
  );
}
