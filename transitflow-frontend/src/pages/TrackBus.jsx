import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import api, { errorMessage } from '../api/axios';
import Card from '../components/Card';
import ErrorMessage from '../components/ErrorMessage';
import LoadingSpinner from '../components/LoadingSpinner';
import StatusBadge from '../components/StatusBadge';
import { useTripSocket } from '../hooks/useTripSocket';
import { formatDateTime } from '../utils/format';

export default function TrackBus() {
  const { id } = useParams();
  const [trip, setTrip] = useState(null);
  const [location, setLocation] = useState(null);
  const [error, setError] = useState('');
  const [allowed, setAllowed] = useState(false);

    const applyLocation = useCallback((promise) => promise
    .then((res) => {
      setLocation(res.data.data);
      setAllowed(true);
      setError('');
    })
    .catch((err) => {
      setAllowed(false);
      setError(err.response?.status === 404
        ? 'You can only track a trip that you have a confirmed booking on.'
        : errorMessage(err));
    }), []);

  const loadLocation = useCallback(
    () => applyLocation(api.get(`/trips/${id}/location`)),
    [id, applyLocation]
  );

  useEffect(() => {
    let active = true;
    api.get(`/trips/${id}`).then((res) => active && setTrip(res.data.data.trip)).catch(() => {});
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
        setError(err.response?.status === 404
          ? 'You can only track a trip that you have a confirmed booking on.'
          : errorMessage(err));
      });
    return () => { active = false; };
  }, [id]);

  // Subscribe only after the REST call proved we are allowed to (a denied subscribe closes the socket).
  const live = useTripSocket(id, {
    onLocation: setLocation,
    onStatus: loadLocation, // trip started, delayed, completed...
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
          <StatusBadge status={location.tripStatus} />
          <span className={`text-xs font-medium ${live ? 'text-green-600' : 'text-slate-400'}`}>
            ● {live ? 'Live' : 'Connecting…'}
          </span>
        </div>
      </div>

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
  );
}