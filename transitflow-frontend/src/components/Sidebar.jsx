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
        className={`fixed inset-y-0 left-0 z-40 w-60 transform border-r border-slate-200 bg-white pt-16 transition-transform md:static md:min-h-[calc(100vh-4rem)] md:translate-x-0 md:pt-0 ${
          open ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <nav className="flex flex-col gap-1 p-3">
          {links.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.end}
              onClick={onClose}
              className={({ isActive }) =>
                `rounded-lg px-3 py-2 text-sm font-medium ${
                  isActive ? 'bg-blue-50 text-blue-700' : 'text-slate-600 hover:bg-slate-100'
                }`
              }
            >
              {link.label}
            </NavLink>
          ))}
        </nav>
      </aside>
    </>
  );
}