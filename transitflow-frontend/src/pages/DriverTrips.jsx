import { useEffect, useState } from 'react';
import api, { errorMessage } from '../api/axios';
import Card from '../components/Card';
import DriverTripCard from '../components/DriverTripCard';
import ErrorMessage from '../components/ErrorMessage';
import LoadingSpinner from '../components/LoadingSpinner';

export default function DriverTrips() {
  const [includePast, setIncludePast] = useState(false);
  const [result, setResult] = useState(null); // { includePast, trips }
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    api.get('/driver/trips', { params: { includePast } })
      .then((res) => active && setResult({ includePast, trips: res.data.data }))
      .catch((err) => active && setError(errorMessage(err)));
    return () => { active = false; };
  }, [includePast]);

  // Still loading while the stored result belongs to the other filter value.
  const trips = result?.includePast === includePast ? result.trips : null;

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold">My trips</h1>
        <label className="flex items-center gap-2 text-sm text-slate-700">
          <input type="checkbox" checked={includePast} onChange={(e) => setIncludePast(e.target.checked)} />
          Show past trips
        </label>
      </div>
      <ErrorMessage message={error} />
      {!trips && !error && <LoadingSpinner center />}
      {trips && trips.length === 0 && <Card><p className="text-slate-600">No trips found.</p></Card>}
      <div className="space-y-3">
        {trips?.map((t) => <DriverTripCard key={t.tripId} trip={t} />)}
      </div>
    </div>
  );
}