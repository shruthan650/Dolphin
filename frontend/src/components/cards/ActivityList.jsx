import { Link } from 'react-router-dom';
import Icon from '../common/Icon';
import { timeAgo } from '../../utils/format';

/** Teacher recent-activity feed built from the dashboard API. */
export default function ActivityList({ items }) {
  return (
    <ul className="activity-list">
      {items.map((item) => (
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
  );
}
