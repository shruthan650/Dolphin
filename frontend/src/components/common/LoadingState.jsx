/** variant: 'spinner' (default) | 'cards' | 'table' skeletons */
export default function LoadingState({ label = 'Loading…', variant = 'spinner', count = 4 }) {
  if (variant === 'cards') {
    return (
      <div className="stat-grid" aria-busy="true" aria-label={label}>
        {Array.from({ length: count }).map((_, i) => (
          <div key={i} className="skeleton skeleton-card" />
        ))}
      </div>
    );
  }
  if (variant === 'table') {
    return (
      <div className="skeleton-table" aria-busy="true" aria-label={label}>
        {Array.from({ length: count }).map((_, i) => (
          <div key={i} className="skeleton skeleton-row" />
        ))}
      </div>
    );
  }
  return (
    <div className="loading-state" role="status">
      <span className="spinner" aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}
