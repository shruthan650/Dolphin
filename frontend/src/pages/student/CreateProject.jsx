import { useNavigate, useParams } from 'react-router-dom';
import Button from '../../components/common/Button';
import Card from '../../components/common/Card';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import LoadingState from '../../components/common/LoadingState';
import PageHeader from '../../components/common/PageHeader';
import ProjectForm from '../../components/forms/ProjectForm';
import { useApi } from '../../hooks/useApi';
import { useToast } from '../../hooks/useToast';
import { projectService } from '../../services/projectService';
import { studentService } from '../../services/studentService';

/** Handles both /student/projects/create and /student/projects/:id/edit. */
export default function CreateProject() {
  const { id } = useParams();
  const editing = Boolean(id);
  const navigate = useNavigate();
  const toast = useToast();
  const { data, loading, error, reload } = useApi(async () => {
    const [project, classes] = await Promise.all([
      editing ? projectService.get(id) : Promise.resolve(null),
      studentService.classes(),
    ]);
    return { project, classes };
  }, [id]);

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
      ) : data.classes.length === 0 ? (
        <EmptyState
          icon="book"
          title="Join a class first"
          message="Every project belongs to one of your classes. Join a class with the code your teacher gave you."
          action={<Button to="/student/class">Join a class</Button>}
        />
      ) : (
        <Card className="form-card">
          <ProjectForm
            key={data.project?.id ?? 'new'}
            initialValues={data.project}
            classes={data.classes}
            submitLabel={editing ? 'Save changes' : 'Create project'}
            onSubmit={submit}
            onCancel={() => navigate(-1)}
          />
        </Card>
      )}
    </>
  );
}
