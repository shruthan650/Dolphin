import Icon from './Icon';

export default function SearchInput({ value, onChange, placeholder = 'Search…' }) {
  return (
    <label className="search-input">
      <Icon name="search" size={16} />
      <input
        type="search"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        aria-label={placeholder}
      />
    </label>
  );
}
