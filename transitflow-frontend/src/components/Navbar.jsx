import { Bus, LogOut, Menu } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';

export default function Navbar({ onMenuClick }) {
  const { user, logout } = useAuth();
  const initials = (user?.name || '?')
    .split(' ').map((p) => p[0]).slice(0, 2).join('').toUpperCase();

  return (
    <header className="sticky top-0 z-50 flex h-16 items-center justify-between border-b border-slate-200 bg-white/90 px-4 backdrop-blur print:hidden">
      <div className="flex items-center gap-3">
        <button onClick={onMenuClick} aria-label="Open menu"
                className="rounded-lg p-2 text-slate-600 hover:bg-slate-100 md:hidden">
          <Menu size={20} />
        </button>
        <div className="flex items-center gap-2">
          <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-600 text-white">
            <Bus size={20} />
          </span>
          <span className="text-lg font-bold tracking-tight text-slate-900">TransitFlow</span>
        </div>
      </div>

      <div className="flex items-center gap-3">
        <div className="hidden text-right sm:block">
          <p className="text-sm font-medium leading-tight">{user?.name}</p>
          <p className="text-xs capitalize text-slate-500">{user?.role?.toLowerCase()}</p>
        </div>
        <span className="flex h-9 w-9 items-center justify-center rounded-full bg-blue-100 text-sm font-semibold text-blue-700">
          {initials}
        </span>
        <button onClick={logout}
                className="flex items-center gap-1.5 rounded-lg border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50">
          <LogOut size={16} />
          <span className="hidden sm:inline">Logout</span>
        </button>
      </div>
    </header>
  );
}