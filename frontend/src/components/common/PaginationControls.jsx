import Button from './Button';
import { PAGE_SIZE } from '../../hooks/usePagination';

export default function PaginationControls({ page, pageCount, totalItems, onPageChange, className = '' }) {
  if (pageCount <= 1) return null;

  const firstItem = page * PAGE_SIZE + 1;
  const lastItem = Math.min((page + 1) * PAGE_SIZE, totalItems);

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
