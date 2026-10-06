import Button from '../common/Button';
import Input from '../common/Input';
import FormAlert from './FormAlert';
import { useForm } from './useForm';

const MAX_LENGTH = 1000;

function validate(values) {
  const errors = {};
  if (!values.target) errors.target = 'Choose a project or LeetCode problem';
  if (!values.message.trim()) errors.message = 'Write your advice';
  else if (values.message.length > MAX_LENGTH) errors.message = `At most ${MAX_LENGTH} characters`;
  return errors;
}

/**
 * Teacher form for advising a student on one of their projects or LeetCode problems.
 * onSubmit receives { targetType, targetId, message }.
 */
export default function AdviceForm({ projects, entries, onSubmit }) {
  const { values, bind, handleSubmit, submitting, formError, setValues } = useForm(
    { target: '', message: '' },
    validate,
    async (v) => {
      const [targetType, targetId] = v.target.split(':');
      await onSubmit({ targetType, targetId, message: v.message.trim() });
      setValues((current) => ({ ...current, message: '' }));
    },
  );

  return (
    <form onSubmit={handleSubmit} noValidate className="form">
      <FormAlert message={formError} />
      <Input as="select" label="About" required {...bind('target')}>
        <option value="">Choose a project or problem…</option>
        {projects.length > 0 && (
          <optgroup label="Projects">
            {projects.map((p) => (
              <option key={p.id} value={`PROJECT:${p.id}`}>
                {p.title}
              </option>
            ))}
          </optgroup>
        )}
        {entries.length > 0 && (
          <optgroup label="LeetCode problems">
            {entries.map((e) => (
              <option key={e.id} value={`LEETCODE:${e.id}`}>
                {e.problemName} ({e.status.replace('_', ' ').toLowerCase()})
              </option>
            ))}
          </optgroup>
        )}
      </Input>
      <Input
        as="textarea"
        label="Advice"
        required
        rows={4}
        maxLength={MAX_LENGTH}
        placeholder="e.g. Nice work! Try the two-pointer approach to get this down to O(n)."
        hint={`${values.message.length}/${MAX_LENGTH}`}
        {...bind('message')}
      />
      <div className="form-actions">
        <Button type="submit" icon="message" loading={submitting}>
          Send advice
        </Button>
      </div>
    </form>
  );
}
