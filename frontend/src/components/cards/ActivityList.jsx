import { Link } from 'react-router-dom';
import Icon from '../common/Icon';
import { timeAgo } from '../../utils/format';
import PaginationControls from '../common/PaginationControls';
import { usePagination } from '../../hooks/usePagination';
import { sortByDate } from '../../utils/sort';

/** Teacher recent-activity feed built from the dashboard API. */
export default function ActivityList({ items }) {
  const pagination = usePagination(sortByDate(items, (item) => item.timestamp));

  return (
    <>
      <ul className="activity-list">
        {pagination.pageItems.map((item) => (
          <li key={`${item.type}-${item.id}`} className="activity-item">
            <span className={`activity-icon ${item.type === 'PROJECT' ? 'tone-indigo' : 'tone-teal'}`}>
              <Icon name={item.type === 'PROJECT' ? 'folder' : 'code'} size={16} />
            </span>
            <div className="activity-text">
              <p>
                <Link to={`/teacher/students/${item.studentId}`} className="activity-student">
                  {item.studentName || 'Student'}
                </Link>{' '}
                <span className="muted">{item.type === 'PROJECT' ? item.detail.toLowerCase() : 'logged'}</span>{' '}
                <strong>{item.title}</strong>
              </p>
              {item.type === 'LEETCODE' && <p className="activity-detail">{item.detail}</p>}
            </div>
            <time className="activity-time" dateTime={item.timestamp}>
              {timeAgo(item.timestamp)}
            </time>
          </li>
        ))}
      </ul>
      <PaginationControls {...pagination} onPageChange={pagination.setPage} />
    </>
  );
}
