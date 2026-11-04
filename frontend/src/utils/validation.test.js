import test from 'node:test';
import assert from 'node:assert/strict';
import { isEmail, normalizeEmail } from './validation.js';

test('accepts college and generic email addresses', () => {
  assert.equal(isEmail('4MT24CS001@mite.ac.in'), true);
  assert.equal(isEmail('student@gmail.com'), true);
  assert.equal(isEmail('teacher@outlook.com'), true);
});

test('college email matching is case-insensitive and trims whitespace', () => {
  assert.equal(isEmail('  4mt24ai001@MITE.AC.IN  '), true);
  assert.equal(normalizeEmail('  4MT24AI001@MITE.AC.IN  '), '4mt24ai001@mite.ac.in');
});

test('rejects invalid college structures, incorrect college domains, and malformed email', () => {
  assert.equal(isEmail('4MT24C001@mite.ac.in'), false);
  assert.equal(isEmail('4MT24CS001@mite.com'), false);
  assert.equal(isEmail('student@mite.ac.in'), false);
  assert.equal(isEmail('missing-domain'), false);
  assert.equal(isEmail('student@@gmail.com'), false);
});
