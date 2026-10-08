import { useMemo, useState } from 'react';
import Button from '../../components/common/Button';
import Card from '../../components/common/Card';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import LoadingState from '../../components/common/LoadingState';
import Modal from '../../components/common/Modal';
import PageHeader from '../../components/common/PageHeader';
import SearchInput from '../../components/common/SearchInput';
import TeacherForm from '../../components/forms/TeacherForm';
import UsersTable from '../../components/tables/UsersTable';
import { useApi } from '../../hooks/useApi';
import { useToast } from '../../hooks/useToast';
import { adminService } from '../../services/adminService';
import { getErrorMessage } from '../../services/api';

function matches(user, query) {
  const q = query.trim().toLowerCase();
  return !q || user.name.toLowerCase().includes(q) || user.email.toLowerCase().includes(q);
}

export default function Teachers() {
  const { data, setData, loading, error, reload } = useApi(adminService.teachers);
  const [creating, setCreating] = useState(false);
  const [query, setQuery] = useState('');
  const [pending, setPending] = useState(null);
  const [deleting, setDeleting] = useState(null);
  const [saving, setSaving] = useState(false);
  const toast = useToast();

  const filtered = useMemo(() => (data || []).filter((t) => matches(t, query)), [data, query]);

  const createTeacher = async (values) => {
    const teacher = await adminService.createTeacher(values);
    setData((current) => [...(current || []), teacher]);
    setCreating(false);
    toast.success(`Teacher created successfully: ${teacher.name}`);
  };

  const toggleActive = async () => {
    setSaving(true);
    try {
      const updated = await adminService.setTeacherActive(pending.id, !pending.active);
      setData((current) => current.map((t) => (t.id === updated.id ? updated : t)));
      toast.success(`${updated.name} ${updated.active ? 'activated' : 'deactivated'}`);
      setPending(null);
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const deleteTeacher = async () => {
    setSaving(true);
    try {
      await adminService.deleteTeacher(deleting.id);
      setData((current) => current.filter((t) => t.id !== deleting.id));
      toast.success(`${deleting.name} deleted`);
      setDeleting(null);
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <PageHeader
        title="Teachers"
        subtitle="Create teacher accounts and manage their access"
        actions={
          <Button icon="plus" onClick={() => setCreating(true)}>
            Create teacher
          </Button>
        }
      />

      <Card
        title={data ? `${data.length} teacher${data.length === 1 ? '' : 's'}` : 'Teachers'}
        actions={data?.length > 0 && <SearchInput value={query} onChange={setQuery} placeholder="Search teachers" />}
        padded={false}
      >
        {loading ? (
          <LoadingState variant="table" label="Loading teachers…" />
        ) : error ? (
          <ErrorState title="Unable to load teachers" message={error.message} onRetry={reload} />
        ) : data.length === 0 ? (
          <EmptyState
            icon="teacher"
            title="No teachers yet"
            message="Create the first teacher account so they can start creating classes."
            action={
              <Button icon="plus" onClick={() => setCreating(true)}>
                Create teacher
              </Button>
            }
          />
        ) : filtered.length === 0 ? (
          <EmptyState icon="search" title="No matching teachers" message="Try a different name or email." />
        ) : (
          <UsersTable
            users={filtered}
            actions={(t) => (
              <div className="row-actions">
                <Button variant={t.active ? 'secondary' : 'primary'} size="sm" icon="power" onClick={() => setPending(t)}>
                  {t.active ? 'Deactivate' : 'Activate'}
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  icon="trash"
                  className="btn-danger-ghost"
                  aria-label={`Delete ${t.name}`}
                  onClick={() => setDeleting(t)}
                />
              </div>
            )}
          />
        )}
      </Card>

      <Modal open={creating} title="Create teacher" description="The new account will have the Teacher role." onClose={() => setCreating(false)}>
        <TeacherForm onSubmit={createTeacher} onCancel={() => setCreating(false)} />
      </Modal>

      <ConfirmDialog
        open={Boolean(pending)}
        title={pending?.active ? 'Deactivate teacher?' : 'Activate teacher?'}
        message={
          pending?.active
            ? `${pending?.name} will be signed out and will not be able to sign in until reactivated. Their classes are kept.`
            : `${pending?.name} will be able to sign in again.`
        }
        confirmLabel={pending?.active ? 'Deactivate' : 'Activate'}
        variant={pending?.active ? 'danger' : 'primary'}
        loading={saving}
        onConfirm={toggleActive}
        onCancel={() => setPending(null)}
      />

      <ConfirmDialog
        open={Boolean(deleting)}
        title="Delete teacher?"
        message={`${deleting?.name} will be permanently deleted along with their ${deleting?.classCount ?? 0} class(es). Students in those classes are unenrolled but keep their accounts, projects and LeetCode entries. This cannot be undone.`}
        confirmLabel="Delete teacher"
        requireText="DELETE"
        loading={saving}
        onConfirm={deleteTeacher}
        onCancel={() => setDeleting(null)}
      />
    </>
  );
}
