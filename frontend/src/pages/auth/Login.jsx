import { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import Button from '../../components/common/Button';
import Icon from '../../components/common/Icon';
import Input from '../../components/common/Input';
import ThemeToggle from '../../components/common/ThemeToggle';
import FormAlert from '../../components/forms/FormAlert';
import ProfileLinksForm, { ProfileLinkFields } from '../../components/forms/ProfileLinksForm';
import { useForm } from '../../components/forms/useForm';
import { useAuth } from '../../hooks/useAuth';
import { ROLE_HOME } from '../../utils/format';
import { isEmail, needsProfileLinks, validateAccount, validateProfileLinks } from '../../utils/validation';

function validateLogin(values) {
  const errors = {};
  if (!values.email.trim()) errors.email = 'Email is required';
  else if (!isEmail(values.email)) errors.email = 'Enter a valid email address';
  if (!values.password) errors.password = 'Password is required';
  return errors;
}

const validateRegistration = (values) => ({ ...validateAccount(values), ...validateProfileLinks(values) });

/** Only redirect back to a page that belongs to the signed-in role. */
function destinationFor(role, from) {
  const home = ROLE_HOME[role];
  const prefix = home.split('/')[1];
  return from && from.startsWith(`/${prefix}/`) ? from : home;
}

function SignInForm({ onSuccess }) {
  const { login } = useAuth();
  const { bind, handleSubmit, submitting, formError } = useForm({ email: '', password: '' }, validateLogin, async (v) =>
    onSuccess(await login(v.email.trim(), v.password)),
  );
  return (
    <form onSubmit={handleSubmit} noValidate className="form">
      <FormAlert message={formError} />
      <Input label="Email" type="email" autoComplete="email" placeholder="you@college.edu" {...bind('email')} />
      <Input label="Password" type="password" autoComplete="current-password" placeholder="••••••••" {...bind('password')} />
      <Button type="submit" loading={submitting} className="btn-block">
        Sign in
      </Button>
    </form>
  );
}

function RegisterForm({ onSuccess }) {
  const { register } = useAuth();
  const { bind, handleSubmit, submitting, formError } = useForm(
    { name: '', email: '', password: '', confirmPassword: '', githubUrl: '', leetCodeUrl: '' },
    validateRegistration,
    async (v) =>
      onSuccess(
        await register({
          ...v,
          name: v.name.trim(),
          email: v.email.trim(),
          githubUrl: v.githubUrl.trim(),
          leetCodeUrl: v.leetCodeUrl.trim(),
        }),
      ),
  );
  return (
    <form onSubmit={handleSubmit} noValidate className="form">
      <FormAlert message={formError} />
      <Input label="Full name" autoComplete="name" placeholder="Rahul Sharma" {...bind('name')} />
      <Input label="Email" type="email" autoComplete="email" placeholder="you@college.edu" {...bind('email')} />
      <Input label="Password" type="password" autoComplete="new-password" hint="At least 8 characters" {...bind('password')} />
      <Input label="Confirm password" type="password" autoComplete="new-password" {...bind('confirmPassword')} />
      <ProfileLinkFields bind={bind} />
      <Button type="submit" loading={submitting} className="btn-block">
        Create student account
      </Button>
    </form>
  );
}

function AuthForms({ onSuccess, sessionMessage }) {
  const [mode, setMode] = useState('signin');
  return (
    <>
      <h2 className="auth-title">{mode === 'signin' ? 'Welcome back' : 'Create your student account'}</h2>
      <p className="auth-subtitle">
        {mode === 'signin'
          ? 'Sign in with the account provided by your institution.'
          : 'Teachers are added by an administrator. Students can sign up here.'}
      </p>

      <div className="segmented" role="tablist" aria-label="Authentication mode">
        <button type="button" role="tab" aria-selected={mode === 'signin'} className={mode === 'signin' ? 'active' : ''} onClick={() => setMode('signin')}>
          Sign in
        </button>
        <button type="button" role="tab" aria-selected={mode === 'register'} className={mode === 'register' ? 'active' : ''} onClick={() => setMode('register')}>
          Student sign up
        </button>
      </div>

      {sessionMessage && mode === 'signin' && <FormAlert message={sessionMessage} />}
      {mode === 'signin' ? <SignInForm onSuccess={onSuccess} /> : <RegisterForm onSuccess={onSuccess} />}
    </>
  );
}

/** Shown after sign-in to a student whose GitHub/LeetCode profile URLs are not on file yet. */
function ProfileLinksStep({ user, onSaved }) {
  const { logout } = useAuth();
  return (
    <>
      <h2 className="auth-title">Add your coding profiles</h2>
      <p className="auth-subtitle">
        Hi {user.name}, enter your GitHub and LeetCode profile URLs so your teachers can follow your progress.
      </p>
      <ProfileLinksForm initialValues={user} submitLabel="Save and continue" onSaved={onSaved} />
      <Button variant="ghost" className="btn-block" onClick={logout}>
        Sign out
      </Button>
    </>
  );
}

export default function Login() {
  const { isAuthenticated, user, sessionMessage } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = location.state?.from?.pathname;
  const linksStep = isAuthenticated && needsProfileLinks(user);

  if (isAuthenticated && !linksStep) {
    return <Navigate to={destinationFor(user.role, from)} replace />;
  }

  // A student without profile links stays here (linksStep) until they are saved.
  const onSuccess = (sessionUser) => {
    if (!needsProfileLinks(sessionUser)) navigate(destinationFor(sessionUser.role, from), { replace: true });
  };

  return (
    <div className="auth-page">
      <section className="auth-brand" aria-hidden="true">
        <div className="auth-brand-inner">
          <div className="auth-logo">
            <img src="/favicon.svg" alt="" width="40" height="40" />
            <span>Dolphin</span>
          </div>
          <h1>Track every student's technical growth in one place.</h1>
          <ul className="auth-points">
            <li>
              <Icon name="book" size={18} /> Classes with shareable join codes
            </li>
            <li>
              <Icon name="folder" size={18} /> Student project portfolios
            </li>
            <li>
              <Icon name="code" size={18} /> LeetCode problem-solving progress
            </li>
          </ul>
        </div>
      </section>

      <section className="auth-panel">
        <ThemeToggle className="auth-theme-toggle" />
        <div className="auth-card">
          <div className="auth-logo auth-logo-mobile">
            <img src="/favicon.svg" alt="" width="32" height="32" />
            <span>Dolphin</span>
          </div>
          {linksStep ? (
            <ProfileLinksStep user={user} onSaved={onSuccess} />
          ) : (
            <AuthForms onSuccess={onSuccess} sessionMessage={sessionMessage} />
          )}
        </div>
      </section>
    </div>
  );
}
