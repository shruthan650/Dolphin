import { useNavigate } from 'react-router-dom';
import Button from '../common/Button';
import { initials } from '../../utils/format';
import PaginationControls from '../common/PaginationControls';
import { usePagination } from '../../hooks/usePagination';
import { sortByName, sortStrings } from '../../utils/sort';
import Table from './Table';

/** Teacher-facing student list with project and LeetCode progress. */
/** removeLabel(student) names the trash button's action, e.g. removing from a class or deleting the account. */
export default function StudentProgressTable({
  students,
  showClasses = false,
  onRemove,
  resetKey,
  removeLabel = (s) => `Remove ${s.name} from class`,
}) {
  const navigate = useNavigate();
  const columns = [
    {
      key: 'name',
      header: 'Name',
      width: showClasses ? 20 : 27,
      render: (s) => (
        <div className="cell-user">
          <span className="avatar avatar-sm">{initials(s.name)}</span>
          <span className="cell-strong cell-truncate" title={s.name}>{s.name}</span>
        </div>
      ),
    },
    { key: 'email', header: 'Email', width: showClasses ? 23 : 30, render: (s) => <span className="cell-truncate" title={s.email}>{s.email}</span> },
    showClasses && { key: 'classNames', header: 'Classes', width: 16, render: (s) => <span className="cell-truncate" title={sortStrings(s.classNames).join(', ')}>{sortStrings(s.classNames).join(', ')}</span> },
    { key: 'projectCount', header: 'Projects', width: showClasses ? 8 : 12, className: 'cell-num' },
    {
      key: 'leetcode',
      header: 'LeetCode progress',
      width: showClasses ? 18 : 21,
      render: (s) => {
        const pct = s.leetCodeTotal ? Math.round((s.leetCodeSolved / s.leetCodeTotal) * 100) : 0;
        return (
          <div className="progress-cell">
            <div className="progress-bar" aria-hidden="true">
              <span style={{ width: `${pct}%` }} />
            </div>
            <span className="progress-text" title={`${s.leetCodeSolved} solved / ${s.leetCodeTotal}`}>
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
      fixedWidth: 136,
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

  const pagination = usePagination(sortByName(students), resetKey);

  return (
    <>
      <Table
        columns={columns}
        rows={pagination.pageItems}
        caption="Students"
        className="student-progress-table"
        onRowClick={(s) => navigate(`/teacher/students/${s.id}`)}
      />
      <PaginationControls {...pagination} onPageChange={pagination.setPage} className="table-pagination" />
    </>
  );
}
