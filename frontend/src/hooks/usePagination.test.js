import assert from 'node:assert/strict';
import test from 'node:test';
import { PAGE_SIZE, paginateItems } from './usePagination.js';

test('pagination returns at most ten items from the requested page', () => {
  const records = Array.from({ length: 23 }, (_, index) => index + 1);

  assert.equal(PAGE_SIZE, 10);
  assert.deepEqual(paginateItems(records, 0), [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);
  assert.deepEqual(paginateItems(records, 1), [11, 12, 13, 14, 15, 16, 17, 18, 19, 20]);
  assert.deepEqual(paginateItems(records, 2), [21, 22, 23]);
  assert.deepEqual(records, Array.from({ length: 23 }, (_, index) => index + 1));
});

test('pagination handles empty lists and negative page indexes', () => {
  assert.deepEqual(paginateItems([], 0), []);
  assert.deepEqual(paginateItems([1, 2], -1), [1, 2]);
});
