import Icon from '../common/Icon';

export default function FormAlert({ message, tone = 'error' }) {
  if (!message) return null;
  return (
    <div className={`form-alert form-alert-${tone}`} role="alert">
      <Icon name={tone === 'error' ? 'alert' : 'check'} size={18} />
      <span>{message}</span>
    </div>
  );
}
