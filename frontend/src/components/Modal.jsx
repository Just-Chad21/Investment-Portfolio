import { useEffect, useRef } from 'react';

// Native <dialog> — gets focus trapping, Esc-to-close, and a backdrop for free.
// Only ever mounted while open (see App.jsx), so it always starts fresh.
export default function Modal({ title, onClose, children }) {
  const dialogRef = useRef(null);

  useEffect(() => {
    dialogRef.current?.showModal();
  }, []);

  function handleClick(event) {
    // A click that lands on the <dialog> element itself (not a descendant) is a
    // click on the backdrop area.
    if (event.target === dialogRef.current) {
      onClose();
    }
  }

  return (
    <dialog ref={dialogRef} className="modal" onClose={onClose} onClick={handleClick}>
      <div className="modal-header">
        <h2>{title}</h2>
        <button type="button" className="modal-close" onClick={onClose} aria-label="Close">
          &times;
        </button>
      </div>
      {children}
    </dialog>
  );
}
