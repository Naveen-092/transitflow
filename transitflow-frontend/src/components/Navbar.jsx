import { useAuth } from '../hooks/useAuth';
import Button from './Button';

export default function Navbar({ onMenuClick }) {
  const { user, logout } = useAuth();
  return (
    <header className="sticky top-0 z-50 flex h-16 items-center justify-between border-b border-slate-200 bg-white px-4">
      <div className="flex items-center gap-3">
        <button onClick={onMenuClick} aria-label="Open menu" className="rounded p-2 text-slate-600 hover:bg-slate-100 md:hidden">☰</button>
        <span className="text-xl font-bold text-blue-700">🚌 TransitFlow</span>
      </div>
      <div className="flex items-center gap-3">
        <div className="hidden text-right sm:block">
          <p className="text-sm font-medium leading-tight">{user?.name}</p>
          <p className="text-xs text-slate-500">{user?.role}</p>
        </div>
        <Button variant="secondary" onClick={logout}>Logout</Button>
      </div>
    </header>
  );
}