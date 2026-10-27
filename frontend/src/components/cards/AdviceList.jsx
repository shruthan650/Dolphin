import Button from '../common/Button';
import Icon from '../common/Icon';
import { timeAgo } from '../../utils/format';
import PaginationControls from '../common/PaginationControls';
import { usePagination } from '../../hooks/usePagination';
import { sortByDate } from '../../utils/sort';

/**
 * Teacher advice, oldest first.
 * showTarget: show which project / LeetCode problem each item is about.
 * canDelete(item) + onDelete(item): show a delete button (teachers, on their own advice).
 */
export default function AdviceList({ items, showTarget = true, canDelete, onDelete }) {
  const pagination = usePagination(sortByDate(items, (item) => item.createdAt));

  return (
    <>
      <ul className="advice-list">
        {pagination.pageItems.map((a) => (
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
      <PaginationControls {...pagination} onPageChange={pagination.setPage} />
    </>
  );
}
