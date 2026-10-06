import Button from '../common/Button';
import Input from '../common/Input';
import FormAlert from './FormAlert';
import { useForm } from './useForm';
import { useAuth } from '../../hooks/useAuth';
import { validateProfileLinks } from '../../utils/validation';

/** Lets a student add or change their GitHub and LeetCode profile URLs. */
export default function ProfileLinksForm({ initialValues, submitLabel = 'Save profiles', onSaved }) {
  const { updateProfileLinks } = useAuth();
  const { bind, handleSubmit, submitting, formError } = useForm(
    { githubUrl: initialValues?.githubUrl ?? '', leetCodeUrl: initialValues?.leetCodeUrl ?? '' },
    validateProfileLinks,
    async (v) => onSaved?.(await updateProfileLinks({ githubUrl: v.githubUrl.trim(), leetCodeUrl: v.leetCodeUrl.trim() })),
  );
  return (
    <form onSubmit={handleSubmit} noValidate className="form">
      <FormAlert message={formError} />
      <ProfileLinkFields bind={bind} />
      <Button type="submit" loading={submitting} className="btn-block">
        {submitLabel}
      </Button>
    </form>
  );
}

/** The two URL inputs, shared by sign-up and the profile links form. */
export function ProfileLinkFields({ bind }) {
  return (
    <>
      <Input label="GitHub profile URL" type="url" required autoComplete="url" placeholder="https://github.com/username" {...bind('githubUrl')} />
      <Input label="LeetCode profile URL" type="url" required autoComplete="url" placeholder="https://leetcode.com/u/username" {...bind('leetCodeUrl')} />
    </>
  );
}
