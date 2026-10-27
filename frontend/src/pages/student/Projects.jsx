import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ProjectCard from '../../components/cards/ProjectCard';
import Button from '../../components/common/Button';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import LoadingState from '../../components/common/LoadingState';
import PageHeader from '../../components/common/PageHeader';
import PaginationControls from '../../components/common/PaginationControls';
import { useApi } from '../../hooks/useApi';
import { useToast } from '../../hooks/useToast';
import { usePagination } from '../../hooks/usePagination';
import { getErrorMessage } from '../../services/api';
import { projectService } from '../../services/projectService';
import { studentService } from '../../services/studentService';
import { sortByName } from '../../utils/sort';

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
      sortByName(
        (data || []).filter(
          (p) => classFilter === 'ALL' || (classFilter === 'NONE' ? !p.classId : p.classId === classFilter),
        ),
        (item) => item?.title,
      ),
    [data, classFilter],
  );
  const projectPagination = usePagination(filtered, classFilter);
  const classOptions = usePagination(sortByName(classes.data, (item) => item?.className));

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
                {classOptions.pageItems.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.className}
                  </option>
                ))}
                <option value="NONE">Unassigned</option>
              </select>
              <PaginationControls {...classOptions} onPageChange={classOptions.setPage} />
            </div>
          )}
          {filtered.length === 0 ? (
            <EmptyState icon="search" title="No projects in this class" />
          ) : (
            <div className="card-grid">
              {projectPagination.pageItems.map((p) => (
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
          {filtered.length > 0 && <PaginationControls {...projectPagination} onPageChange={projectPagination.setPage} />}
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
