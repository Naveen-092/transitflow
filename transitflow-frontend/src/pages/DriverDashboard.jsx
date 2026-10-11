import { useEffect, useState } from 'react';
import { CalendarClock, Truck, Users } from 'lucide-react';
import api, { errorMessage } from '../api/axios';
import Card from '../components/Card';
import DriverTripCard from '../components/DriverTripCard';
import ErrorMessage from '../components/ErrorMessage';
import LinkButton from '../components/LinkButton';
import LoadingSpinner from '../components/LoadingSpinner';
import StatCard from '../components/StatCard';
import { useAuth } from '../hooks/useAuth';
import { todayISO } from '../utils/format';
import { isRunning } from '../utils/trip';

export default function DriverDashboard() {
  const { user } = useAuth();
  const [trips, setTrips] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    api.get('/driver/trips')
      .then((res) => active && setTrips(res.data.data))
      .catch((err) => active && setError(errorMessage(err)));
    return () => { active = false; };
  }, []);

  const today = todayISO();
  const list = trips || [];
  const running = list.filter((t) => isRunning(t.status));
  const todays = list.filter((t) => t.tripDate === today && !isRunning(t.status));
  const upcoming = list.filter((t) => t.tripDate > today && !isRunning(t.status)).slice(0, 5);
  const passengersToday = [...running, ...todays]
    .filter((t) => t.status !== 'CANCELLED')
    .reduce((sum, t) => sum + t.passengerCount, 0);

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">Welcome, {user.name}</h1>
      <ErrorMessage message={error} />
      {!trips && !error && <LoadingSpinner center />}

      {trips && (
        <>
          <div className="grid gap-3 sm:grid-cols-3">
            <StatCard label="Trips in progress" value={running.length} icon={Truck} tone="green" />
            <StatCard label="Trips today" value={todays.length + running.length} icon={CalendarClock} tone="blue" />
            <StatCard label="Passengers today" value={passengersToday} icon={Users} tone="violet" />
          </div>

          {running.length > 0 && (
            <section className="space-y-3">
              <h2 className="text-lg font-semibold">In progress</h2>
              {running.map((t) => <DriverTripCard key={t.tripId} trip={t} />)}
            </section>
          )}

          <section className="space-y-3">
            <h2 className="text-lg font-semibold">Today</h2>
            {todays.length === 0
              ? <Card><p className="text-slate-600">No other trips today.</p></Card>
              : todays.map((t) => <DriverTripCard key={t.tripId} trip={t} />)}
          </section>

          <section className="space-y-3">
            <h2 className="text-lg font-semibold">Upcoming</h2>
            {upcoming.length === 0
              ? <Card><p className="text-slate-600">No upcoming trips assigned.</p></Card>
              : upcoming.map((t) => <DriverTripCard key={t.tripId} trip={t} />)}
          </section>

          <LinkButton to="/driver/verify" variant="secondary">Verify a ticket</LinkButton>
        </>
      )}
    </div>
  );
}