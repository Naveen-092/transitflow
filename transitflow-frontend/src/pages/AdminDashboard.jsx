import { Bus, CalendarClock, Route, Ticket, Users } from 'lucide-react';
import Card from '../components/Card';
import ErrorMessage from '../components/ErrorMessage';
import LinkButton from '../components/LinkButton';
import LoadingSpinner from '../components/LoadingSpinner';
import StatCard from '../components/StatCard';
import StatusBadge from '../components/StatusBadge';
import Table from '../components/Table';
import { useApiList } from '../hooks/useApiList';
import { useAuth } from '../hooks/useAuth';
import { formatDate, formatTime } from '../utils/format';

const ACTIVE_COLUMNS = [
  { key: 'route', header: 'Route', render: (t) => `${t.source} → ${t.destination}` },
  { key: 'when', header: 'When', render: (t) => `${formatDate(t.tripDate)}, ${formatTime(t.departureTime)}` },
  { key: 'busNumber', header: 'Bus' },
  { key: 'driverName', header: 'Driver' },
  { key: 'status', header: 'Status', render: (t) => <StatusBadge status={t.status} /> },
];

export default function AdminDashboard() {
  const { user } = useAuth();
  const buses = useApiList('/admin/buses');
  const drivers = useApiList('/admin/drivers');
  const routes = useApiList('/admin/routes');
  const trips = useApiList('/admin/trips');
  const active = useApiList('/admin/trips/active');
  const bookings = useApiList('/admin/bookings', { status: 'CONFIRMED' });

  const error = buses.error || drivers.error || routes.error || trips.error || active.error || bookings.error;
  const loading = [buses, drivers, routes, trips, active, bookings].some((x) => !x.data);

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">Welcome, {user.name}</h1>
      <ErrorMessage message={error} />
      {loading && !error && <LoadingSpinner center />}

      {!loading && (
        <>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
            <StatCard label="Active buses" value={buses.data.filter((b) => b.status === 'ACTIVE').length} icon={Bus} tone="blue" />
            <StatCard label="Active drivers" value={drivers.data.filter((d) => d.active).length} icon={Users} tone="green" />
            <StatCard label="Active routes" value={routes.data.filter((r) => r.active).length} icon={Route} tone="violet" />
            <StatCard label="Scheduled trips" value={trips.data.filter((t) => t.status === 'SCHEDULED').length} icon={CalendarClock} tone="amber" />
            <StatCard label="Confirmed bookings" value={bookings.data.length} icon={Ticket} tone="slate" />
          </div>

          <Card title={`On the road now (${active.data.length})`}>
            <Table columns={ACTIVE_COLUMNS} rows={active.data} empty="No trips are running right now" />
          </Card>

          <div className="flex flex-wrap gap-2">
            <LinkButton to="/admin/trips">Manage trips</LinkButton>
            <LinkButton to="/admin/bookings" variant="secondary">View bookings</LinkButton>
            <LinkButton to="/admin/buses" variant="secondary">Manage buses</LinkButton>
          </div>
        </>
      )}
    </div>
  );
}