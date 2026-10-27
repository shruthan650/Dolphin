import assert from 'node:assert/strict';
import test from 'node:test';
import { canViewLeetCodeStreak } from './streakVisibility.js';

test('students see a streak only in their own profile', () => {
  assert.equal(canViewLeetCodeStreak('STUDENT', 'own-student-profile'), true);
  assert.equal(canViewLeetCodeStreak('STUDENT', 'authorized-student-profile'), false);
});

test('teachers see a streak only in an authorized student profile', () => {
  assert.equal(canViewLeetCodeStreak('TEACHER', 'authorized-student-profile'), true);
  assert.equal(canViewLeetCodeStreak('TEACHER', 'own-student-profile'), false);
});

test('admins cannot see a streak in any profile context', () => {
  assert.equal(canViewLeetCodeStreak('ADMIN', 'own-student-profile'), false);
  assert.equal(canViewLeetCodeStreak('ADMIN', 'authorized-student-profile'), false);
  assert.equal(canViewLeetCodeStreak('ADMIN', 'admin-profile'), false);
});
