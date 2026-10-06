import { Link } from 'react-router-dom';
import CopyButton from '../common/CopyButton';
import Icon from '../common/Icon';

/** `to` makes the whole card a link (teacher view); students see the teacher name instead. */
export default function ClassCard({ cls, to, highlight = false }) {
  const body = (
    <>
      <div className="class-card-top">
        <div className="class-card-icon">
          <Icon name="book" size={20} />
        </div>
        <div className="class-card-heading">
          <h3>{cls.className}</h3>
          <p>
            Semester {cls.semester} · {cls.branch} · Section {cls.section}
          </p>
        </div>
      </div>
      <dl className="class-card-meta">
        <div>
          <dt>Class code</dt>
          <dd>
            <CopyButton value={cls.classCode} />
          </dd>
        </div>
        <div>
          <dt>Students</dt>
          <dd className="class-card-count">{cls.studentCount}</dd>
        </div>
        {cls.teacherName && !to && (
          <div>
            <dt>Teacher</dt>
            <dd>{cls.teacherName}</dd>
          </div>
        )}
      </dl>
      {to && (
        <span className="class-card-link">
          View class <Icon name="arrowRight" size={16} />
        </span>
      )}
    </>
  );

  const className = `class-card ${highlight ? 'class-card-new' : ''}`;
  return to ? (
    <Link to={to} className={`${className} class-card-clickable`}>
      {body}
    </Link>
  ) : (
    <div className={className}>{body}</div>
  );
}
