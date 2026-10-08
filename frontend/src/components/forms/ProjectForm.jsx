import { useState } from 'react';
import Button from '../common/Button';
import Icon from '../common/Icon';
import Input from '../common/Input';
import { validateOptionalUrl } from '../../utils/validation';
import ClassSelect from './ClassSelect';
import FormAlert from './FormAlert';
import { useForm } from './useForm';

const EMPTY = { title: '', description: '', githubUrl: '', liveUrl: '', technologies: [], classId: '' };

function validate(values) {
  const errors = {};
  if (!values.title.trim()) errors.title = 'Title is required';
  if (!values.classId) errors.classId = 'Choose the class this project belongs to';
  const github = validateOptionalUrl(values.githubUrl, 'GitHub URL');
  if (github) errors.githubUrl = github;
  const live = validateOptionalUrl(values.liveUrl, 'Live URL');
  if (live) errors.liveUrl = live;
  return errors;
}

/**
 * Student project form (create + edit). The owner is never sent; the backend takes it from the JWT.
 * classes are the student's joined classes; a single class is preselected.
 */
export default function ProjectForm({ initialValues, classes = [], submitLabel = 'Create project', onSubmit, onCancel }) {
  const start = initialValues
    ? {
        title: initialValues.title ?? '',
        description: initialValues.description ?? '',
        githubUrl: initialValues.githubUrl ?? '',
        liveUrl: initialValues.liveUrl ?? '',
        technologies: initialValues.technologies ?? [],
        classId: initialValues.classId ?? '',
      }
    : { ...EMPTY, classId: classes.length === 1 ? classes[0].id : '' };
  const { values, setField, bind, handleSubmit, submitting, formError, errors } = useForm(start, validate, (v) =>
    onSubmit({
      title: v.title.trim(),
      description: v.description.trim(),
      githubUrl: v.githubUrl.trim(),
      liveUrl: v.liveUrl.trim(),
      technologies: v.technologies,
      classId: v.classId,
    }),
  );
  const [techInput, setTechInput] = useState('');

  const addTech = () => {
    const tech = techInput.trim().replace(/,$/, '');
    if (!tech) return;
    if (!values.technologies.some((t) => t.toLowerCase() === tech.toLowerCase())) {
      setField('technologies', [...values.technologies, tech]);
    }
    setTechInput('');
  };

  const onTechKey = (e) => {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault();
      addTech();
    } else if (e.key === 'Backspace' && !techInput && values.technologies.length) {
      setField('technologies', values.technologies.slice(0, -1));
    }
  };

  const techError = errors.technologies || Object.entries(errors).find(([k]) => k.startsWith('technologies['))?.[1];

  return (
    <form onSubmit={handleSubmit} noValidate className="form">
      <FormAlert message={formError} />
      <ClassSelect classes={classes} {...bind('classId')} />
      <Input label="Project title" placeholder="Smart Campus" required maxLength={120} {...bind('title')} />
      <Input
        as="textarea"
        rows={4}
        label="Description"
        placeholder="What does the project do? What did you learn?"
        maxLength={2000}
        {...bind('description')}
      />
      <div className="form-row">
        <Input label="GitHub URL" type="url" placeholder="https://github.com/you/project" {...bind('githubUrl')} />
        <Input label="Live URL" type="url" placeholder="https://project.example.com" {...bind('liveUrl')} />
      </div>
      <div className={`field ${techError ? 'field-invalid' : ''}`}>
        <label className="field-label" htmlFor="tech-input">
          Technologies
        </label>
        <div className="tag-input">
          {values.technologies.map((tech) => (
            <span key={tech} className="tag">
              {tech}
              <button
                type="button"
                aria-label={`Remove ${tech}`}
                onClick={() => setField('technologies', values.technologies.filter((t) => t !== tech))}
              >
                <Icon name="x" size={12} />
              </button>
            </span>
          ))}
          <input
            id="tech-input"
            value={techInput}
            onChange={(e) => setTechInput(e.target.value)}
            onKeyDown={onTechKey}
            onBlur={addTech}
            placeholder={values.technologies.length ? '' : 'React, Spring Boot, Java…'}
          />
        </div>
        {techError ? <p className="field-error">{techError}</p> : <p className="field-hint">Press Enter or comma to add</p>}
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
