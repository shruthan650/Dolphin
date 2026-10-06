import { useNavigate } from 'react-router-dom';
import Button from '../common/Button';
import { initials } from '../../utils/format';
import Table from './Table';

/** Teacher-facing student list with project and LeetCode progress. */
/** removeLabel(student) names the trash button's action, e.g. removing from a class or deleting the account. */
export default function StudentProgressTable({
  students,
  showClasses = false,
  onRemove,
  removeLabel = (s) => `Remove ${s.name} from class`,
}) {
  const navigate = useNavigate();
  const columns = [
    {
      key: 'name',
      header: 'Name',
      render: (s) => (
        <div className="cell-user">
          <span className="avatar avatar-sm">{initials(s.name)}</span>
          <span className="cell-strong">{s.name}</span>
        </div>
      ),
    },
    { key: 'email', header: 'Email' },
    showClasses && { key: 'classNames', header: 'Classes', render: (s) => s.classNames.join(', ') },
    { key: 'projectCount', header: 'Projects', className: 'cell-num' },
    {
      key: 'leetcode',
      header: 'LeetCode progress',
      render: (s) => {
        const pct = s.leetCodeTotal ? Math.round((s.leetCodeSolved / s.leetCodeTotal) * 100) : 0;
        return (
          <div className="progress-cell">
            <div className="progress-bar" aria-hidden="true">
              <span style={{ width: `${pct}%` }} />
            </div>
            <span className="progress-text">
              {s.leetCodeSolved} solved / {s.leetCodeTotal}
            </span>
          </div>
        );
      },
    },
    {
      key: 'actions',
      header: <span className="sr-only">Actions</span>,
      className: 'cell-actions',
      render: (s) => (
        <div className="row-actions">
          <Button variant="secondary" size="sm" to={`/teacher/students/${s.id}`} onClick={(e) => e.stopPropagation()}>
            View
          </Button>
          {onRemove && (
            <Button
              variant="ghost"
              size="sm"
              icon="trash"
              className="btn-danger-ghost"
              aria-label={removeLabel(s)}
              title={removeLabel(s)}
              onClick={(e) => {
                e.stopPropagation();
                onRemove(s);
              }}
            />
          )}
        </div>
      ),
    },
  ].filter(Boolean);

  return (
    <Table
      columns={columns}
      rows={students}
      caption="Students"
      onRowClick={(s) => navigate(`/teacher/students/${s.id}`)}
    />
  );
}
