import { useState } from 'react';
import { Link } from 'react-router-dom';
import AdviceList from '../../components/cards/AdviceList';
import ClassCard from '../../components/cards/ClassCard';
import StatCard from '../../components/cards/StatCard';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import Card from '../../components/common/Card';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import LoadingState from '../../components/common/LoadingState';
import PageHeader from '../../components/common/PageHeader';
import JoinClassForm from '../../components/forms/JoinClassForm';
import LeaveClassDialog from '../../components/forms/LeaveClassDialog';
import { useApi } from '../../hooks/useApi';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { adviceService } from '../../services/adviceService';
import { studentService } from '../../services/studentService';
import { formatDate, timeAgo } from '../../utils/format';

export default function StudentDashboard() {
  const { user } = useAuth();
  const { data, loading, error, reload } = useApi(studentService.dashboard);
  const advice = useApi(adviceService.mine);
  const [leaving, setLeaving] = useState(null);
  const toast = useToast();

  // Leaving deletes that class's projects, LeetCode entries and advice, so every dashboard figure is refetched.
  const onLeft = () => {
    setLeaving(null);
    reload();
    advice.reload();
  };

  const onJoined = (result) => {
    toast.success(result.message);
    reload();
  };

  return (
    <>
      <PageHeader
        title={`Hi, ${user?.name?.split(' ')[0] ?? 'there'}`}
        subtitle="Keep track of your projects and problem-solving progress"
        actions={
          <>
            <Button variant="secondary" icon="code" to="/student/leetcode">
              Log problem
            </Button>
            <Button icon="plus" to="/student/projects/create">
              New project
            </Button>
          </>
        }
      />

      {loading && !data ? (
        <LoadingState variant="cards" label="Loading dashboard…" />
      ) : error ? (
        <ErrorState title="Unable to load dashboard" message={error.message} onRetry={reload} />
      ) : (
        <>
          {data.joinedClasses.length === 0 && (
            <Card title="Join your class" subtitle="Enter the class code your teacher shared with you" className="card-highlight">
              <JoinClassForm onJoined={onJoined} />
            </Card>
          )}

          <div className="stat-grid">
            <StatCard
              label="Class"
              value={data.joinedClasses.length === 0 ? 'Not joined' : data.joinedClasses[0].className}
              icon="book"
              tone="indigo"
              hint={data.joinedClasses.length > 1 ? `+${data.joinedClasses.length - 1} more` : data.joinedClasses[0]?.teacherName}
            />
            <StatCard label="Projects" value={data.projectCount} icon="folder" tone="amber" />
            <StatCard label="Problems solved" value={data.leetCodeStats.solved} icon="target" tone="teal" hint={`${data.leetCodeStats.total} logged`} />
            <StatCard
              label="In progress"
              value={data.leetCodeStats.inProgress + data.leetCodeStats.attempted}
              icon="activity"
              tone="rose"
              hint="Attempted or in progress"
            />
          </div>

          <Card title="Advice from your teachers" subtitle="Feedback on your projects and LeetCode problems">
            {advice.loading && !advice.data ? (
              <p className="muted">Loading advice…</p>
            ) : advice.error ? (
              <p className="muted">Unable to load advice right now.</p>
            ) : advice.data.length === 0 ? (
              <p className="muted">No advice yet. Your teachers can comment on your projects and LeetCode problems.</p>
            ) : (
              <AdviceList items={advice.data.slice(0, 5)} />
            )}
          </Card>

          <div className="grid-2">
            <Card title="Recent projects" actions={data.projectCount > 0 && <Button variant="ghost" size="sm" to="/student/projects">View all</Button>}>
              {data.recentProjects.length === 0 ? (
                <EmptyState
                  icon="folder"
                  title="No projects yet."
                  message="Create your first project."
                  action={<Button icon="plus" to="/student/projects/create">Create project</Button>}
                />
              ) : (
                <ul className="simple-list">
                  {data.recentProjects.map((p) => (
                    <li key={p.id}>
                      <Link to={`/student/projects/${p.id}`} className="simple-list-title">
                        {p.title}
                      </Link>
                      <span className="muted">{p.technologies.slice(0, 3).join(' · ') || 'No technologies listed'}</span>
                      <span className="simple-list-meta">{timeAgo(p.updatedAt)}</span>
                    </li>
                  ))}
                </ul>
              )}
            </Card>

            <Card title="Recent LeetCode activity" actions={data.leetCodeStats.total > 0 && <Button variant="ghost" size="sm" to="/student/leetcode">View all</Button>}>
              {data.recentLeetCode.length === 0 ? (
                <EmptyState
                  icon="code"
                  title="No problems logged yet."
                  message="Track the problems you solve to show your progress."
                  action={<Button icon="plus" to="/student/leetcode">Add problem</Button>}
                />
              ) : (
                <ul className="simple-list">
                  {data.recentLeetCode.map((e) => (
                    <li key={e.id}>
                      <span className="simple-list-title">{e.problemName}</span>
                      <span className="badge-row">
                        <Badge value={e.difficulty} />
                        <Badge value={e.status} />
                      </span>
                      <span className="simple-list-meta">{e.solvedAt ? formatDate(e.solvedAt) : timeAgo(e.updatedAt)}</span>
                    </li>
                  ))}
                </ul>
              )}
            </Card>
          </div>

          {data.joinedClasses.length > 0 && (
            <Card title="My classes" actions={<Button variant="ghost" size="sm" to="/student/class">Manage</Button>}>
              <div className="card-grid">
                {data.joinedClasses.map((cls) => (
                  <ClassCard key={cls.id} cls={cls} onLeave={setLeaving} />
                ))}
              </div>
            </Card>
          )}
        </>
      )}

      <LeaveClassDialog cls={leaving} onCancel={() => setLeaving(null)} onLeft={onLeft} />
    </>
  );
}
