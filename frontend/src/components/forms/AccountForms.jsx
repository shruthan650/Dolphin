import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '../common/Button';
import ConfirmDialog from '../common/ConfirmDialog';
import Input from '../common/Input';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { getErrorMessage, getFieldErrors } from '../../services/api';
import { userService } from '../../services/userService';
import { isEmail } from '../../utils/validation';
import FormAlert from './FormAlert';
import { useForm } from './useForm';

/**
 * Two-step submit for sensitive changes: the form validates, then a confirmation dialog runs the request.
 * Server errors are shown on the form after the dialog closes.
 */
function useConfirmedSubmit(validate, action) {
  const [values, setValues] = useState(null);
  const [busy, setBusy] = useState(false);
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState(null);

  const request = (formValues) => {
    const clientErrors = validate(formValues);
    setErrors(clientErrors);
    setFormError(null);
    if (Object.keys(clientErrors).length === 0) setValues(formValues);
  };

  const confirm = async () => {
    setBusy(true);
    try {
      await action(values);
      setValues(null);
    } catch (err) {
      setErrors(getFieldErrors(err));
      setFormError(getErrorMessage(err));
      setValues(null);
    } finally {
      setBusy(false);
    }
  };

  return { pending: values, busy, errors, formError, request, confirm, cancel: () => setValues(null) };
}

/** Name for every role. Students keep their coding profile links, which are edited separately. */
export function ProfileDetailsForm() {
  const { user, updateProfile } = useAuth();
  const toast = useToast();
  const { bind, handleSubmit, submitting, formError } = useForm(
    { name: user?.name ?? '' },
    (v) => (v.name.trim() ? {} : { name: 'Name is required' }),
    async (v) => {
      await updateProfile({ name: v.name.trim(), githubUrl: user.githubUrl ?? '', leetCodeUrl: user.leetCodeUrl ?? '' });
      toast.success('Profile updated');
    },
  );
  return (
    <form onSubmit={handleSubmit} noValidate className="form">
      <FormAlert message={formError} />
      <Input label="Full name" autoComplete="name" maxLength={100} required {...bind('name')} />
      <div className="form-actions">
        <Button type="submit" loading={submitting}>
          Save name
        </Button>
      </div>
    </form>
  );
}

export function ChangeEmailForm() {
  const { user, changeEmail } = useAuth();
  const toast = useToast();
  const [form, setForm] = useState({ newEmail: '', currentPassword: '' });
  const flow = useConfirmedSubmit(
    (v) => {
      const errors = {};
      if (!v.newEmail.trim()) errors.newEmail = 'New email is required';
      else if (!isEmail(v.newEmail)) errors.newEmail = 'Enter a valid email address';
      else if (v.newEmail.trim().toLowerCase() === user?.email) errors.newEmail = 'This is already your email address';
      if (!v.currentPassword) errors.currentPassword = 'Current password is required';
      return errors;
    },
    async (v) => {
      await changeEmail({ newEmail: v.newEmail.trim(), currentPassword: v.currentPassword });
      setForm({ newEmail: '', currentPassword: '' });
      toast.success('Email changed. Use the new address the next time you sign in.');
    },
  );
  const field = (name) => ({
    name,
    value: form[name],
    onChange: (e) => setForm((f) => ({ ...f, [name]: e.target.value })),
    error: flow.errors[name],
  });

  return (
    <form
      noValidate
      className="form"
      onSubmit={(e) => {
        e.preventDefault();
        flow.request(form);
      }}
    >
      <FormAlert message={flow.formError} />
      <p className="muted">
        Current email: <strong className="break-anywhere">{user?.email}</strong>
      </p>
      <Input label="New email" type="email" autoComplete="email" required {...field('newEmail')} />
      <Input label="Current password" type="password" autoComplete="current-password" required {...field('currentPassword')} />
      <div className="form-actions">
        <Button type="submit">Change email</Button>
      </div>
      <ConfirmDialog
        open={Boolean(flow.pending)}
        title="Change your email?"
        message={`You will sign in with ${flow.pending?.newEmail.trim()} from now on. Other devices signed in to this account will be signed out.`}
        confirmLabel="Change email"
        variant="primary"
        loading={flow.busy}
        onConfirm={flow.confirm}
        onCancel={flow.cancel}
      />
    </form>
  );
}

