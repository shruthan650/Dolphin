import Badge from '../common/Badge';
import { formatDate, initials } from '../../utils/format';
import PaginationControls from '../common/PaginationControls';
import { usePagination } from '../../hooks/usePagination';
import { sortByName } from '../../utils/sort';
import Table from './Table';

/** Admin user listing. classLabel names the class count column; actions renders extra cells. */
export default function UsersTable({ users, showRole = false, classLabel = 'Classes', actions, actionsWidth = 76, resetKey }) {
  const columns = [
    {
      key: 'name',
      header: 'Name',
      width: showRole ? 22 : 25,
      render: (u) => (
        <div className="cell-user">
          <span className="avatar avatar-sm">{initials(u.name)}</span>
          <span className="cell-strong cell-truncate" title={u.name}>{u.name}</span>
        </div>
      ),
    },
    { key: 'email', header: 'Email', width: showRole ? 29 : 34, render: (u) => <span className="cell-truncate" title={u.email}>{u.email}</span> },
    showRole && { key: 'role', header: 'Role', width: 10, render: (u) => <Badge value={u.role} /> },
    classLabel && { key: 'classCount', header: classLabel, width: 10, className: 'cell-num' },
    {
      key: 'active',
      header: 'Status',
      width: 12,
      render: (u) => <Badge tone={u.active ? 'green' : 'red'}>{u.active ? 'Active' : 'Inactive'}</Badge>,
    },
    { key: 'createdAt', header: 'Created', width: showRole ? 15 : 15, render: (u) => formatDate(u.createdAt) },
    actions && { key: 'actions', header: <span className="sr-only">Actions</span>, className: 'cell-actions', fixedWidth: actionsWidth, render: actions },
  ].filter(Boolean);

  const sortedUsers = sortByName(users);
  const pagination = usePagination(sortedUsers, resetKey);

  return (
    <>
      <Table
        columns={columns}
        rows={pagination.pageItems}
        caption="Users"
        className={`users-table ${actions && actionsWidth > 76 ? 'users-table-wide-actions' : ''}`}
      />
      <PaginationControls {...pagination} onPageChange={pagination.setPage} className="table-pagination" />
    </>
  );
}
