import { useState } from 'react';
import { Link } from 'react-router-dom';
import Card from '../../components/common/Card';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import Icon from '../../components/common/Icon';
import Input from '../../components/common/Input';
import LoadingState from '../../components/common/LoadingState';
import PageHeader from '../../components/common/PageHeader';
import Button from '../../components/common/Button';
import PaginationControls from '../../components/common/PaginationControls';
import { useApi } from '../../hooks/useApi';
import { studentService } from '../../services/studentService';
import { initials } from '../../utils/format';

const PAGE_SIZE = 12;

export default function PublicStudents() {
  const [search, setSearch] = useState('');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const { data, loading, error, reload } = useApi(
    () => studentService.publicDirectory(query, page, PAGE_SIZE),
    [query, page],
  );

  const submitSearch = (event) => {
    event.preventDefault();
    setPage(0);
    setQuery(search.trim());
  };

  const pageCount = data?.totalPages ?? 0;

  return (
    <>
      <PageHeader title="Student directory" subtitle="Discover students who have chosen to share their learning progress." />
      <Card>
        <form className="public-directory-search" onSubmit={submitSearch}>
          <Input
            label="Search public profiles"
            placeholder="Search by display name"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            maxLength={80}
          />
          <Button type="submit" icon="search">Search</Button>
        </form>
      </Card>

      {loading && !data ? (
        <LoadingState variant="cards" label="Loading student directory…" />
      ) : error ? (
        <ErrorState title="Unable to load student directory" message={error.message} onRetry={reload} />
      ) : data.students.length === 0 ? (
        <EmptyState
          icon="users"
          title={query ? 'No public profiles match your search' : 'No public profiles yet'}
          message={query ? 'Try another display name.' : 'Students who opt in to sharing will appear here.'}
        />
      ) : (
        <>
          <div className="public-student-grid">
            {data.students.map((student) => (
              <Card key={student.profileId} className="public-student-card">
                <div className="public-student-heading">
                  <span className="avatar avatar-lg" aria-hidden="true">{initials(student.displayName)}</span>
                  <div className="public-student-name">
                    <h2>{student.displayName}</h2>
                    <span className="muted">Public student profile</span>
                  </div>
                </div>
                <dl className="public-student-stats">
                  <div><dt>Problems solved</dt><dd>{student.solvedProblems}</dd></div>
                  <div><dt>Current streak</dt><dd>{student.currentStreak} days</dd></div>
                  <div><dt>Shared projects</dt><dd>{student.publicProjectCount}</dd></div>
                </dl>
                <Link className="btn btn-primary btn-md" to={`/student/students/${encodeURIComponent(student.profileId)}`}>
                  <Icon name="user" size={18} />
                  <span>View profile</span>
                </Link>
              </Card>
            ))}
          </div>
          <PaginationControls
            page={data.page}
            pageCount={pageCount}
            totalItems={data.totalElements}
            onPageChange={setPage}
            pageSize={PAGE_SIZE}
          />
        </>
      )}
    </>
  );
}
