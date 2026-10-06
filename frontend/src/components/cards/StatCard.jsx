import Icon from '../common/Icon';

/** tone: indigo | teal | amber | rose | slate */
export default function StatCard({ label, value, icon, tone = 'indigo', hint }) {
  return (
    <div className="stat-card">
      <div className={`stat-icon tone-${tone}`}>
        <Icon name={icon} size={20} />
      </div>
      <div className="stat-content">
        <p className="stat-label">{label}</p>
        <p className="stat-value">{value ?? '—'}</p>
        {hint && <p className="stat-hint">{hint}</p>}
      </div>
    </div>
  );
}
