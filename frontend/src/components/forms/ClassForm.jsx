import Button from '../common/Button';
import Input from '../common/Input';
import FormAlert from './FormAlert';
import { useForm } from './useForm';

const EMPTY = { className: '', semester: '', branch: '', section: '' };

function validate(values) {
  const errors = {};
  if (!values.className.trim()) errors.className = 'Class name is required';
  const semester = Number(values.semester);
  if (!Number.isInteger(semester) || semester < 1 || semester > 12) errors.semester = 'Semester must be between 1 and 12';
  if (!values.branch.trim()) errors.branch = 'Branch is required';
  if (!values.section.trim()) errors.section = 'Section is required';
  return errors;
}

/** Teacher class form. The owning teacher is never sent; the backend takes it from the JWT. */
export default function ClassForm({ initialValues, submitLabel = 'Create class', onSubmit, onCancel }) {
  const { bind, handleSubmit, submitting, formError } = useForm(
    initialValues ? { ...EMPTY, ...initialValues, semester: String(initialValues.semester ?? '') } : EMPTY,
    validate,
    (values) =>
      onSubmit({
        className: values.className.trim(),
        semester: Number(values.semester),
        branch: values.branch.trim(),
        section: values.section.trim(),
      }),
  );

  return (
    <form onSubmit={handleSubmit} noValidate className="form">
      <FormAlert message={formError} />
      <Input label="Class name" placeholder="5th Semester CS-IoT" required {...bind('className')} />
      <div className="form-row form-row-3">
        <Input label="Semester" type="number" min="1" max="12" placeholder="5" required {...bind('semester')} />
        <Input label="Branch" placeholder="CS-IoT" required {...bind('branch')} />
        <Input label="Section" placeholder="A" required {...bind('section')} />
      </div>
      <div className="form-actions">
        <Button variant="secondary" onClick={onCancel} disabled={submitting}>
          Cancel
        </Button>
        <Button type="submit" loading={submitting}>
          {submitLabel}
        </Button>
      </div>
    </form>
  );
}
