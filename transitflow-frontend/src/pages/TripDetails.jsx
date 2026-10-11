import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import api, { errorMessage } from '../api/axios';
import Card from '../components/Card';
import ErrorMessage from '../components/ErrorMessage';
import LinkButton from '../components/LinkButton';
import LoadingSpinner from '../components/LoadingSpinner';
import StatusBadge from '../components/StatusBadge';
import { formatDate, formatMoney, formatTime } from '../utils/format';

export default function TripDetails() {
  const { id } = useParams();
  const [detail, setDetail] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    api.get(`/trips/${id}`)
      .then((res) => active && setDetail(res.data.data))
      .catch((err) => active && setError(errorMessage(err)));
    return () => { active = false; };
  }, [id]);

  if (error) return <ErrorMessage message={error} />;
  if (!detail) return <LoadingSpinner center size="lg" />;

  const { trip, stops } = detail;
  const open = trip.status === 'SCHEDULED';

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-3">
        <h1 className="text-2xl font-bold">{trip.source} → {trip.destination}</h1>
        <StatusBadge status={trip.status} />
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card title="Trip details">
          <dl className="grid grid-cols-2 gap-y-3 text-sm">
            <dt className="text-slate-500">Route</dt><dd>{trip.routeName}</dd>
            <dt className="text-slate-500">Bus</dt><dd>{trip.busNumber}</dd>
            <dt className="text-slate-500">Date</dt><dd>{formatDate(trip.tripDate)}</dd>
            <dt className="text-slate-500">Departure</dt><dd>{formatTime(trip.departureTime)}</dd>
            <dt className="text-slate-500">Arrival</dt><dd>{formatTime(trip.arrivalTime)}</dd>
            <dt className="text-slate-500">Fare per seat</dt><dd className="font-semibold">{formatMoney(trip.fare)}</dd>
            <dt className="text-slate-500">Seats available</dt><dd>{trip.availableSeats} of {trip.totalSeats}</dd>
          </dl>
          <div className="mt-5">
            {open
              ? <LinkButton to={`/passenger/trips/${trip.tripId}/seats`}>Select seats</LinkButton>
              : <p className="text-sm text-slate-500">This trip is no longer open for booking.</p>}
          </div>
        </Card>

        <Card title="Stops">
          {stops.length === 0 ? (
            <p className="text-sm text-slate-500">No stops listed for this route.</p>
          ) : (
            <ol className="space-y-3">
              {stops.map((stop) => (
                <li key={stop.stopId} className="flex items-start gap-3">
                  <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-blue-100 text-xs font-semibold text-blue-700">
                    {stop.stopOrder}
                  </span>
                  <div>
                    <p className="text-sm font-medium">{stop.stopName}</p>
                    {stop.location && <p className="text-xs text-slate-500">{stop.location}</p>}
                  </div>
                </li>
              ))}
            </ol>
          )}
        </Card>
      </div>
    </div>
  );
}