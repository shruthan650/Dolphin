import Button from '../common/Button';
import Icon from '../common/Icon';
import { timeAgo } from '../../utils/format';

/**
 * Teacher advice, newest first.
 * showTarget: show which project / LeetCode problem each item is about.
 * canDelete(item) + onDelete(item): show a delete button (teachers, on their own advice).
 */
export default function AdviceList({ items, showTarget = true, canDelete, onDelete }) {
  return (
    <ul className="advice-list">
      {items.map((a) => (
        <li key={a.id} className="advice-item">
          <span className={`advice-icon ${a.targetType === 'PROJECT' ? 'tone-indigo' : 'tone-teal'}`} aria-hidden="true">
            <Icon name={a.targetType === 'PROJECT' ? 'folder' : 'code'} size={15} />
          </span>
          <div className="advice-body">
            <p className="advice-meta">
              <strong>{a.teacherName || 'Teacher'}</strong>
              {showTarget && (
                <>
                  <span className="muted"> on {a.targetType === 'PROJECT' ? 'project' : 'problem'} </span>
                  <strong>{a.targetTitle || 'a deleted item'}</strong>
                </>
              )}
              <time className="advice-time" dateTime={a.createdAt}>
                {timeAgo(a.createdAt)}
              </time>
            </p>
            <p className="advice-message">{a.message}</p>
          </div>
          {onDelete && canDelete?.(a) && (
            <Button
              variant="ghost"
              size="sm"
              icon="trash"
              className="btn-danger-ghost"
              aria-label="Delete advice"
              title="Delete advice"
              onClick={() => onDelete(a)}
            />
          )}
        </li>
      ))}
    </ul>
  );
}
