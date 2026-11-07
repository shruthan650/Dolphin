import { useParams } from 'react-router-dom';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import Card from '../../components/common/Card';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import LoadingState from '../../components/common/LoadingState';
import PageHeader from '../../components/common/PageHeader';
import LeetCodeStreakCard from '../../components/cards/LeetCodeStreakCard';
import { useApi } from '../../hooks/useApi';
import { studentService } from '../../services/studentService';
import { formatDate, initials } from '../../utils/format';

export default function PublicStudentProfile() {
  const { profileId } = useParams();
  const { data, loading, error, reload } = useApi(() => studentService.publicProfile(profileId), [profileId]);

  if (loading && !data) return <LoadingState label="Loading public profile…" />;
  if (error) return <ErrorState title="Unable to load public profile" message={error.message} onRetry={reload} />;

  const { leetCodeStats, projects } = data;
  const streak = {
    currentStreak: leetCodeStats.currentStreak,
    longestStreak: leetCodeStats.longestStreak,
    totalActiveDays: leetCodeStats.activeDays,
    mostRecentSolvedDate: leetCodeStats.lastSolvedDate,
    recentActivity: leetCodeStats.recentActivity,
  };

  return (
    <>
      <PageHeader
        title={data.displayName}
        subtitle="Public student profile"
        backTo="/student/students"
        backLabel="Student directory"
      />
      <section className="profile-header">
        <span className="avatar avatar-lg" aria-hidden="true">{initials(data.displayName)}</span>
        <div className="profile-header-text">
          <h2 className="profile-name">{data.displayName}</h2>
          <div className="chip-row"><Badge value="Student" /></div>
        </div>
        {data.joinedAt && <p className="profile-meta muted">Member since {formatDate(data.joinedAt)}</p>}
      </section>

      <Card title="Coding statistics">
        <dl className="public-coding-stats">
          <div><dt>Total solved</dt><dd>{leetCodeStats.totalSolved}</dd></div>
          <div><dt>Easy</dt><dd>{leetCodeStats.easySolved}</dd></div>
          <div><dt>Medium</dt><dd>{leetCodeStats.mediumSolved}</dd></div>
          <div><dt>Hard</dt><dd>{leetCodeStats.hardSolved}</dd></div>
          <div><dt>Current streak</dt><dd>{leetCodeStats.currentStreak} days</dd></div>
          <div><dt>Longest streak</dt><dd>{leetCodeStats.longestStreak} days</dd></div>
        </dl>
      </Card>

      <LeetCodeStreakCard streak={streak} />

      <Card title="Public projects" subtitle="Only projects individually marked public are shown.">
        {projects.length === 0 ? (
          <EmptyState icon="folder" title="No public projects" message="This student has not shared any projects yet." />
        ) : (
          <div className="public-project-grid">
            {projects.map((project, index) => (
              <article className="public-project" key={`${project.title}-${index}`}>
                <h3>{project.title}</h3>
                {project.description && <p>{project.description}</p>}
                {project.technologies.length > 0 && (
                  <div className="chip-row" aria-label="Technologies">
                    {project.technologies.map((technology) => <span className="chip" key={technology}>{technology}</span>)}
                  </div>
                )}
                <div className="public-project-links">
                  {project.githubUrl && (
                    <Button variant="outline" size="sm" href={project.githubUrl} icon="external">GitHub</Button>
                  )}
                  {project.liveUrl && (
                    <Button variant="outline" size="sm" href={project.liveUrl} icon="external">Live project</Button>
                  )}
                </div>
              </article>
            ))}
          </div>
        )}
      </Card>
    </>
  );
}
