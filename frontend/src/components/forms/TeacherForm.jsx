import Button from '../common/Button';
import Input from '../common/Input';
import { validateAccount } from '../../utils/validation';
import FormAlert from './FormAlert';
import { useForm } from './useForm';

const EMPTY = { name: '', email: '', password: '', confirmPassword: '' };

/** Admin "create teacher" form. There is deliberately no role selector: the backend always creates a TEACHER. */
export default function TeacherForm({ onSubmit, onCancel }) {
  const { bind, handleSubmit, submitting, formError } = useForm(EMPTY, validateAccount, onSubmit);

  return (
    <form onSubmit={handleSubmit} noValidate className="form">
      <FormAlert message={formError} />
      <Input label="Full name" autoComplete="off" required {...bind('name')} />
      <Input label="Email" type="email" autoComplete="off" required {...bind('email')} />
      <div className="form-row">
        <Input label="Password" type="password" autoComplete="new-password" required hint="At least 8 characters" {...bind('password')} />
        <Input label="Confirm password" type="password" autoComplete="new-password" required {...bind('confirmPassword')} />
      </div>
      <div className="form-actions">
        <Button variant="secondary" onClick={onCancel} disabled={submitting}>
          Cancel
        </Button>
        <Button type="submit" loading={submitting}>
          Create teacher
        </Button>
      </div>
    </form>
  );
}
