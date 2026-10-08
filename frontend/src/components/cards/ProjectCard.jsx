import { Link } from 'react-router-dom';
import Button from '../common/Button';
import Icon from '../common/Icon';
import { formatDate } from '../../utils/format';

/**
 * Project summary. Pass onEdit/onDelete for owner actions, viewTo for a details link. classLabel names the class the
 * project belongs to (falls back to the className teachers receive).
 */
export default function ProjectCard({ project, viewTo, onEdit, onDelete, showOwner = false, classLabel }) {
  const cls = classLabel ?? project.className;
  return (
    <article className="project-card">
      <div className="project-card-head">
        <h3 className="project-title">
          {viewTo ? <Link to={viewTo}>{project.title}</Link> : project.title}
        </h3>
        <span className="project-owner">
          {showOwner && project.ownerName ? `${project.ownerName} · ` : ''}Created {formatDate(project.createdAt)}
        </span>
        {cls !== undefined && <span className="chip chip-sm">{cls || 'Unassigned'}</span>}
      </div>
      {project.description ? (
        <p className="project-description">{project.description}</p>
      ) : (
        <p className="project-description muted">No description provided.</p>
      )}
      {project.technologies?.length > 0 && (
        <ul className="tech-list" aria-label="Technologies">
          {project.technologies.map((t) => (
            <li key={t} className="tag">
              {t}
            </li>
          ))}
        </ul>
      )}
      <div className="project-card-foot">
        <div className="project-links">
          {project.githubUrl && (
            <a href={project.githubUrl} target="_blank" rel="noopener noreferrer">
              <Icon name="github" size={16} /> Code
            </a>
          )}
          {project.liveUrl && (
            <a href={project.liveUrl} target="_blank" rel="noopener noreferrer">
              <Icon name="external" size={16} /> Live
            </a>
          )}
          {!project.githubUrl && !project.liveUrl && <span className="muted">No links</span>}
        </div>
        {(onEdit || onDelete) && (
          <div className="project-actions">
            {onEdit && <Button variant="ghost" size="sm" icon="edit" onClick={onEdit} aria-label={`Edit ${project.title}`} />}
            {onDelete && (
              <Button variant="ghost" size="sm" icon="trash" onClick={onDelete} aria-label={`Delete ${project.title}`} className="btn-danger-ghost" />
            )}
          </div>
        )}
      </div>
    </article>
  );
}
