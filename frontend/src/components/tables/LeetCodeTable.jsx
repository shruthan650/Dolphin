import { Link } from 'react-router-dom';
import Badge from '../common/Badge';
import Button from '../common/Button';
import Icon from '../common/Icon';
import { formatDate } from '../../utils/format';
import Table from './Table';

/**
 * LeetCode entries. showStudent adds a student column (teacher view); onEdit/onDelete add owner actions.
 * classLabel(entry) adds a Class column (null = unassigned).
 */
export default function LeetCodeTable({ entries, showStudent = false, classLabel, onEdit, onDelete }) {
  const columns = [
    {
      key: 'problemName',
      header: 'Problem',
      render: (e) =>
        e.problemUrl ? (
          <a href={e.problemUrl} target="_blank" rel="noopener noreferrer" className="cell-link">
            {e.problemName} <Icon name="external" size={13} />
          </a>
        ) : (
          <span className="cell-strong">{e.problemName}</span>
        ),
    },
    showStudent && {
      key: 'studentName',
      header: 'Student',
      render: (e) => <Link to={`/teacher/students/${e.studentId}`}>{e.studentName}</Link>,
    },
    classLabel && {
      key: 'class',
      header: 'Class',
      render: (e) => classLabel(e) || <span className="muted">Unassigned</span>,
    },
    { key: 'difficulty', header: 'Difficulty', render: (e) => <Badge value={e.difficulty} /> },
    { key: 'status', header: 'Status', render: (e) => <Badge value={e.status} /> },
    { key: 'topic', header: 'Topic', render: (e) => e.topic || <span className="muted">—</span> },
    { key: 'solvedAt', header: 'Solved', render: (e) => formatDate(e.solvedAt) },
    (onEdit || onDelete) && {
      key: 'actions',
      header: <span className="sr-only">Actions</span>,
      className: 'cell-actions',
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

  return <Table columns={columns} rows={entries} caption="LeetCode problems" />;
}
