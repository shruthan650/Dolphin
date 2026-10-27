import Card from '../common/Card';

const ACTIVITY_DAYS = 84;

function isoDate(date) {
  return date.toISOString().slice(0, 10);
}

function parseDate(value) {
  return new Date(`${value}T00:00:00Z`);
}

function formatDate(value) {
  return parseDate(value).toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    timeZone: 'UTC',
  });
}

function weekday(value) {
  return parseDate(value).toLocaleDateString(undefined, { weekday: 'short', timeZone: 'UTC' });
}

function recentWeek(today) {
  const startOfWeek = new Date(`${today}T00:00:00Z`);
  startOfWeek.setUTCDate(startOfWeek.getUTCDate() - startOfWeek.getUTCDay());
  return Array.from({ length: 7 }, (_, index) => {
    const date = new Date(startOfWeek);
    date.setUTCDate(date.getUTCDate() + index);
    return isoDate(date);
  });
}

export default function LeetCodeStreakCard({ streak }) {
  const activityCounts = new Map((streak?.recentActivity ?? []).map(({ date, solvedCount }) => [date, solvedCount]));
  const today = streak?.recentActivity?.at(-1)?.date ?? isoDate(new Date());
  const week = recentWeek(today);
  const heatmap = Array.from({ length: ACTIVITY_DAYS }, (_, index) => {
    const date = new Date(`${today}T00:00:00Z`);
    date.setUTCDate(date.getUTCDate() - (ACTIVITY_DAYS - 1 - index));
    const dateString = isoDate(date);
    return { date: dateString, solvedCount: activityCounts.get(dateString) ?? 0 };
  });
  const firstDate = heatmap[0].date;
  const currentStreak = streak?.currentStreak ?? 0;

  return (
    <Card title="LeetCode daily streak" subtitle="Your solving activity, based on recorded solved dates" className="streak-card">
      <div className="streak-summary">
        <div className="streak-current">
          <span className="streak-flame" aria-hidden="true">🔥</span>
          <div>
            <p className="streak-count">{currentStreak} day{currentStreak === 1 ? '' : 's'}</p>
            <p className="streak-label">current streak</p>
          </div>
        </div>
        <p className="streak-message">
          {currentStreak > 0 ? 'Keep solving to keep your streak alive!' : 'Solve a problem to start a new streak.'}
        </p>
        <dl className="streak-stats">
          <div>
            <dt>Longest</dt>
            <dd>{streak?.longestStreak ?? 0} days</dd>
          </div>
          <div>
            <dt>Active days</dt>
            <dd>{streak?.totalActiveDays ?? 0}</dd>
          </div>
          <div>
            <dt>Last solved</dt>
            <dd>{streak?.mostRecentSolvedDate ? formatDate(streak.mostRecentSolvedDate) : 'Not yet'}</dd>
          </div>
        </dl>
      </div>

      <div className="streak-week" aria-label="This week, Sunday through Saturday">
        {week.map((date) => {
          const solved = (activityCounts.get(date) ?? 0) > 0;
          const isToday = date === today;
          return (
            <div className={`streak-week-day ${isToday ? 'is-today' : ''}`} key={date}>
              <span className="streak-weekday">{weekday(date)}</span>
              <span
                className={`streak-day-marker ${solved ? 'is-complete' : ''}`}
                title={`${formatDate(date)}: ${solved ? 'solved' : 'no solve recorded'}`}
                aria-label={`${weekday(date)} ${formatDate(date)}${solved ? ', completed' : ', incomplete'}${isToday ? ', today' : ''}`}
              >
                {solved ? '✓' : ''}
              </span>
            </div>
          );
        })}
      </div>

      <div className="streak-activity">
        <div className="streak-activity-header">
          <h3>Recent activity</h3>
          <span className="muted">{formatDate(firstDate)} – {formatDate(today)}</span>
        </div>
        <div className="streak-heatmap" role="list" aria-label="Daily solving activity over the last 12 weeks">
          {heatmap.map(({ date, solvedCount }) => (
            <span
              key={date}
              role="listitem"
              className={`streak-heatmap-day ${solvedCount > 0 ? 'has-activity' : ''}`}
              title={`${formatDate(date)}: ${solvedCount} solved problem${solvedCount === 1 ? '' : 's'}`}
              aria-label={`${formatDate(date)}: ${solvedCount} solved problem${solvedCount === 1 ? '' : 's'}`}
            />
          ))}
        </div>
        <p className="streak-legend"><span className="streak-legend-swatch" /> Day with at least one solved problem</p>
      </div>
    </Card>
  );
}
