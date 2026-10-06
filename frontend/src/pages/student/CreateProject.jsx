import { useNavigate, useParams } from 'react-router-dom';
import Card from '../../components/common/Card';
import ErrorState from '../../components/common/ErrorState';
import LoadingState from '../../components/common/LoadingState';
import PageHeader from '../../components/common/PageHeader';
import ProjectForm from '../../components/forms/ProjectForm';
import { useApi } from '../../hooks/useApi';
import { useToast } from '../../hooks/useToast';
import { projectService } from '../../services/projectService';

/** Handles both /student/projects/create and /student/projects/:id/edit. */
export default function CreateProject() {
  const { id } = useParams();
  const editing = Boolean(id);
  const navigate = useNavigate();
  const toast = useToast();
  const { data, loading, error, reload } = useApi(() => (editing ? projectService.get(id) : Promise.resolve(null)), [id]);

  const submit = async (values) => {
    if (editing) {
      const updated = await projectService.update(id, values);
      toast.success('Project updated successfully');
      navigate(`/student/projects/${updated.id}`);
    } else {
      const created = await projectService.create(values);
      toast.success('Project created successfully');
      navigate(`/student/projects/${created.id}`);
    }
  };

  const title = editing ? 'Edit project' : 'Create project';

  return (
    <>
      <PageHeader
        title={title}
        subtitle={editing ? 'Update your project details' : 'Add a project to your portfolio'}
        backTo={editing ? `/student/projects/${id}` : '/student/projects'}
        backLabel={editing ? 'Project' : 'My projects'}
      />
      {loading ? (
        <LoadingState label="Loading project…" />
      ) : error ? (
        <ErrorState title="Unable to load project" message={error.message} onRetry={error.status >= 500 ? reload : undefined} />
      ) : (
        <Card className="form-card">
          <ProjectForm
            key={data?.id ?? 'new'}
            initialValues={data}
            submitLabel={editing ? 'Save changes' : 'Create project'}
            onSubmit={submit}
            onCancel={() => navigate(-1)}
          />
        </Card>
      )}
    </>
  );
}
