import assert from 'node:assert/strict';
import test from 'node:test';
import { sortByDate, sortByName, sortStrings } from './sort.js';

test('sortByName sorts case-insensitively and uses IDs as deterministic tie-breakers', () => {
  const records = [
    { id: 'b', name: 'alex' },
    { id: 'c', name: 'Alex' },
    { id: 'd', name: null },
    { id: 'a', name: 'Bea' },
  ];

  assert.deepEqual(sortByName(records).map((record) => record.id), ['b', 'c', 'a', 'd']);
  assert.deepEqual(records.map((record) => record.id), ['b', 'c', 'd', 'a']);
  assert.deepEqual(sortByName([]), []);
  assert.deepEqual(sortByName(null), []);
  assert.deepEqual(
    sortByName([{ name: 'Problem 10' }, { name: 'Problem 2' }]).map((record) => record.name),
    ['Problem 2', 'Problem 10'],
  );
});

test('sortByDate orders actual dates oldest first and puts missing dates last', () => {
  const records = [
    { id: 'later', timestamp: '2026-03-10T00:00:00Z' },
    { id: 'invalid', timestamp: 'not a date' },
    { id: 'earlier', timestamp: '2026-02-01T00:00:00Z' },
    { id: 'missing', timestamp: null },
  ];

  assert.deepEqual(sortByDate(records, (record) => record.timestamp).map((record) => record.id), [
    'earlier',
    'later',
    'invalid',
    'missing',
  ]);
  assert.deepEqual(records.map((record) => record.id), ['later', 'invalid', 'earlier', 'missing']);
});

test('sortStrings sorts names alphabetically without mutating the source array', () => {
  const values = ['beta', null, 'Alpha', 'alpha'];

  assert.deepEqual(sortStrings(values), ['Alpha', 'alpha', 'beta', null]);
  assert.deepEqual(values, ['beta', null, 'Alpha', 'alpha']);
});
