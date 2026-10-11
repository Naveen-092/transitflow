import { useState } from 'react';
import api, { errorMessage } from '../api/axios';
import Button from '../components/Button';
import Card from '../components/Card';
import ErrorMessage from '../components/ErrorMessage';
import Input from '../components/Input';
import LoadingSpinner from '../components/LoadingSpinner';
import TripCard from '../components/TripCard';
import { todayISO } from '../utils/format';

export default function SearchTrips() {
  const [form, setForm] = useState({ source: '', destination: '', date: todayISO() });
  const [trips, setTrips] = useState(null); // null = not searched yet
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await api.get('/trips/search', { params: form });
      setTrips(res.data.data);
    } catch (err) {
      setError(errorMessage(err));
      setTrips(null);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">Search trips</h1>
      <Card>
        <form onSubmit={onSubmit} className="grid gap-4 sm:grid-cols-4 sm:items-end">
          <Input label="From" name="source" required placeholder="e.g. Campus"
                 value={form.source} onChange={onChange} />
          <Input label="To" name="destination" required placeholder="e.g. Madhapur"
                 value={form.destination} onChange={onChange} />
          <Input label="Date" name="date" type="date" required min={todayISO()}
                 value={form.date} onChange={onChange} />
          <Button type="submit" loading={loading}>Search</Button>
        </form>
      </Card>

      <ErrorMessage message={error} />
      {loading && <LoadingSpinner center />}

      {!loading && trips && (
        trips.length === 0 ? (
          <Card><p className="text-slate-600">No trips found for that route and date.</p></Card>
        ) : (
          <div className="space-y-3">
            {trips.map((trip) => <TripCard key={trip.tripId} trip={trip} />)}
          </div>
        )
      )}
    </div>
  );
}