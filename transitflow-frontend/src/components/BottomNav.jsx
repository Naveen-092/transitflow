import { NavLink } from 'react-router-dom';
import { NAV } from '../navConfig';
import { useAuth } from '../hooks/useAuth';

/** Phone-only tab bar for passengers and drivers (admin has too many links; the drawer covers it). */
export default function BottomNav() {
  const { user } = useAuth();
  if (!user || user.role === 'ADMIN') return null;
  const links = NAV[user.role] || [];

  return (
    <nav className="fixed inset-x-0 bottom-0 z-40 flex border-t border-slate-200 bg-white/95 pb-[env(safe-area-inset-bottom)] backdrop-blur print:hidden md:hidden">
      {links.map(({ to, label, icon: Icon, end }) => (
        <NavLink key={to} to={to} end={end}
                 className={({ isActive }) =>
                   `flex flex-1 flex-col items-center gap-0.5 py-2 text-[11px] font-medium ${
                     isActive ? 'text-blue-600' : 'text-slate-500'}`}>
          <Icon size={20} />
          <span className="max-w-full truncate px-1">{label}</span>
        </NavLink>
      ))}
    </nav>
  );
}