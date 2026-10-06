const TONES = {
  EASY: 'green',
  MEDIUM: 'amber',
  HARD: 'red',
  SOLVED: 'green',
  ATTEMPTED: 'amber',
  IN_PROGRESS: 'blue',
  ADMIN: 'violet',
  TEACHER: 'blue',
  STUDENT: 'slate',
};

const LABELS = {
  IN_PROGRESS: 'In progress',
};

function humanize(value) {
  if (LABELS[value]) return LABELS[value];
  return value.charAt(0) + value.slice(1).toLowerCase();
}

/** Renders a status pill. Pass `value` for known enums or `tone` + children for custom badges. */
export default function Badge({ value, tone, children }) {
  const resolvedTone = tone || TONES[value] || 'slate';
  return <span className={`badge badge-${resolvedTone}`}>{children ?? (value ? humanize(value) : '')}</span>;
}
