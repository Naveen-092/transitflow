import Card from '../components/Card';
import StatusBadge from '../components/StatusBadge';
import { useAuth } from '../hooks/useAuth';
import { formatDateTime } from '../utils/format';

export default function Profile() {
  const { user } = useAuth();
  return (
    <div className="max-w-lg space-y-4">
      <h1 className="text-2xl font-bold">Profile</h1>
      <Card>
        <dl className="space-y-3 text-sm">
          <div className="flex justify-between"><dt className="text-slate-500">Name</dt><dd className="font-medium">{user.name}</dd></div>
          <div className="flex justify-between"><dt className="text-slate-500">Email</dt><dd className="font-medium">{user.email}</dd></div>
          <div className="flex justify-between"><dt className="text-slate-500">Phone</dt><dd className="font-medium">{user.phone}</dd></div>
          <div className="flex justify-between"><dt className="text-slate-500">Role</dt><dd><StatusBadge status={user.role} /></dd></div>
          <div className="flex justify-between"><dt className="text-slate-500">Member since</dt><dd className="font-medium">{formatDateTime(user.createdAt)}</dd></div>
        </dl>
      </Card>
    </div>
  );
}