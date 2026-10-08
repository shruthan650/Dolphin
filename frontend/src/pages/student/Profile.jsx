import Badge from '../../components/common/Badge';
import Card from '../../components/common/Card';
import ErrorState from '../../components/common/ErrorState';
import LoadingState from '../../components/common/LoadingState';
import PageHeader from '../../components/common/PageHeader';
import ProfileLinks from '../../components/common/ProfileLinks';
import ProfileLinksForm from '../../components/forms/ProfileLinksForm';
import { AccountSections } from '../account/AccountSettings';
import { useApi } from '../../hooks/useApi';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { authService } from '../../services/authService';
import { studentService } from '../../services/studentService';
import { formatDate, initials } from '../../utils/format';

async function loadProfile() {
  const [me, dashboard] = await Promise.all([authService.me(), studentService.dashboard()]);
  return { me, dashboard };
}

export default function Profile() {
  const { data, loading, error, reload } = useApi(loadProfile);
  const toast = useToast();
  const { user } = useAuth();

  if (loading) return <LoadingState label="Loading profile…" />;
  if (error) return <ErrorState title="Unable to load profile" message={error.message} onRetry={reload} />;

  const { me, dashboard } = data;
  const stats = dashboard.leetCodeStats;

  return (
    <>
      <PageHeader title="Profile" subtitle="Your account and progress summary" />

      <div className="profile-header">
        <span className="avatar avatar-lg">{initials(user?.name ?? me.name)}</span>
        <div className="profile-header-text">
          {/* The session user reflects name/email changes made below without reloading the page. */}
          <h2 className="profile-name">{user?.name ?? me.name}</h2>
          <p className="muted break-anywhere">{user?.email ?? me.email}</p>
          <ProfileLinks githubUrl={me.githubUrl} leetCodeUrl={me.leetCodeUrl} />
          <div className="chip-row">
            <Badge value={me.role} />
            <Badge tone={me.active ? 'green' : 'red'}>{me.active ? 'Active' : 'Inactive'}</Badge>
          </div>
        </div>
        <p className="profile-meta muted">Member since {formatDate(me.createdAt)}</p>
      </div>

      <div className="grid-2">
        <Card title="Classes">
          {dashboard.joinedClasses.length === 0 ? (
            <p className="muted">You have not joined a class yet.</p>
          ) : (
            <ul className="simple-list">
              {dashboard.joinedClasses.map((c) => (
                <li key={c.id}>
                  <span className="simple-list-title">{c.className}</span>
                  <span className="muted">
                    Semester {c.semester} · {c.branch} · Section {c.section}
                  </span>
                  <span className="simple-list-meta">{c.teacherName}</span>
                </li>
              ))}
            </ul>
          )}
        </Card>
        <Card title="Progress summary">
          <dl className="detail-list detail-list-inline">
            <div>
              <dt>Projects</dt>
              <dd>{dashboard.projectCount}</dd>
            </div>
            <div>
              <dt>Problems logged</dt>
              <dd>{stats.total}</dd>
            </div>
            <div>
              <dt>Solved</dt>
              <dd>{stats.solved}</dd>
            </div>
            <div>
              <dt>Easy / Medium / Hard solved</dt>
              <dd>
                {stats.easySolved} / {stats.mediumSolved} / {stats.hardSolved}
              </dd>
            </div>
          </dl>
        </Card>
      </div>

      <Card title="Coding profiles" subtitle="Your teachers can see these links">
        <ProfileLinksForm
          key={`${me.githubUrl}|${me.leetCodeUrl}`}
          initialValues={me}
          onSaved={() => {
            toast.success('Coding profiles updated');
            reload();
          }}
        />
      </Card>

      <AccountSections />
    </>
  );
}
