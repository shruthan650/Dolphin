import Badge from '../common/Badge';
import { formatDate, initials } from '../../utils/format';
import Table from './Table';

/** Admin user listing. classLabel names the class count column; actions renders extra cells. */
export default function UsersTable({ users, showRole = false, classLabel = 'Classes', actions }) {
  const columns = [
    {
      key: 'name',
      header: 'Name',
      render: (u) => (
        <div className="cell-user">
          <span className="avatar avatar-sm">{initials(u.name)}</span>
          <span className="cell-strong">{u.name}</span>
        </div>
      ),
    },
    { key: 'email', header: 'Email' },
    showRole && { key: 'role', header: 'Role', render: (u) => <Badge value={u.role} /> },
    classLabel && { key: 'classCount', header: classLabel, className: 'cell-num' },
    {
      key: 'active',
      header: 'Status',
      render: (u) => <Badge tone={u.active ? 'green' : 'red'}>{u.active ? 'Active' : 'Inactive'}</Badge>,
    },
    { key: 'createdAt', header: 'Created', render: (u) => formatDate(u.createdAt) },
    actions && { key: 'actions', header: <span className="sr-only">Actions</span>, className: 'cell-actions', render: actions },
  ].filter(Boolean);

  return <Table columns={columns} rows={users} caption="Users" />;
}
