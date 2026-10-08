import { useEffect, useState } from 'react';
import Button from './Button';
import Input from './Input';
import Modal from './Modal';

/**
 * Confirmation before a sensitive action. requireText (e.g. "DELETE") adds a field the user must type it into
 * before the confirm button is enabled; children render extra content below the message.
 */
export default function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = 'Confirm',
  variant = 'danger',
  loading = false,
  requireText,
  confirmDisabled = false,
  onConfirm,
  onCancel,
  children,
}) {
  const [typed, setTyped] = useState('');
  useEffect(() => {
    if (!open) setTyped('');
  }, [open]);
  const blocked = confirmDisabled || (requireText && typed.trim() !== requireText);

  return (
    <Modal
      open={open}
      title={title}
      onClose={loading ? undefined : onCancel}
      size="sm"
      footer={
        <>
          <Button variant="secondary" onClick={onCancel} disabled={loading}>
            Cancel
          </Button>
          <Button variant={variant} onClick={onConfirm} loading={loading} disabled={blocked}>
            {confirmLabel}
          </Button>
        </>
      }
    >
      {typeof message === 'string' ? <p className="confirm-message">{message}</p> : message}
      {children}
      {requireText && (
        <Input
          label={
            <>
              Type <strong>{requireText}</strong> to confirm
            </>
          }
          value={typed}
          onChange={(e) => setTyped(e.target.value)}
          autoComplete="off"
          autoCapitalize="characters"
          spellCheck={false}
          className="confirm-type"
        />
      )}
    </Modal>
  );
}
