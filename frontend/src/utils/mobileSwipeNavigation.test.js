import test from 'node:test';
import assert from 'node:assert/strict';
import { getStudentSwipeDestination } from './mobileSwipeNavigation.js';

test('student swipes move to adjacent pages without wrapping', () => {
  assert.equal(getStudentSwipeDestination('/student/dashboard', -100, 0), '/student/class');
  assert.equal(getStudentSwipeDestination('/student/class', -100, 0), '/student/projects');
  assert.equal(getStudentSwipeDestination('/student/projects', -100, 0), '/student/leetcode');
  assert.equal(getStudentSwipeDestination('/student/leetcode', -100, 0), null);
  assert.equal(getStudentSwipeDestination('/student/dashboard', 100, 0), null);
  assert.equal(getStudentSwipeDestination('/student/leetcode', 100, 0), '/student/projects');
});

test('project subroutes participate as the Projects tab', () => {
  assert.equal(getStudentSwipeDestination('/student/projects/create', -100, 0), '/student/leetcode');
  assert.equal(getStudentSwipeDestination('/student/projects/123/edit', 100, 0), '/student/class');
});

test('short, vertical, and Profile gestures do not navigate', () => {
  assert.equal(getStudentSwipeDestination('/student/dashboard', -40, 0), null);
  assert.equal(getStudentSwipeDestination('/student/dashboard', -100, 100), null);
  assert.equal(getStudentSwipeDestination('/student/profile', -100, 0), null);
});
