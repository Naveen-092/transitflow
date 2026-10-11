import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import api, { errorMessage } from '../api/axios';
import Button from '../components/Button';
import Card from '../components/Card';
import ErrorMessage from '../components/ErrorMessage';
import LinkButton from '../components/LinkButton';
import LoadingSpinner from '../components/LoadingSpinner';
import Modal from '../components/Modal';
import StatusBadge from '../components/StatusBadge';
import { useTripSocket } from '../hooks/useTripSocket';
import { formatDate, formatDateTime, formatTime } from '../utils/format';
import { isRunning } from '../utils/trip';

export default function DriverTripDetails() {
  const { id } = useParams();
  const [detail, setDetail] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState('');
  const [confirm, setConfirm] = useState(null); // 'end' | 'cancel' | null

  const load = useCallback(async () => {
    const res = await api.get(`/driver/trips/${id}`);
    setDetail(res.data.data);
  }, [id]);

  useEffect(() => {
    let active = true;
    api.get(`/driver/trips/${id}`)
      .then((res) => active && setDetail(res.data.data))
      .catch((err) => active && setError(errorMessage(err)));
    return () => { active = false; };
  }, [id]);

  // Status pushes (admin cancel, our own actions). Connect only once the trip is known to be ours.
  const live = useTripSocket(id, { onStatus: () => load().catch(() => {}) }, Boolean(detail));

  const put = (path, body) => api.put(`/driver/trips/${id}${path}`, body);

  const act = async (name, request) => {
    setBusy(name);
    setError('');
    try {
      await request();
      await load();
    } catch (err) {
      setError(errorMessage(err));
      load().catch(() => {});
    } finally {
      setBusy('');
    }
  };

  const CONFIRMS = {
    end: {
      title: 'End this trip?',
      text: 'The trip is marked completed and location sharing stops.',
      button: 'End trip',
      run: () => act('end', () => put('/end')),
    },
    cancel: {
      title: 'Cancel this trip?',
      text: 'All bookings on this trip are cancelled and the seats released. This cannot be undone.',
      button: 'Cancel trip',
      run: () => act('cancel', () => put('/status', { status: 'CANCELLED' })),
    },
  };

  if (!detail) return error ? <ErrorMessage message={error} /> : <LoadingSpinner center size="lg" />;

  const { trip, stops, location } = detail;
  const status = trip.status;
  const running = isRunning(status);
  const disabled = Boolean(busy);
  const cfg = confirm ? CONFIRMS[confirm] : null;

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-3">
          <h1 className="text-2xl font-bold">{trip.source} → {trip.destination}</h1>
          <StatusBadge status={status} />
        </div>
        <span className={`text-xs font-medium ${live ? 'text-green-600' : 'text-slate-400'}`}>
          ● {live ? 'Live' : 'Connecting…'}
        </span>
      </div>

      <ErrorMessage message={error} />

      <Card title="Actions">
        <div className="flex flex-wrap gap-2">
          {status === 'SCHEDULED' && (
            <>
              <Button loading={busy === 'start'} disabled={!trip.canStart || disabled}
                      onClick={() => act('start', () => put('/start'))}>
                Start trip
              </Button>
              <Button variant="danger" disabled={disabled} onClick={() => setConfirm('cancel')}>Cancel trip</Button>
            </>
          )}
          {status === 'STARTED' && (
            <Button variant="secondary" loading={busy === 'delay'} disabled={disabled}
                    onClick={() => act('delay', () => put('/status', { status: 'DELAYED' }))}>
              Mark delayed
            </Button>
          )}
          {status === 'DELAYED' && (
            <Button variant="secondary" loading={busy === 'resume'} disabled={disabled}
                    onClick={() => act('resume', () => put('/status', { status: 'STARTED' }))}>
              Resume trip
            </Button>
          )}
          {running && (
            <>
              <LinkButton to={`/driver/trips/${id}/location`}>Update location</LinkButton>
              <Button variant="danger" disabled={disabled} onClick={() => setConfirm('end')}>End trip</Button>
            </>
          )}
          <LinkButton to={`/driver/trips/${id}/passengers`} variant="secondary">Passenger list</LinkButton>
          {(status === 'SCHEDULED' || running) && (
            <LinkButton to={`/driver/verify?trip=${id}`} variant="secondary">Verify tickets</LinkButton>
          )}
        </div>
        {status === 'SCHEDULED' && !trip.canStart && (
          <p className="mt-3 text-sm text-slate-500">
            Starting opens shortly before departure and closes at the scheduled arrival time.
          </p>
        )}
        {(status === 'COMPLETED' || status === 'CANCELLED') && (
          <p className="mt-3 text-sm text-slate-500">This trip is {status.toLowerCase()}. No further actions.</p>
        )}
      </Card>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card title="Trip details">
          <dl className="grid grid-cols-2 gap-y-3 text-sm">
            <dt className="text-slate-500">Route</dt><dd>{trip.routeName}</dd>
            <dt className="text-slate-500">Bus</dt><dd>{trip.busNumber} ({trip.registrationNumber})</dd>
            <dt className="text-slate-500">Date</dt><dd>{formatDate(trip.tripDate)}</dd>
            <dt className="text-slate-500">Departure</dt><dd>{formatTime(trip.departureTime)}</dd>
            <dt className="text-slate-500">Arrival</dt><dd>{formatTime(trip.arrivalTime)}</dd>
            <dt className="text-slate-500">Seats booked</dt><dd>{trip.passengerCount} of {trip.capacity}</dd>
          </dl>
          {location?.latitude != null && (
            <p className="mt-4 border-t border-slate-100 pt-3 text-sm text-slate-600">
              Last position: {location.latitude.toFixed(5)}, {location.longitude.toFixed(5)}
              {' '}({formatDateTime(location.updatedAt)})
            </p>
          )}
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

      <Link to="/driver/trips" className="text-sm text-blue-600 hover:underline">Back to my trips</Link>

      <Modal
        open={Boolean(cfg)}
        title={cfg?.title}
        onClose={() => setConfirm(null)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setConfirm(null)}>Back</Button>
            <Button variant="danger" onClick={() => { const run = cfg.run; setConfirm(null); run(); }}>
              {cfg?.button}
            </Button>
          </>
        }
      >
        <p className="text-sm text-slate-600">{cfg?.text}</p>
      </Modal>
    </div>
  );
}