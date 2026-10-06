import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import AdviceList from '../../components/cards/AdviceList';
import Button from '../../components/common/Button';
import Card from '../../components/common/Card';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import ErrorState from '../../components/common/ErrorState';
import LoadingState from '../../components/common/LoadingState';
import PageHeader from '../../components/common/PageHeader';
import { useApi } from '../../hooks/useApi';
import { useToast } from '../../hooks/useToast';
import { adviceService } from '../../services/adviceService';
import { getErrorMessage } from '../../services/api';
import { projectService } from '../../services/projectService';
import { formatDate } from '../../utils/format';

export default function ProjectDetails() {
  const { id } = useParams();
  const { data, loading, error, reload } = useApi(() => projectService.get(id), [id]);
  const advice = useApi(adviceService.mine);
  const [confirming, setConfirming] = useState(false);
  const [busy, setBusy] = useState(false);
  const toast = useToast();
  const navigate = useNavigate();

  if (loading) return <LoadingState label="Loading project…" />;
  if (error) {
    return (
      <>
        <PageHeader title="Project" backTo="/student/projects" backLabel="My projects" />
        <ErrorState
          title={error.status === 404 ? 'Project not found' : error.status === 403 ? 'Access denied' : 'Unable to load project'}
          message={error.message}
          onRetry={error.status >= 500 || !error.status ? reload : undefined}
        />
      </>
    );
  }

  const remove = async () => {
    setBusy(true);
    try {
      await projectService.remove(id);
      toast.success('Project deleted');
      navigate('/student/projects', { replace: true });
    } catch (err) {
      toast.error(getErrorMessage(err));
      setBusy(false);
    }
  };

  return (
    <>
      <PageHeader
        title={data.title}
        subtitle={`Created ${formatDate(data.createdAt)} · Updated ${formatDate(data.updatedAt)}`}
        backTo="/student/projects"
        backLabel="My projects"
        actions={
          <>
            <Button variant="secondary" icon="edit" to={`/student/projects/${id}/edit`}>
              Edit
            </Button>
            <Button variant="danger" icon="trash" onClick={() => setConfirming(true)}>
              Delete
            </Button>
          </>
        }
      />

      <div className="grid-2 grid-2-wide">
        <Card title="About this project">
          {data.description ? <p className="prose">{data.description}</p> : <p className="muted">No description provided.</p>}
        </Card>
        <Card title="Details">
          <dl className="detail-list">
            <div>
              <dt>Technologies</dt>
              <dd>
                {data.technologies.length ? (
                  <ul className="tech-list">
                    {data.technologies.map((t) => (
                      <li key={t} className="tag">
                        {t}
                      </li>
                    ))}
                  </ul>
                ) : (
                  <span className="muted">None listed</span>
                )}
              </dd>
            </div>
            <div>
              <dt>Source code</dt>
              <dd>
                {data.githubUrl ? (
                  <a href={data.githubUrl} target="_blank" rel="noopener noreferrer" className="break">
                    {data.githubUrl}
                  </a>
                ) : (
                  <span className="muted">Not provided</span>
                )}
              </dd>
            </div>
            <div>
              <dt>Live demo</dt>
              <dd>
                {data.liveUrl ? (
                  <a href={data.liveUrl} target="_blank" rel="noopener noreferrer" className="break">
                    {data.liveUrl}
                  </a>
                ) : (
                  <span className="muted">Not provided</span>
                )}
              </dd>
            </div>
          </dl>
        </Card>
      </div>

      <Card title="Teacher advice" subtitle="What your teachers suggested for this project">
        {advice.loading && !advice.data ? (
          <p className="muted">Loading advice…</p>
        ) : advice.error ? (
          <p className="muted">Unable to load advice right now.</p>
        ) : advice.data.filter((a) => a.targetId === id).length === 0 ? (
          <p className="muted">No advice on this project yet.</p>
        ) : (
          <AdviceList items={advice.data.filter((a) => a.targetId === id)} showTarget={false} />
        )}
      </Card>

      <ConfirmDialog
        open={confirming}
        title="Delete project?"
        message={`"${data.title}" will be permanently deleted.`}
        confirmLabel="Delete"
        loading={busy}
        onConfirm={remove}
        onCancel={() => setConfirming(false)}
      />
    </>
  );
}
