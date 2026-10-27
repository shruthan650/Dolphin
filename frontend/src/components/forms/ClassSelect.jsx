import Input from '../common/Input';
import PaginationControls from '../common/PaginationControls';
import { usePagination } from '../../hooks/usePagination';
import { sortByName } from '../../utils/sort';

/** Required class picker for a student's own work (the backend checks they are enrolled). */
export default function ClassSelect({ classes, ...props }) {
  const pagination = usePagination(sortByName(classes, (item) => item?.className));

  return (
    <>
      <Input as="select" label="Class" required hint="This work belongs to the selected class" {...props}>
        <option value="" disabled>
          Select a class
        </option>
        {pagination.pageItems.map((c) => (
          <option key={c.id} value={c.id}>
            {c.className}
          </option>
        ))}
      </Input>
      <PaginationControls {...pagination} onPageChange={pagination.setPage} />
    </>
  );
}
