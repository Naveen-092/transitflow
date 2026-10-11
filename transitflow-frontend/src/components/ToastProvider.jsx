import { useCallback, useMemo, useRef, useState } from 'react';
import { CheckCircle2, Info, XCircle } from 'lucide-react';
import { ToastContext } from './toastContextObject';

const STYLES = {
  success: { box: 'border-green-200 bg-green-50 text-green-800', Icon: CheckCircle2 },
  error: { box: 'border-red-200 bg-red-50 text-red-800', Icon: XCircle },
  info: { box: 'border-blue-200 bg-blue-50 text-blue-800', Icon: Info },
};

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const nextId = useRef(1);

  const dismiss = useCallback((id) => setToasts((t) => t.filter((x) => x.id !== id)), []);

  const push = useCallback((type, message) => {
    const id = nextId.current++;
    setToasts((t) => [...t, { id, type, message }]);
    setTimeout(() => dismiss(id), 4000);
  }, [dismiss]);

  const value = useMemo(() => ({
    success: (m) => push('success', m),
    error: (m) => push('error', m),
    info: (m) => push('info', m),
  }), [push]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="pointer-events-none fixed bottom-4 right-4 z-[100] flex w-80 max-w-[calc(100vw-2rem)] flex-col gap-2 print:hidden">
        {toasts.map(({ id, type, message }) => {
          const { box, Icon } = STYLES[type];
          return (
            <div key={id} role="status"
                 className={`toast-in pointer-events-auto flex items-start gap-2 rounded-lg border px-4 py-3 text-sm shadow-lg ${box}`}>
              <Icon size={18} className="mt-0.5 shrink-0" />
              <span className="flex-1">{message}</span>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}