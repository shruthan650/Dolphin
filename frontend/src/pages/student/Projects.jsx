import { useState } from 'react';
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

export default function Projects() {
  const { data, setData, loading, error, reload } = useApi(projectService.mine);
  const [deleting, setDeleting] = useState(null);
  const [busy, setBusy] = useState(false);
  const toast = useToast();
  const navigate = useNavigate();

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
        <div className="card-grid">
          {data.map((p) => (
            <ProjectCard
              key={p.id}
              project={p}
              viewTo={`/student/projects/${p.id}`}
              onEdit={() => navigate(`/student/projects/${p.id}/edit`)}
              onDelete={() => setDeleting(p)}
            />
          ))}
        </div>
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
