import Icon from './Icon';

/** A student's GitHub and LeetCode profile links. */
export default function ProfileLinks({ githubUrl, leetCodeUrl }) {
  if (!githubUrl && !leetCodeUrl) return <p className="muted">No coding profiles added yet.</p>;
  return (
    <div className="project-links profile-links">
      {githubUrl && (
        <a href={githubUrl} target="_blank" rel="noopener noreferrer">
          <Icon name="github" size={16} /> GitHub
        </a>
      )}
      {leetCodeUrl && (
        <a href={leetCodeUrl} target="_blank" rel="noopener noreferrer">
          <Icon name="code" size={16} /> LeetCode
        </a>
      )}
    </div>
  );
}
