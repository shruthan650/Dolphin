import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Button from '../../components/common/Button';
import Card from '../../components/common/Card';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import CopyButton from '../../components/common/CopyButton';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import LoadingState from '../../components/common/LoadingState';
import Modal from '../../components/common/Modal';
import PageHeader from '../../components/common/PageHeader';
import ClassForm from '../../components/forms/ClassForm';
import StudentProgressTable from '../../components/tables/StudentProgressTable';
import { useApi } from '../../hooks/useApi';
import { useToast } from '../../hooks/useToast';
import { getErrorMessage } from '../../services/api';
import { classService } from '../../services/classService';
import { formatDate } from '../../utils/format';

export default function ClassDetails() {
  const { id } = useParams();
  const { data, setData, loading, error, reload } = useApi(() => classService.details(id), [id]);
  const [editing, setEditing] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [removing, setRemoving] = useState(null);
  const [busy, setBusy] = useState(false);
  const toast = useToast();
  const navigate = useNavigate();

  if (loading) return <LoadingState label="Loading class…" />;
  if (error) {
    return (
      <>
        <PageHeader title="Class" backTo="/teacher/classes" backLabel="My classes" />
        <ErrorState
          title={error.status === 403 ? 'Access denied' : error.status === 404 ? 'Class not found' : 'Unable to load class'}
          message={error.message}
          onRetry={error.status >= 500 || !error.status ? reload : undefined}
        />
      </>
    );
  }

  const cls = data.classInfo;

  const updateClass = async (values) => {
    const updated = await classService.update(id, values);
    setData((current) => ({ ...current, classInfo: updated }));
    setEditing(false);
    toast.success('Class updated');
  };

  const deleteClass = async () => {
    setBusy(true);
    try {
      await classService.remove(id);
      toast.success(`${cls.className} deleted`);
      navigate('/teacher/classes', { replace: true });
    } catch (err) {
      toast.error(getErrorMessage(err));
      setBusy(false);
    }
  };

  const removeStudent = async () => {
    setBusy(true);
    try {
      await classService.removeStudent(id, removing.id);
      toast.success(`${removing.name} removed from class`);
      setRemoving(null);
      reload();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <PageHeader
        title={cls.className}
        subtitle={`Semester ${cls.semester} · ${cls.branch} · Section ${cls.section}`}
        backTo="/teacher/classes"
        backLabel="My classes"
        actions={
          <>
            <Button variant="secondary" icon="edit" onClick={() => setEditing(true)}>
              Edit
            </Button>
            <Button variant="danger" icon="trash" onClick={() => setConfirmDelete(true)}>
              Delete
            </Button>
          </>
        }
      />

      <div className="info-grid">
        <div className="info-item">
          <span className="info-label">Class code</span>
          <CopyButton value={cls.classCode} />
        </div>
        <div className="info-item">
          <span className="info-label">Students</span>
          <span className="info-value">{cls.studentCount}</span>
        </div>
        <div className="info-item">
          <span className="info-label">Semester</span>
          <span className="info-value">{cls.semester}</span>
        </div>
        <div className="info-item">
          <span className="info-label">Branch / Section</span>
          <span className="info-value">
            {cls.branch} / {cls.section}
          </span>
        </div>
        <div className="info-item">
          <span className="info-label">Created</span>
          <span className="info-value">{formatDate(cls.createdAt)}</span>
        </div>
      </div>

      <Card title="Students" subtitle="Click a student to see their projects and LeetCode progress" padded={false}>
        {data.students.length === 0 ? (
          <EmptyState
            icon="users"
            title="No students have joined yet"
            message={`Share the class code ${cls.classCode} with your students. They can join from their dashboard.`}
          />
        ) : (
          <StudentProgressTable students={data.students} onRemove={setRemoving} />
        )}
      </Card>

      <Modal open={editing} title="Edit class" onClose={() => setEditing(false)}>
        <ClassForm initialValues={cls} submitLabel="Save changes" onSubmit={updateClass} onCancel={() => setEditing(false)} />
      </Modal>

      <ConfirmDialog
        open={confirmDelete}
        title="Delete class?"
        message={`"${cls.className}" will be deleted and its ${cls.studentCount} student(s) will be unenrolled. Their projects and LeetCode entries are kept, no longer assigned to a class.`}
        confirmLabel="Delete class"
        loading={busy}
        onConfirm={deleteClass}
        onCancel={() => setConfirmDelete(false)}
      />

      <ConfirmDialog
        open={Boolean(removing)}
        title="Remove student?"
        message={`${removing?.name} will be removed from ${cls.className}. Their projects, LeetCode records and advice in this class will be permanently deleted. Their account and their data in other classes are not affected.`}
        confirmLabel="Remove"
        loading={busy}
        onConfirm={removeStudent}
        onCancel={() => setRemoving(null)}
      />
    </>
  );
}
