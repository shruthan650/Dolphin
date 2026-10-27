import { Link } from 'react-router-dom';
import Badge from '../common/Badge';
import Button from '../common/Button';
import Icon from '../common/Icon';
import { formatDate } from '../../utils/format';
import PaginationControls from '../common/PaginationControls';
import { usePagination } from '../../hooks/usePagination';
import { sortByName } from '../../utils/sort';
import Table from './Table';

/**
 * LeetCode entries. showStudent adds a student column (teacher view); onEdit/onDelete add owner actions.
 * classLabel(entry) adds a Class column (null = unassigned).
 */
export default function LeetCodeTable({ entries, showStudent = false, classLabel, onEdit, onDelete, resetKey }) {
  const columns = [
    {
      key: 'problemName',
      header: 'Problem',
      width: showStudent ? 21 : 25,
      render: (e) =>
        e.problemUrl ? (
          <a href={e.problemUrl} target="_blank" rel="noopener noreferrer" className="cell-link">
            <span className="cell-truncate" title={e.problemName}>{e.problemName}</span> <Icon name="external" size={13} />
          </a>
        ) : (
          <span className="cell-strong cell-truncate" title={e.problemName}>{e.problemName}</span>
        ),
    },
    showStudent && {
      key: 'studentName',
      header: 'Student',
      width: 17,
      render: (e) => <Link to={`/teacher/students/${e.studentId}`}>{e.studentName}</Link>,
    },
    classLabel && {
      key: 'class',
      header: 'Class',
      width: 16,
      render: (e) => classLabel(e) || <span className="muted">Unassigned</span>,
    },
    { key: 'difficulty', header: 'Difficulty', width: 10, render: (e) => <Badge value={e.difficulty} /> },
    { key: 'status', header: 'Status', width: 12, render: (e) => <Badge value={e.status} /> },
    { key: 'topic', header: 'Topic', width: 12, render: (e) => <span className="cell-truncate" title={e.topic}>{e.topic || <span className="muted">—</span>}</span> },
    { key: 'solvedAt', header: 'Solved', width: 14, render: (e) => formatDate(e.solvedAt) },
    (onEdit || onDelete) && {
      key: 'actions',
      header: <span className="sr-only">Actions</span>,
      className: 'cell-actions',
      fixedWidth: 104,
      render: (e) => (
        <div className="row-actions">
          {onEdit && <Button variant="ghost" size="sm" icon="edit" onClick={() => onEdit(e)} aria-label={`Edit ${e.problemName}`} />}
          {onDelete && (
            <Button variant="ghost" size="sm" icon="trash" className="btn-danger-ghost" onClick={() => onDelete(e)} aria-label={`Delete ${e.problemName}`} />
          )}
        </div>
      ),
    },
  ].filter(Boolean);

  const sortedEntries = sortByName(
    entries,
    (entry) => entry?.problemName,
    (entry) => `${entry?.studentName ?? ''}\u0000${entry?.id ?? ''}`,
  );
  const pagination = usePagination(sortedEntries, resetKey);

  return (
    <>
      <Table columns={columns} rows={pagination.pageItems} caption="LeetCode problems" className="leetcode-table" />
      <PaginationControls {...pagination} onPageChange={pagination.setPage} className="table-pagination" />
    </>
  );
}
