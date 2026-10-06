import Button from './Button';
import Icon from './Icon';

export default function ErrorState({ title = 'Something went wrong', message, onRetry }) {
  return (
    <div className="error-state" role="alert">
      <div className="error-icon">
        <Icon name="alert" size={24} />
      </div>
      <h3 className="empty-title">{title}</h3>
      {message && <p className="empty-message">{message}</p>}
      {onRetry && (
        <Button variant="secondary" onClick={onRetry}>
          Try again
        </Button>
      )}
    </div>
  );
}
