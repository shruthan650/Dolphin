import { useMemo, useState } from 'react';
import Button from '../../components/common/Button';
import Card from '../../components/common/Card';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import LoadingState from '../../components/common/LoadingState';
import PageHeader from '../../components/common/PageHeader';
import SearchInput from '../../components/common/SearchInput';
import StudentProgressTable from '../../components/tables/StudentProgressTable';
import { useApi } from '../../hooks/useApi';
import { useToast } from '../../hooks/useToast';
import { getErrorMessage } from '../../services/api';
import { teacherService } from '../../services/teacherService';

export default function Students() {
  const { data, setData, loading, error, reload } = useApi(teacherService.students);
  const [query, setQuery] = useState('');
  const [deleting, setDeleting] = useState(null);
  const [busy, setBusy] = useState(false);
  const toast = useToast();

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return (data || []).filter(
      (s) =>
        !q ||
        s.name.toLowerCase().includes(q) ||
        s.email.toLowerCase().includes(q) ||
        s.classNames.some((c) => c.toLowerCase().includes(q)),
    );
  }, [data, query]);

  const deleteStudent = async () => {
    setBusy(true);
    try {
      await teacherService.deleteStudent(deleting.id);
      setData((current) => current.filter((s) => s.id !== deleting.id));
      toast.success(`${deleting.name} deleted`);
      setDeleting(null);
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <PageHeader title="Students" subtitle="Students enrolled in your classes" />
      <Card
        title={data ? `${data.length} student${data.length === 1 ? '' : 's'}` : 'Students'}
        actions={data?.length > 0 && <SearchInput value={query} onChange={setQuery} placeholder="Search name, email or class" />}
        padded={false}
      >
        {loading ? (
          <LoadingState variant="table" label="Loading students…" />
        ) : error ? (
          <ErrorState title="Unable to load students" message={error.message} onRetry={reload} />
        ) : data.length === 0 ? (
          <EmptyState
            icon="users"
            title="No students yet"
            message="Students appear here after they join one of your classes with its class code."
            action={<Button to="/teacher/classes">Go to my classes</Button>}
          />
        ) : filtered.length === 0 ? (
          <EmptyState icon="search" title="No matching students" />
        ) : (
          <StudentProgressTable
            students={filtered}
            showClasses
            onRemove={setDeleting}
            removeLabel={(s) => `Delete ${s.name}`}
          />
        )}
      </Card>

      <ConfirmDialog
        open={Boolean(deleting)}
        title="Delete student?"
        message={`${deleting?.name}'s account will be permanently deleted, together with their projects, LeetCode entries and enrolment in every class. This cannot be undone. To only take them out of one class, use Remove on the class page instead.`}
        confirmLabel="Delete student"
        loading={busy}
        onConfirm={deleteStudent}
        onCancel={() => setDeleting(null)}
      />
    </>
  );
}
