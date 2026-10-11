import { useEffect, useState } from 'react';
import api, { errorMessage } from '../api/axios';
import BookingCard from '../components/BookingCard';
import Card from '../components/Card';
import ErrorMessage from '../components/ErrorMessage';
import LinkButton from '../components/LinkButton';
import LoadingSpinner from '../components/LoadingSpinner';
import { useAuth } from '../hooks/useAuth';
import { todayISO } from '../utils/format';

export default function PassengerDashboard() {
  const { user } = useAuth();
  const [bookings, setBookings] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    api.get('/bookings/my')
      .then((res) => active && setBookings(res.data.data))
      .catch((err) => active && setError(errorMessage(err)));
    return () => { active = false; };
  }, []);

    const upcoming = (bookings || [])
    .filter((b) => b.status === 'CONFIRMED' && b.tripDate >= todayISO())
    .sort((a, b) => (a.tripDate + a.departureTime).localeCompare(b.tripDate + b.departureTime))
    .slice(0, 3);
    
  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">Welcome, {user.name}</h1>

      <div className="flex flex-wrap gap-2">
        <LinkButton to="/passenger/search">Search trips</LinkButton>
        <LinkButton to="/passenger/bookings" variant="secondary">My bookings</LinkButton>
      </div>

      <ErrorMessage message={error} />
      <h2 className="text-lg font-semibold">Upcoming trips</h2>
      {!bookings && !error && <LoadingSpinner center />}
      {bookings && upcoming.length === 0 && (
        <Card><p className="text-slate-600">No upcoming trips. Search for a trip to book your seat.</p></Card>
      )}
      <div className="space-y-3">
        {upcoming.map((b) => <BookingCard key={b.id} booking={b} />)}
      </div>
    </div>
  );
}