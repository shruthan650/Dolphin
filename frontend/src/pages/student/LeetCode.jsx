import { useMemo, useState } from 'react';
import AdviceList from '../../components/cards/AdviceList';
import StatCard from '../../components/cards/StatCard';
import Button from '../../components/common/Button';
import Card from '../../components/common/Card';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import LoadingState from '../../components/common/LoadingState';
import Modal from '../../components/common/Modal';
import PageHeader from '../../components/common/PageHeader';
import LeetCodeForm from '../../components/forms/LeetCodeForm';
import LeetCodeTable from '../../components/tables/LeetCodeTable';
import { useApi } from '../../hooks/useApi';
import { useToast } from '../../hooks/useToast';
import { adviceService } from '../../services/adviceService';
import { getErrorMessage } from '../../services/api';
import { leetcodeService } from '../../services/leetcodeService';
import { studentService } from '../../services/studentService';

export default function LeetCode() {
  const { data, setData, loading, error, reload } = useApi(leetcodeService.mine);
  const advice = useApi(adviceService.mine);
  const classes = useApi(studentService.classes);
  const [classFilter, setClassFilter] = useState('ALL');
  const classNames = useMemo(
    () => Object.fromEntries((classes.data || []).map((c) => [c.id, c.className])),
    [classes.data],
  );
  const hasClasses = classes.data?.length > 0;
  const problemAdvice = (advice.data || []).filter((a) => a.targetType === 'LEETCODE');
  const [editor, setEditor] = useState(null); // null | { entry?: object }
  const [deleting, setDeleting] = useState(null);
  const [busy, setBusy] = useState(false);
  const [status, setStatus] = useState('ALL');
  const toast = useToast();

  const stats = useMemo(() => {
    const entries = data || [];
    const solved = entries.filter((e) => e.status === 'SOLVED');
    return {
      total: entries.length,
      solved: solved.length,
      easy: solved.filter((e) => e.difficulty === 'EASY').length,
      medium: solved.filter((e) => e.difficulty === 'MEDIUM').length,
      hard: solved.filter((e) => e.difficulty === 'HARD').length,
    };
  }, [data]);

  const filtered = useMemo(
    () =>
      (data || []).filter(
        (e) =>
          (status === 'ALL' || e.status === status) &&
          (classFilter === 'ALL' || (classFilter === 'NONE' ? !e.classId : e.classId === classFilter)),
      ),
    [data, status, classFilter],
  );

  const save = async (values) => {
    if (editor.entry) {
      const updated = await leetcodeService.update(editor.entry.id, values);
      setData((current) => current.map((e) => (e.id === updated.id ? updated : e)));
      toast.success('Problem updated successfully');
    } else {
      const created = await leetcodeService.create(values);
      setData((current) => [created, ...current]);
      toast.success('Problem added successfully');
    }
    setEditor(null);
  };

  const confirmDelete = async () => {
    setBusy(true);
    try {
      await leetcodeService.remove(deleting.id);
      setData((current) => current.filter((e) => e.id !== deleting.id));
      toast.success('Problem deleted');
      setDeleting(null);
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <PageHeader
        title="LeetCode progress"
        subtitle="Log the problems you work on and track your growth"
        actions={
          <Button icon="plus" onClick={() => setEditor({})} disabled={!hasClasses}>
            Add problem
          </Button>
        }
      />

      {classes.data && !hasClasses && (
        <Card title="Join a class first" subtitle="Every problem you log belongs to one of your classes">
          <Button to="/student/class">Join a class</Button>
        </Card>
      )}

      {loading ? (
        <LoadingState variant="cards" label="Loading problems…" />
      ) : error ? (
        <ErrorState title="Unable to load LeetCode progress." message={error.message} onRetry={reload} />
      ) : (
        <>
          <div className="stat-grid">
            <StatCard label="Problems solved" value={stats.solved} icon="target" tone="teal" hint={`${stats.total} logged`} />
            <StatCard label="Easy" value={stats.easy} icon="code" tone="indigo" hint="Solved" />
            <StatCard label="Medium" value={stats.medium} icon="code" tone="amber" hint="Solved" />
            <StatCard label="Hard" value={stats.hard} icon="code" tone="rose" hint="Solved" />
          </div>

          {problemAdvice.length > 0 && (
            <Card title="Advice from your teachers" subtitle="Tips on the problems you have logged">
              <AdviceList items={problemAdvice} />
            </Card>
          )}

          <Card
            title="Problem list"
            actions={
              data.length > 0 && (
                <div className="toolbar">
                  <select className="field-control field-control-sm" value={classFilter} onChange={(e) => setClassFilter(e.target.value)} aria-label="Filter by class">
                    <option value="ALL">All classes</option>
                {(classes.data || []).map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.className}
                  </option>
                ))}
                    <option value="NONE">Unassigned</option>
                  </select>
                  <select className="field-control field-control-sm" value={status} onChange={(e) => setStatus(e.target.value)} aria-label="Filter by status">
                    <option value="ALL">All statuses</option>
                    <option value="SOLVED">Solved</option>
                    <option value="ATTEMPTED">Attempted</option>
                    <option value="IN_PROGRESS">In progress</option>
                  </select>
                </div>
              )
            }
            padded={false}
          >
            {data.length === 0 ? (
              <EmptyState
                icon="code"
                title="No problems logged yet."
                message="Add the first problem you've worked on."
                action={
                  hasClasses && (
                    <Button icon="plus" onClick={() => setEditor({})}>
                      Add problem
                    </Button>
                  )
                }
              />
            ) : filtered.length === 0 ? (
              <EmptyState icon="search" title="No matching problems" />
            ) : (
              <LeetCodeTable entries={filtered} classLabel={(e) => classNames[e.classId]} onEdit={(entry) => setEditor({ entry })} onDelete={setDeleting} />
            )}
          </Card>
        </>
      )}

      <Modal open={Boolean(editor)} title={editor?.entry ? 'Edit problem' : 'Add problem'} onClose={() => setEditor(null)}>
        {editor && (
          <LeetCodeForm
            initialValues={editor.entry}
            classes={classes.data || []}
            submitLabel={editor.entry ? 'Save changes' : 'Add problem'}
            onSubmit={save}
            onCancel={() => setEditor(null)}
          />
        )}
      </Modal>

      <ConfirmDialog
        open={Boolean(deleting)}
        title="Delete problem?"
        message={`"${deleting?.problemName}" will be removed from your progress.`}
        confirmLabel="Delete"
        loading={busy}
        onConfirm={confirmDelete}
        onCancel={() => setDeleting(null)}
      />
    </>
  );
}
