import { useState } from 'react';
import Icon from './Icon';

/** Displays a value (e.g. a class code) with a one-click copy button. */
export default function CopyButton({ value, label = 'Copy' }) {
  const [copied, setCopied] = useState(false);

  const copy = async (e) => {
    e.stopPropagation();
    e.preventDefault();
    try {
      await navigator.clipboard.writeText(value);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      setCopied(false);
    }
  };

  return (
    <span className="copy-chip">
      <code>{value}</code>
      <button type="button" onClick={copy} aria-label={`${label} ${value}`} title={label}>
        <Icon name={copied ? 'check' : 'copy'} size={14} />
        <span className="copy-text">{copied ? 'Copied' : label}</span>
      </button>
    </span>
  );
}
