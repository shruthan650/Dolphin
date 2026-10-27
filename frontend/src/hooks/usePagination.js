import { useEffect, useMemo, useState } from 'react';

export const PAGE_SIZE = 10;

export function paginateItems(items, page) {
  const start = Math.max(page, 0) * PAGE_SIZE;
  return items.slice(start, start + PAGE_SIZE);
}

export function usePagination(items, resetKey) {
  const [page, setPage] = useState(0);
  const totalItems = items.length;
  const pageCount = Math.ceil(totalItems / PAGE_SIZE);
  const currentPage = Math.min(page, Math.max(pageCount - 1, 0));

  useEffect(() => {
    setPage(0);
  }, [resetKey]);

  const pageItems = useMemo(
    () => paginateItems(items, currentPage),
    [items, currentPage],
  );

  return { pageItems, page: currentPage, setPage, pageCount, totalItems };
}