const EMPTY_PASSWORDS = { currentPassword: '', newPassword: '', confirmPassword: '' };

export function ChangePasswordForm() {
  const { changePassword } = useAuth();
  const toast = useToast();
  const [form, setForm] = useState(EMPTY_PASSWORDS);
  const flow = useConfirmedSubmit(
    (v) => {
      const errors = {};
      if (!v.currentPassword) errors.currentPassword = 'Current password is required';
      if (v.newPassword.length < 8) errors.newPassword = 'Password must be at least 8 characters';
      else if (v.newPassword.length > 72) errors.newPassword = 'Password must be at most 72 characters';
      if (v.confirmPassword !== v.newPassword) errors.confirmPassword = 'Passwords do not match';
      return errors;
    },
    async (v) => {
      await changePassword(v);
      setForm(EMPTY_PASSWORDS);
      toast.success('Password changed');
    },
  );
  const field = (name) => ({
    name,
    value: form[name],
    onChange: (e) => setForm((f) => ({ ...f, [name]: e.target.value })),
    error: flow.errors[name],
  });

  return (
    <form
      noValidate
      className="form"
      onSubmit={(e) => {
        e.preventDefault();
        flow.request(form);
      }}
    >
      <FormAlert message={flow.formError} />
      <Input label="Current password" type="password" autoComplete="current-password" required {...field('currentPassword')} />
      <div className="form-row">
        <Input label="New password" type="password" autoComplete="new-password" hint="At least 8 characters" required {...field('newPassword')} />
        <Input label="Confirm new password" type="password" autoComplete="new-password" required {...field('confirmPassword')} />
      </div>
      <div className="form-actions">
        <Button type="submit">Change password</Button>
      </div>
      <ConfirmDialog
        open={Boolean(flow.pending)}
        title="Change your password?"
        message="Other devices signed in to this account will be signed out. This device stays signed in."
        confirmLabel="Change password"
        variant="primary"
        loading={flow.busy}
        onConfirm={flow.confirm}
        onCancel={flow.cancel}
      />
    </form>
  );
}

/** Permanent self-deletion for students and teachers, behind a type-DELETE confirmation and the password. */
export function DeleteAccountSection() {
  const { user, logout } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const close = () => {
    setOpen(false);
    setPassword('');
    setError(null);
  };

  const confirm = async () => {
    setBusy(true);
    setError(null);
    try {
      await userService.deleteAccount({ currentPassword: password, confirmation: 'DELETE' });
      logout();
      toast.success('Your account has been deleted');
      navigate('/login', { replace: true });
    } catch (err) {
      setError(getErrorMessage(err));
      setBusy(false);
    }
  };

  const consequences =
    user?.role === 'TEACHER'
      ? 'Your account and all your classes will be permanently deleted. Students in your classes are unenrolled, and the advice you gave is removed.'
      : 'Your account, projects, LeetCode records and advice will be permanently deleted, and you will be removed from every class.';

  return (
    <div className="danger-zone">
      <div>
        <h3 className="danger-zone-title">Delete account</h3>
        <p className="muted">{consequences} This cannot be undone.</p>
      </div>
      <Button variant="danger" icon="trash" onClick={() => setOpen(true)}>
        Delete account
      </Button>
      <ConfirmDialog
        open={open}
        title="Delete your account?"
        message={`${consequences} This cannot be undone.`}
        confirmLabel="Delete my account"
        requireText="DELETE"
        confirmDisabled={!password}
        loading={busy}
        onConfirm={confirm}
        onCancel={close}
      >
        <FormAlert message={error} />
        <Input
          label="Current password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
      </ConfirmDialog>
    </div>
  );
}
