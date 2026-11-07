import Button from './Button';
import { PAGE_SIZE } from '../../hooks/usePagination';

export default function PaginationControls({ page, pageCount, totalItems, onPageChange, className = '', pageSize = PAGE_SIZE }) {
  if (pageCount <= 1) return null;

  const firstItem = page * pageSize + 1;
  const lastItem = Math.min((page + 1) * pageSize, totalItems);

  return (
    <nav className={`pagination ${className}`} aria-label="Pagination">
      <span className="muted">
        Showing {firstItem}–{lastItem} of {totalItems}
      </span>
      <div className="row-actions">
        <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => onPageChange(page - 1)}>
          Previous
        </Button>
        <Button variant="secondary" size="sm" disabled={page + 1 >= pageCount} onClick={() => onPageChange(page + 1)}>
          Next
        </Button>
      </div>
    </nav>
  );
}
