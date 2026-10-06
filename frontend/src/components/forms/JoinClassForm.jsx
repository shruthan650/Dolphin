import Button from '../common/Button';
import Input from '../common/Input';
import { classService } from '../../services/classService';
import FormAlert from './FormAlert';
import { useForm } from './useForm';

function validate(values) {
  return values.classCode.trim() ? {} : { classCode: 'Enter the class code from your teacher' };
}

export default function JoinClassForm({ onJoined }) {
  const { bind, handleSubmit, submitting, formError, setValues } = useForm({ classCode: '' }, validate, async (v) => {
    const result = await classService.join(v.classCode.trim().toUpperCase());
    setValues({ classCode: '' });
    onJoined(result);
  });

  return (
    <form onSubmit={handleSubmit} noValidate className="form join-form">
      <FormAlert message={formError} />
      <div className="join-row">
        <Input
          label="Class code"
          placeholder="DOLPHIN-A8F21"
          autoComplete="off"
          spellCheck={false}
          className="join-input"
          {...bind('classCode')}
        />
        <Button type="submit" icon="login" loading={submitting}>
          Join class
        </Button>
      </div>
    </form>
  );
}
