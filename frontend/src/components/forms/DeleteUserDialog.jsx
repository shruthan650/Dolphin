import { useState } from 'react';
import ConfirmDialog from '../common/ConfirmDialog';
import { useToast } from '../../hooks/useToast';
import { adminService } from '../../services/adminService';
import { getErrorMessage } from '../../services/api';

const CONSEQUENCES = {
  STUDENT: (u) =>
    `${u.name}'s account will be permanently deleted, together with their projects, LeetCode records and advice, and they will be removed from all ${u.classCount} class(es).`,
  TEACHER: (u) =>
    `${u.name} will be permanently deleted along with their ${u.classCount} class(es). Students in those classes are unenrolled but keep their accounts and work.`,
};

/** Admin deletion of a student or teacher account, behind a type-DELETE confirmation. */
export default function DeleteUserDialog({ user, onCancel, onDeleted }) {
  const [busy, setBusy] = useState(false);
  const toast = useToast();

  const remove = async () => {
    setBusy(true);
    try {
      if (user.role === 'TEACHER') await adminService.deleteTeacher(user.id);
      else await adminService.deleteStudent(user.id);
      toast.success(`${user.name} deleted`);
      onDeleted(user);
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <ConfirmDialog
      open={Boolean(user)}
      title={user?.role === 'TEACHER' ? 'Delete teacher?' : 'Delete student?'}
      message={user ? `${CONSEQUENCES[user.role](user)} This cannot be undone.` : ''}
      confirmLabel="Delete account"
      requireText="DELETE"
      loading={busy}
      onConfirm={remove}
      onCancel={onCancel}
    />
  );
}
