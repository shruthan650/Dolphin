import { useState } from 'react';
import { getErrorMessage, getFieldErrors } from '../../services/api';

/**
 * Minimal form state helper: values, client validation, submit state and server errors.
 *
 * @param initialValues
 * @param validate (values) => errors object (empty when valid)
 * @param onSubmit async (values) => void; thrown API errors are mapped onto fields
 */
export function useForm(initialValues, validate, onSubmit) {
  const [values, setValues] = useState(initialValues);
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const setField = (name, value) => {
    setValues((current) => ({ ...current, [name]: value }));
    if (errors[name]) setErrors((current) => ({ ...current, [name]: undefined }));
  };

  const bind = (name) => ({
    name,
    value: values[name] ?? '',
    onChange: (e) => setField(name, e.target.value),
    error: errors[name],
  });

  const handleSubmit = async (e) => {
    e?.preventDefault();
    const clientErrors = validate ? validate(values) : {};
    setErrors(clientErrors);
    setFormError(null);
    if (Object.keys(clientErrors).length > 0) return;

    setSubmitting(true);
    try {
      await onSubmit(values);
    } catch (err) {
      const fieldErrors = getFieldErrors(err);
      setErrors(fieldErrors);
      setFormError(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  return { values, setValues, setField, errors, formError, submitting, bind, handleSubmit };
}
