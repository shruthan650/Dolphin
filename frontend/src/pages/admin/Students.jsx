import { useMemo, useState } from 'react';
import Card from '../../components/common/Card';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import LoadingState from '../../components/common/LoadingState';
import PageHeader from '../../components/common/PageHeader';
import SearchInput from '../../components/common/SearchInput';
import UsersTable from '../../components/tables/UsersTable';
import { useApi } from '../../hooks/useApi';
import { adminService } from '../../services/adminService';

export default function Students() {
  const { data, loading, error, reload } = useApi(adminService.students);
  const [query, setQuery] = useState('');

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return (data || []).filter((s) => !q || s.name.toLowerCase().includes(q) || s.email.toLowerCase().includes(q));
  }, [data, query]);

  return (
    <>
      <PageHeader title="Students" subtitle="All registered student accounts" />
      <Card
        title={data ? `${data.length} student${data.length === 1 ? '' : 's'}` : 'Students'}
        actions={data?.length > 0 && <SearchInput value={query} onChange={setQuery} placeholder="Search students" />}
        padded={false}
      >
        {loading ? (
          <LoadingState variant="table" label="Loading students…" />
        ) : error ? (
          <ErrorState title="Unable to load students" message={error.message} onRetry={reload} />
        ) : data.length === 0 ? (
          <EmptyState icon="users" title="No students yet" message="Students appear here once they sign up." />
        ) : filtered.length === 0 ? (
          <EmptyState icon="search" title="No matching students" />
        ) : (
          <UsersTable users={filtered} classLabel="Classes joined" />
        )}
      </Card>
    </>
  );
}
