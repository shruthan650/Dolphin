import Button from '../common/Button';
import Input from '../common/Input';
import { todayIso } from '../../utils/format';
import { validateOptionalUrl } from '../../utils/validation';
import FormAlert from './FormAlert';
import ClassSelect from './ClassSelect';
import { useForm } from './useForm';

const EMPTY = { problemName: '', problemUrl: '', difficulty: 'EASY', status: 'SOLVED', topic: '', solvedAt: '', classId: '' };

function validate(values) {
  const errors = {};
  if (!values.problemName.trim()) errors.problemName = 'Problem name is required';
  if (!values.classId) errors.classId = 'Choose the class this problem belongs to';
  const url = validateOptionalUrl(values.problemUrl, 'Problem URL');
  if (url) errors.problemUrl = url;
  if (values.solvedAt && values.solvedAt > todayIso()) errors.solvedAt = 'Solved date cannot be in the future';
  return errors;
}

/** classes are the student's joined classes; a single class is preselected. */
export default function LeetCodeForm({ initialValues, classes = [], submitLabel = 'Add problem', onSubmit, onCancel }) {
  const start = initialValues
    ? {
        problemName: initialValues.problemName ?? '',
        problemUrl: initialValues.problemUrl ?? '',
        difficulty: initialValues.difficulty ?? 'EASY',
        status: initialValues.status ?? 'SOLVED',
        topic: initialValues.topic ?? '',
        solvedAt: initialValues.solvedAt ?? '',
        classId: initialValues.classId ?? '',
      }
    : { ...EMPTY, classId: classes.length === 1 ? classes[0].id : '' };
  const { values, bind, handleSubmit, submitting, formError } = useForm(start, validate, (v) =>
    onSubmit({
      problemName: v.problemName.trim(),
      problemUrl: v.problemUrl.trim(),
      difficulty: v.difficulty,
      status: v.status,
      topic: v.topic.trim(),
      solvedAt: v.status === 'SOLVED' && v.solvedAt ? v.solvedAt : null,
      classId: v.classId,
    }),
  );

  return (
    <form onSubmit={handleSubmit} noValidate className="form">
      <FormAlert message={formError} />
      <ClassSelect classes={classes} {...bind('classId')} />
      <Input label="Problem name" placeholder="Two Sum" required maxLength={150} {...bind('problemName')} />
      <Input label="Problem URL" type="url" placeholder="https://leetcode.com/problems/two-sum/" {...bind('problemUrl')} />
      <div className="form-row">
        <Input as="select" label="Difficulty" required {...bind('difficulty')}>
          <option value="EASY">Easy</option>
          <option value="MEDIUM">Medium</option>
          <option value="HARD">Hard</option>
        </Input>
        <Input as="select" label="Status" required {...bind('status')}>
          <option value="SOLVED">Solved</option>
          <option value="ATTEMPTED">Attempted</option>
          <option value="IN_PROGRESS">In progress</option>
        </Input>
      </div>
      <div className="form-row">
        <Input label="Topic" placeholder="Arrays, DP, Graphs…" maxLength={60} {...bind('topic')} />
        {values.status === 'SOLVED' && (
          <Input label="Solved on" type="date" max={todayIso()} hint="Defaults to today" {...bind('solvedAt')} />
        )}
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
