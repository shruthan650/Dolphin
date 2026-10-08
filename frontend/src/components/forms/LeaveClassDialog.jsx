import { useState } from 'react';
import ConfirmDialog from '../common/ConfirmDialog';
import { useToast } from '../../hooks/useToast';
import { getErrorMessage } from '../../services/api';
import { studentService } from '../../services/studentService';

/**
 * Confirms leaving a class and warns that the student's data of that class is deleted. onLeft runs only after the
 * backend confirmed the class was left; on failure the error is shown and nothing changes.
 */
export default function LeaveClassDialog({ cls, onCancel, onLeft }) {
  const [busy, setBusy] = useState(false);
  const toast = useToast();

  const leave = async () => {
    setBusy(true);
    try {
      await studentService.leaveClass(cls.id);
      toast.success(`You left ${cls.className}`);
      onLeft(cls);
    } catch (err) {
      toast.error(getErrorMessage(err, 'Unable to leave the class. Please try again.'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <ConfirmDialog
      open={Boolean(cls)}
      title="Leave this class?"
      message={
        <div className="confirm-message">
          <p>
            You are about to leave <strong>{cls?.className}</strong>. All your projects, LeetCode records, project
            activities, and other class-specific data associated with this class will be permanently deleted.
          </p>
          <p>Your profile and data belonging to your other classes will not be affected.</p>
          <p>Are you sure you want to leave?</p>
        </div>
      }
      confirmLabel="Leave Class"
      loading={busy}
      onConfirm={leave}
      onCancel={onCancel}
    />
  );
}
