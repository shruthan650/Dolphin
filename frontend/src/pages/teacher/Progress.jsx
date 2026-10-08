import { useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import ProjectCard from '../../components/cards/ProjectCard';
import Card from '../../components/common/Card';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import LoadingState from '../../components/common/LoadingState';
import PageHeader from '../../components/common/PageHeader';
import SearchInput from '../../components/common/SearchInput';
import LeetCodeTable from '../../components/tables/LeetCodeTable';
import { useApi } from '../../hooks/useApi';
import { teacherService } from '../../services/teacherService';

function ProjectsTab() {
  const { data, loading, error, reload } = useApi(teacherService.projects);
  const [query, setQuery] = useState('');
  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return (data || []).filter(
      (p) =>
        !q ||
        p.title.toLowerCase().includes(q) ||
        p.ownerName?.toLowerCase().includes(q) ||
        p.technologies.some((t) => t.toLowerCase().includes(q)),
    );
  }, [data, query]);

  if (loading) return <LoadingState label="Loading projects…" />;
  if (error) return <ErrorState title="Unable to load projects" message={error.message} onRetry={reload} />;
  if (data.length === 0) {
    return <EmptyState icon="folder" title="No projects yet" message="Projects created by students in your classes appear here." />;
  }
  return (
    <Card
      title={`${data.length} project${data.length === 1 ? '' : 's'}`}
      actions={<SearchInput value={query} onChange={setQuery} placeholder="Search title, student or tech" />}
    >
      {filtered.length === 0 ? (
        <EmptyState icon="search" title="No matching projects" />
      ) : (
        <div className="card-grid">
          {filtered.map((p) => (
            <ProjectCard key={p.id} project={p} showOwner classLabel={p.className ?? null} />
          ))}
        </div>
      )}
    </Card>
  );
}

function LeetCodeTab() {
  const { data, loading, error, reload } = useApi(teacherService.leetCode);
  const [difficulty, setDifficulty] = useState('ALL');
  const [status, setStatus] = useState('ALL');
  const filtered = useMemo(
    () =>
      (data || []).filter(
        (e) => (difficulty === 'ALL' || e.difficulty === difficulty) && (status === 'ALL' || e.status === status),
      ),
    [data, difficulty, status],
  );

  if (loading) return <LoadingState variant="table" label="Loading LeetCode progress…" />;
  if (error) return <ErrorState title="Unable to load LeetCode progress" message={error.message} onRetry={reload} />;
  if (data.length === 0) {
    return <EmptyState icon="code" title="No problems logged yet" message="LeetCode entries from students in your classes appear here." />;
  }
  return (
    <Card
      title={`${filtered.length} of ${data.length} entries`}
      actions={
        <div className="toolbar">
          <select className="field-control field-control-sm" value={difficulty} onChange={(e) => setDifficulty(e.target.value)} aria-label="Filter by difficulty">
            <option value="ALL">All difficulties</option>
            <option value="EASY">Easy</option>
            <option value="MEDIUM">Medium</option>
            <option value="HARD">Hard</option>
          </select>
          <select className="field-control field-control-sm" value={status} onChange={(e) => setStatus(e.target.value)} aria-label="Filter by status">
            <option value="ALL">All statuses</option>
            <option value="SOLVED">Solved</option>
            <option value="ATTEMPTED">Attempted</option>
            <option value="IN_PROGRESS">In progress</option>
          </select>
        </div>
      }
      padded={false}
    >
      {filtered.length === 0 ? <EmptyState icon="search" title="No matching entries" /> : <LeetCodeTable entries={filtered} showStudent classLabel={(e) => e.className} />}
    </Card>
  );
}

export default function Progress() {
  const [params, setParams] = useSearchParams();
  const tab = params.get('tab') === 'leetcode' ? 'leetcode' : 'projects';

  return (
    <>
      <PageHeader title="Student progress" subtitle="Projects and LeetCode activity across all of your classes" />
      <div className="segmented page-tabs" role="tablist" aria-label="Progress type">
        <button type="button" role="tab" aria-selected={tab === 'projects'} className={tab === 'projects' ? 'active' : ''} onClick={() => setParams({ tab: 'projects' })}>
          Projects
        </button>
        <button type="button" role="tab" aria-selected={tab === 'leetcode'} className={tab === 'leetcode' ? 'active' : ''} onClick={() => setParams({ tab: 'leetcode' })}>
          LeetCode
        </button>
      </div>
      {tab === 'projects' ? <ProjectsTab /> : <LeetCodeTab />}
    </>
  );
}
