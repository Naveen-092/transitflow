import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import api, { errorMessage } from '../api/axios';
import Card from '../components/Card';
import ErrorMessage from '../components/ErrorMessage';
import LoadingSpinner from '../components/LoadingSpinner';
import StatusBadge from '../components/StatusBadge';
import TripTimeline from '../components/TripTimeline';
import { useTripSocket } from '../hooks/useTripSocket';
import { formatDateTime } from '../utils/format';

const NOT_ALLOWED = 'You can only track a trip that you have a confirmed booking on.';

export default function TrackBus() {
  const { id } = useParams();
  const [trip, setTrip] = useState(null);
  const [stops, setStops] = useState([]);
  const [location, setLocation] = useState(null);
  const [error, setError] = useState('');
  const [allowed, setAllowed] = useState(false);

  const loadLocation = useCallback(
    () => api.get(`/trips/${id}/location`)
      .then((res) => { setLocation(res.data.data); setAllowed(true); setError(''); })
      .catch((err) => {
        setAllowed(false);
        setError(err.response?.status === 404 ? NOT_ALLOWED : errorMessage(err));
      }),
    [id]
  );

  useEffect(() => {
    let active = true;
    api.get(`/trips/${id}`)
      .then((res) => {
        if (!active) return;
        setTrip(res.data.data.trip);
        setStops(res.data.data.stops);
      })
      .catch(() => {});
    api.get(`/trips/${id}/location`)
      .then((res) => {
        if (!active) return;
        setLocation(res.data.data);
        setAllowed(true);
        setError('');
      })
      .catch((err) => {
        if (!active) return;
        setAllowed(false);
        setError(err.response?.status === 404 ? NOT_ALLOWED : errorMessage(err));
      });
    return () => { active = false; };
  }, [id]);

  // Subscribe only after REST proved we may (a denied subscribe closes the socket).
  const live = useTripSocket(id, {
    onLocation: setLocation,
    onStatus: (msg) => {
      setTrip((t) => (t ? { ...t, status: msg.status } : t));
      loadLocation();
    },
  }, allowed);

  if (error) {
    return (
      <div className="space-y-4">
        <ErrorMessage message={error} />
        <Link to="/passenger/bookings" className="text-sm text-blue-600 hover:underline">Back to my bookings</Link>
      </div>
    );
  }
  if (!location) return <LoadingSpinner center size="lg" />;

  const hasPosition = location.latitude != null && location.longitude != null;
  const lat = location.latitude;
  const lon = location.longitude;
  const d = 0.01;
  const mapSrc = hasPosition
    ? `https://www.openstreetmap.org/export/embed.html?bbox=${lon - d},${lat - d},${lon + d},${lat + d}&layer=mapnik&marker=${lat},${lon}`
    : null;

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold">
          Track bus{trip ? `: ${trip.source} → ${trip.destination}` : ''}
        </h1>
        <div className="flex items-center gap-3">
          <StatusBadge status={trip?.status || location.tripStatus} />
          <span className={`flex items-center gap-1.5 text-xs font-medium ${live ? 'text-green-600' : 'text-slate-400'}`}>
            <span className={`h-2 w-2 rounded-full ${live ? 'animate-pulse bg-green-500' : 'bg-slate-300'}`} />
            {live ? 'Live' : 'Connecting…'}
          </span>
        </div>
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <div className="lg:col-span-2">
          {hasPosition ? (
            <Card>
              <iframe key={`${lat},${lon}`} title="Bus location" src={mapSrc}
                      className="h-96 w-full rounded-lg border border-slate-200" />
              <div className="mt-3 flex flex-wrap items-center justify-between gap-2 text-sm text-slate-600">
                <span>Lat {lat.toFixed(5)}, Lng {lon.toFixed(5)} · updated {formatDateTime(location.updatedAt)}</span>
                <a className="text-blue-600 hover:underline" target="_blank" rel="noreferrer"
                   href={`https://www.openstreetmap.org/?mlat=${lat}&mlon=${lon}#map=16/${lat}/${lon}`}>
                  Open larger map
                </a>
              </div>
            </Card>
          ) : (
            <Card>
              <p className="text-slate-600">
                {location.tripStatus === 'SCHEDULED' && 'The bus has not started yet. Its position appears here once the driver starts the trip.'}
                {(location.tripStatus === 'STARTED' || location.tripStatus === 'DELAYED') && 'Waiting for the driver to share a position…'}
                {location.tripStatus === 'COMPLETED' && 'This trip has been completed.'}
                {location.tripStatus === 'CANCELLED' && 'This trip was cancelled.'}
              </p>
            </Card>
          )}
        </div>

        <Card title="Route progress">
          {trip ? <TripTimeline stops={stops} trip={trip} /> : <LoadingSpinner />}
        </Card>
      </div>
    </div>
  );
}