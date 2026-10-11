import { NavLink } from 'react-router-dom';
import { NAV } from '../navConfig';
import { useAuth } from '../hooks/useAuth';

export default function Sidebar({ open, onClose }) {
  const { user } = useAuth();
  const links = NAV[user?.role] || [];

  return (
    <>
      {open && <div className="fixed inset-0 z-30 bg-slate-900/30 md:hidden" onClick={onClose} />}
      <aside
        className={`fixed inset-y-0 left-0 z-40 w-64 transform border-r border-slate-200 bg-white pt-16 transition-transform print:hidden md:static md:min-h-[calc(100vh-4rem)] md:translate-x-0 md:pt-0 ${
          open ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <p className="px-5 pb-2 pt-5 text-xs font-semibold uppercase tracking-wider text-slate-400">
          {user?.role} menu
        </p>
        <nav className="flex flex-col gap-1 px-3 pb-4">
          {links.map(({ to, label, icon: Icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              onClick={onClose}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition ${
                  isActive
                    ? 'bg-blue-50 text-blue-700 shadow-[inset_3px_0_0_0_theme(colors.blue.600)]'
                    : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
                }`
              }
            >
              <Icon size={18} />
              {label}
            </NavLink>
          ))}
        </nav>
      </aside>
    </>
  );
}