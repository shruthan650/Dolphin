import { useMemo, useState } from 'react';
import Button from '../../components/common/Button';
import Card from '../../components/common/Card';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import LoadingState from '../../components/common/LoadingState';
import PageHeader from '../../components/common/PageHeader';
import SearchInput from '../../components/common/SearchInput';
import DeleteUserDialog from '../../components/forms/DeleteUserDialog';
import UsersTable from '../../components/tables/UsersTable';
import { useApi } from '../../hooks/useApi';
import { adminService } from '../../services/adminService';

const ROLES = ['ALL', 'ADMIN', 'TEACHER', 'STUDENT'];

export default function Users() {
  const [deleting, setDeleting] = useState(null);
  const { data, setData, loading, error, reload } = useApi(adminService.users);
  const [query, setQuery] = useState('');
  const [role, setRole] = useState('ALL');

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return (data || []).filter(
      (u) => (role === 'ALL' || u.role === role) && (!q || u.name.toLowerCase().includes(q) || u.email.toLowerCase().includes(q)),
    );
  }, [data, query, role]);

  return (
    <>
      <PageHeader title="Users" subtitle="Every account in the system" />
      <Card
        title={data ? `${filtered.length} of ${data.length} users` : 'Users'}
        actions={
          data?.length > 0 && (
            <div className="toolbar">
              <div className="segmented segmented-sm" role="tablist" aria-label="Filter by role">
                {ROLES.map((r) => (
                  <button key={r} type="button" role="tab" aria-selected={role === r} className={role === r ? 'active' : ''} onClick={() => setRole(r)}>
                    {r === 'ALL' ? 'All' : r.charAt(0) + r.slice(1).toLowerCase()}
                  </button>
                ))}
              </div>
              <SearchInput value={query} onChange={setQuery} placeholder="Search users" />
            </div>
          )
        }
        padded={false}
      >
        {loading ? (
          <LoadingState variant="table" label="Loading users…" />
        ) : error ? (
          <ErrorState title="Unable to load users" message={error.message} onRetry={reload} />
        ) : filtered.length === 0 ? (
          <EmptyState icon="search" title="No matching users" />
        ) : (
          <UsersTable
            users={filtered}
            showRole
            actions={(u) =>
              u.role !== 'ADMIN' && (
                <div className="row-actions">
                  <Button
                    variant="ghost"
                    size="sm"
                    icon="trash"
                    className="btn-danger-ghost"
                    aria-label={`Delete ${u.name}`}
                    title={`Delete ${u.name}`}
                    onClick={() => setDeleting(u)}
                  />
                </div>
              )
            }
          />
        )}
      </Card>

      <DeleteUserDialog
        user={deleting}
        onCancel={() => setDeleting(null)}
        onDeleted={(u) => {
          setDeleting(null);
          setData((current) => current.filter((x) => x.id !== u.id));
        }}
      />
    </>
  );
}
