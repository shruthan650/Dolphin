// Client-side checks for fast feedback only. The backend re-validates everything.

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const URL_RE = /^https?:\/\/\S+$/i;
// Kept in sync with ProfileLinkPatterns on the backend.
const GITHUB_PROFILE_RE = /^https?:\/\/(www\.)?github\.com\/[A-Za-z0-9][A-Za-z0-9-]{0,38}\/?$/;
const LEETCODE_PROFILE_RE = /^https?:\/\/(www\.)?leetcode\.(com|cn)\/(u\/)?[A-Za-z0-9_.-]{1,50}\/?$/;

export const isEmail = (value) => EMAIL_RE.test(value.trim());
export const isHttpUrl = (value) => URL_RE.test(value.trim());

/** Validates name/email/password/confirmPassword account forms. */
export function validateAccount(values) {
  const errors = {};
  if (!values.name.trim()) errors.name = 'Name is required';
  if (!values.email.trim()) errors.email = 'Email is required';
  else if (!isEmail(values.email)) errors.email = 'Enter a valid email address';
  if (values.password.length < 8) errors.password = 'Password must be at least 8 characters';
  if (values.confirmPassword !== values.password) errors.confirmPassword = 'Passwords do not match';
  return errors;
}

export function validateOptionalUrl(value, label) {
  if (value && value.trim() && !isHttpUrl(value)) return `${label} must start with http:// or https://`;
  return null;
}

/** Validates the required GitHub and LeetCode profile URLs of a student. */
export function validateProfileLinks(values) {
  const errors = {};
  const github = values.githubUrl.trim();
  const leetCode = values.leetCodeUrl.trim();
  if (!github) errors.githubUrl = 'GitHub profile URL is required';
  else if (!GITHUB_PROFILE_RE.test(github)) errors.githubUrl = 'Enter your GitHub profile URL, e.g. https://github.com/username';
  if (!leetCode) errors.leetCodeUrl = 'LeetCode profile URL is required';
  else if (!LEETCODE_PROFILE_RE.test(leetCode)) errors.leetCodeUrl = 'Enter your LeetCode profile URL, e.g. https://leetcode.com/u/username';
  return errors;
}

/** True for a signed-in student whose GitHub or LeetCode profile URL is not on file yet. */
export const needsProfileLinks = (user) => user?.role === 'STUDENT' && (!user.githubUrl || !user.leetCodeUrl);
