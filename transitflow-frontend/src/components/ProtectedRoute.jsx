import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { roleHome } from '../navConfig';
import LoadingSpinner from './LoadingSpinner';

/**
 * Not logged in        -> /login (remembering where the user wanted to go)
 * Logged in, wrong role -> their own home page
 * The backend enforces the same rules; this only gives a better experience.
 */
export default function ProtectedRoute({ roles, children }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) return <LoadingSpinner center size="lg" />;
  if (!user) return <Navigate to="/login" state={{ from: location }} replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to={roleHome(user.role)} replace />;
  return children ?? <Outlet />;
}