import { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';

export default function Input({ label, error, id, type = 'text', className = '', ...props }) {
  const inputId = id || props.name;
  const [visible, setVisible] = useState(false);
  const isPassword = type === 'password';

  return (
    <div className={className}>
      {label && (
        <label htmlFor={inputId} className="mb-1 block text-sm font-medium text-slate-700">
          {label}
        </label>
      )}
      <div className="relative">
        <input
          id={inputId}
          type={isPassword && visible ? 'text' : type}
          className={`w-full rounded-lg border bg-white px-3 py-2 text-sm placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 ${
            isPassword ? 'pr-10' : ''
          } ${error ? 'border-red-400' : 'border-slate-300'}`}
          {...props}
        />
        {isPassword && (
          <button
            type="button"
            tabIndex={-1}
            onClick={() => setVisible((v) => !v)}
            aria-label={visible ? 'Hide password' : 'Show password'}
            className="absolute inset-y-0 right-0 flex items-center px-3 text-slate-400 hover:text-slate-600"
          >
            {visible ? <EyeOff size={18} /> : <Eye size={18} />}
          </button>
        )}
      </div>
      {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
    </div>
  );
}