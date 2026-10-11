import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import api, { errorMessage } from '../api/axios';
import Button from '../components/Button';
import Card from '../components/Card';
import ErrorMessage from '../components/ErrorMessage';
import LoadingSpinner from '../components/LoadingSpinner';
import StatusBadge from '../components/StatusBadge';
import Table from '../components/Table';

const COLUMNS = [
  { key: 'bookingReference', header: 'Booking reference', render: (r) => <span className="font-mono">{r.bookingReference}</span> },
  { key: 'passengerName', header: 'Passenger' },
  { key: 'seats', header: 'Seats', render: (r) => r.seatNumbers.join(', ') },
  { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
];

export default function DriverPassengers() {
  const { id } = useParams();
  const [includeCancelled, setIncludeCancelled] = useState(false);
  const [result, setResult] = useState(null); // { includeCancelled, rows }
  const [trip, setTrip] = useState(null);
  const [error, setError] = useState('');
  const [refreshing, setRefreshing] = useState(false);

  const fetchRows = useCallback(
    () => api.get(`/driver/trips/${id}/passengers`, { params: { includeCancelled } })
      .then((res) => setResult({ includeCancelled, rows: res.data.data })),
    [id, includeCancelled]
  );

  useEffect(() => {
    let active = true;
    api.get(`/driver/trips/${id}`)
      .then((res) => active && setTrip(res.data.data.trip))
      .catch((err) => active && setError(errorMessage(err)));
    return () => { active = false; };
  }, [id]);

  useEffect(() => {
    let active = true;
    fetchRows().catch((err) => active && setError(errorMessage(err)));
    return () => { active = false; };
  }, [fetchRows]);

  const rows = result?.includeCancelled === includeCancelled ? result.rows : null;
  const confirmed = rows ? rows.filter((r) => r.status === 'CONFIRMED') : [];
  const seatCount = confirmed.reduce((n, r) => n + r.seatNumbers.length, 0);

  const refresh = async () => {
    setRefreshing(true);
    setError('');
    try {
      await fetchRows();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setRefreshing(false);
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">Passenger list</h1>
          {trip && <p className="text-sm text-slate-600">{trip.source} → {trip.destination} · Bus {trip.busNumber}</p>}
        </div>
        <div className="flex items-center gap-4">
          <label className="flex items-center gap-2 text-sm text-slate-700">
            <input type="checkbox" checked={includeCancelled} onChange={(e) => setIncludeCancelled(e.target.checked)} />
            Include cancelled
          </label>
          <Button variant="secondary" loading={refreshing} onClick={refresh}>Refresh</Button>
        </div>
      </div>

      <ErrorMessage message={error} />
      {!rows && !error && <LoadingSpinner center />}

      {rows && (
        <>
          <Card>
            <p className="text-sm text-slate-700">
              <span className="font-semibold">{confirmed.length}</span> confirmed bookings ·{' '}
              <span className="font-semibold">{seatCount}</span> seats
            </p>
          </Card>
          <Table columns={COLUMNS} rows={rows} rowKey="bookingId" empty="No passengers booked yet" />
        </>
      )}

      <Link to={`/driver/trips/${id}`} className="text-sm text-blue-600 hover:underline">Back to trip</Link>
    </div>
  );
}