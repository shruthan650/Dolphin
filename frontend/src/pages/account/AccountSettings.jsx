import Badge from '../../components/common/Badge';
import Card from '../../components/common/Card';
import PageHeader from '../../components/common/PageHeader';
import {
  ChangeEmailForm,
  ChangePasswordForm,
  DeleteAccountSection,
  ProfileDetailsForm,
} from '../../components/forms/AccountForms';
import { useAuth } from '../../hooks/useAuth';
import { initials } from '../../utils/format';

/** Name, email, password and (except for admins) account deletion. Shared by every role. */
export function AccountSections() {
  const { user } = useAuth();
  return (
    <>
      <div className="grid-2">
        <Card title="Profile" subtitle="Your name as teachers, students and admins see it">
          <ProfileDetailsForm key={user?.name} />
        </Card>
        <Card title="Email" subtitle="Your email is also your sign-in">
          <ChangeEmailForm />
        </Card>
      </div>
      <Card title="Password" subtitle="Use at least 8 characters">
        <ChangePasswordForm />
      </Card>
      {user?.role !== 'ADMIN' && (
        <Card title="Danger zone" className="card-danger">
          <DeleteAccountSection />
        </Card>
      )}
    </>
  );
}

/** Account page for admins and teachers (students have it inside their Profile page). */
export default function AccountSettings() {
  const { user } = useAuth();
  return (
    <>
      <PageHeader title="Profile" subtitle="Manage your account" />
      <div className="profile-header">
        <span className="avatar avatar-lg">{initials(user?.name)}</span>
        <div className="profile-header-text">
          <h2 className="profile-name">{user?.name}</h2>
          <p className="muted break-anywhere">{user?.email}</p>
          <div className="chip-row">
            <Badge value={user?.role} />
          </div>
        </div>
      </div>
      <AccountSections />
    </>
  );
}
