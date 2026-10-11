import { Link } from 'react-router-dom';

const VARIANTS = {
  primary: 'bg-blue-600 text-white hover:bg-blue-700',
  secondary: 'border border-slate-300 bg-white text-slate-700 hover:bg-slate-50',
};

export default function LinkButton({ to, variant = 'primary', className = '', children }) {
  return (
    <Link
      to={to}
      className={`inline-flex items-center justify-center rounded-lg px-4 py-2 text-sm font-medium transition ${VARIANTS[variant]} ${className}`}
    >
      {children}
    </Link>
  );
}