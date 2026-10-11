import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import api, { errorMessage } from '../api/axios';
import Button from '../components/Button';
import Card from '../components/Card';
import ErrorMessage from '../components/ErrorMessage';
import Input from '../components/Input';
import LoadingSpinner from '../components/LoadingSpinner';
import StatusBadge from '../components/StatusBadge';
import { formatDateTime } from '../utils/format';
import { isRunning } from '../utils/trip';

const STEPS = 20;
const INTERVAL_MS = 3000;

export default function UpdateLocation() {
  const { id } = useParams();
  const [trip, setTrip] = useState(null);
  const [last, setLast] = useState(null);
  const [manual, setManual] = useState({ lat: '17.385000', lng: '78.486700' });
  const [from, setFrom] = useState({ lat: '17.385000', lng: '78.486700' });
  const [to, setTo] = useState({ lat: '17.443500', lng: '78.377200' });
  const [progress, setProgress] = useState(null); // null = not simulating
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');

  const timer = useRef(null);
  const step = useRef(0);
  const inFlight = useRef(false);

  useEffect(() => {
    let active = true;
    api.get(`/driver/trips/${id}`)
      .then((res) => {
        if (!active) return;
        setTrip(res.data.data.trip);
        if (res.data.data.location?.latitude != null) setLast(res.data.data.location);
      })
      .catch((err) => active && setError(errorMessage(err)));
    return () => { active = false; };
  }, [id]);

  // Stop any running simulation when leaving the page.
  useEffect(() => () => clearInterval(timer.current), []);

  const send = useCallback(async (latitude, longitude) => {
    const res = await api.put(`/driver/trips/${id}/location`, {
      latitude: Number(latitude.toFixed(6)),
      longitude: Number(longitude.toFixed(6)),
    });
    setLast(res.data.data);
  }, [id]);

  const stopSim = useCallback(() => {
    clearInterval(timer.current);
    timer.current = null;
    setProgress(null);
  }, []);

  const sendManual = async () => {
    const lat = Number(manual.lat);
    const lng = Number(manual.lng);
    if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
      setError('Enter valid numbers for latitude and longitude.');
      return;
    }
    setError('');
    setSending(true);
    try {
      await send(lat, lng);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSending(false);
    }
  };

  const fillFromGps = () => {
    if (!navigator.geolocation) {
      setError('This browser cannot read your location.');
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => setManual({ lat: pos.coords.latitude.toFixed(6), lng: pos.coords.longitude.toFixed(6) }),
      () => setError('Could not read your location. Allow location access in the browser.')
    );
  };

  const startSim = () => {
    const a = [Number(from.lat), Number(from.lng)];
    const b = [Number(to.lat), Number(to.lng)];
    if ([...a, ...b].some((n) => !Number.isFinite(n))) {
      setError('Enter valid start and end coordinates.');
      return;
    }
    setError('');
    step.current = 0;
    setProgress(0);

    const tick = async () => {
      if (inFlight.current) return;
      inFlight.current = true;
      const t = step.current / STEPS;
      try {
        await send(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t);
        if (timer.current === null) return; // stopped while the request was in flight
        setProgress(step.current);
        step.current += 1;
        if (step.current > STEPS) stopSim();
      } catch (err) {
        setError(errorMessage(err));
        stopSim();
      } finally {
        inFlight.current = false;
      }
    };

    tick();
    timer.current = setInterval(tick, INTERVAL_MS);
  };

  if (!trip) return error ? <ErrorMessage message={error} /> : <LoadingSpinner center size="lg" />;

  const running = isRunning(trip.status);
  const simulating = progress !== null;
  const setField = (setter) => (e) => setter((prev) => ({ ...prev, [e.target.name]: e.target.value }));

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-3">
        <h1 className="text-2xl font-bold">Update location</h1>
        <StatusBadge status={trip.status} />
      </div>
      <p className="text-sm text-slate-600">{trip.source} → {trip.destination} · Bus {trip.busNumber}</p>

      {!running && (
        <ErrorMessage message="Location can only be shared while the trip is running. Start the trip first." />
      )}
      <ErrorMessage message={error} />

      <div className="grid gap-4 lg:grid-cols-2">
        <Card title="Send a position">
          <div className="grid grid-cols-2 gap-3">
            <Input label="Latitude" name="lat" value={manual.lat} onChange={setField(setManual)} />
            <Input label="Longitude" name="lng" value={manual.lng} onChange={setField(setManual)} />
          </div>
          <div className="mt-4 flex flex-wrap gap-2">
            <Button loading={sending} disabled={!running || simulating} onClick={sendManual}>Send location</Button>
            <Button variant="secondary" onClick={fillFromGps}>Use my GPS</Button>
          </div>
        </Card>

        <Card title="Simulate movement">
          <p className="mb-3 text-sm text-slate-600">
            Moves the bus in {STEPS} steps from the start to the end point, one update every {INTERVAL_MS / 1000} seconds.
          </p>
          <div className="grid grid-cols-2 gap-3">
            <Input label="Start latitude" name="lat" value={from.lat} onChange={setField(setFrom)} />
            <Input label="Start longitude" name="lng" value={from.lng} onChange={setField(setFrom)} />
            <Input label="End latitude" name="lat" value={to.lat} onChange={setField(setTo)} />
            <Input label="End longitude" name="lng" value={to.lng} onChange={setField(setTo)} />
          </div>
          <div className="mt-4 flex flex-wrap items-center gap-3">
            {simulating
              ? <Button variant="danger" onClick={stopSim}>Stop simulation</Button>
              : <Button disabled={!running} onClick={startSim}>Start simulation</Button>}
            {simulating && <span className="text-sm text-slate-600">Step {progress} of {STEPS}</span>}
          </div>
        </Card>
      </div>

      <Card title="Last position sent">
        {last?.latitude != null ? (
          <p className="text-sm">
            {last.latitude.toFixed(5)}, {last.longitude.toFixed(5)} · {formatDateTime(last.updatedAt)}
          </p>
        ) : (
          <p className="text-sm text-slate-500">Nothing sent yet.</p>
        )}
      </Card>

      <Link to={`/driver/trips/${id}`} className="text-sm text-blue-600 hover:underline">Back to trip</Link>
    </div>
  );
}