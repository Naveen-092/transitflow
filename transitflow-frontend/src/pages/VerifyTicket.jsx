import { useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import api, { errorMessage } from '../api/axios';
import Button from '../components/Button';
import Card from '../components/Card';
import ErrorMessage from '../components/ErrorMessage';
import Input from '../components/Input';
import LoadingSpinner from '../components/LoadingSpinner';
import { formatDate, formatTime, todayISO } from '../utils/format';
import { isRunning } from '../utils/trip';

/** Default trip: a running one, else today's, else the first. */
function pickDefault(trips) {
  const today = todayISO();
  const found = trips.find((t) => isRunning(t.status)) || trips.find((t) => t.tripDate === today) || trips[0];
  return found ? String(found.tripId) : '';
}

export default function VerifyTicket() {
  const [params] = useSearchParams();
  const [trips, setTrips] = useState(null);
  const [tripId, setTripId] = useState(params.get('trip') || '');
  const [reference, setReference] = useState('');
  const [result, setResult] = useState(null);
  const [history, setHistory] = useState([]);
  const [checking, setChecking] = useState(false);
  const [error, setError] = useState('');
  const inputRef = useRef(null);

  useEffect(() => {
    let active = true;
    api.get('/driver/trips')
      .then((res) => {
        if (!active) return;
        setTrips(res.data.data);
        setTripId((current) => current || pickDefault(res.data.data));
      })
      .catch((err) => active && setError(errorMessage(err)));
    return () => { active = false; };
  }, []);

  const onSubmit = async (e) => {
    e.preventDefault();
    if (!tripId) {
      setError('Choose a trip first.');
      return;
    }
    if (!reference.trim()) {
      setError('Enter or scan the booking reference.');
      return;
    }
    setError('');
    setChecking(true);
    try {
      const res = await api.post('/driver/tickets/verify', {
        tripId: Number(tripId),
        bookingReference: reference.trim(),
      });
      setResult(res.data.data);
      setHistory((h) => [res.data.data, ...h].slice(0, 8));
      setReference('');
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setChecking(false);
      inputRef.current?.focus(); // ready for the next scan
    }
  };

  if (!trips) return error ? <ErrorMessage message={error} /> : <LoadingSpinner center size="lg" />;

  return (
    <div className="mx-auto max-w-xl space-y-4">
      <h1 className="text-2xl font-bold">Verify ticket</h1>

      <Card>
        <form onSubmit={onSubmit} className="space-y-4">
          <div>
            <label htmlFor="trip" className="mb-1 block text-sm font-medium text-slate-700">Trip</label>
            <select id="trip" value={tripId} onChange={(e) => setTripId(e.target.value)}
                    className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
              <option value="">Select a trip</option>
              {trips.map((t) => (
                <option key={t.tripId} value={t.tripId}>
                  {t.source} → {t.destination} · {formatDate(t.tripDate)} {formatTime(t.departureTime)} ({t.status})
                </option>
              ))}
            </select>
          </div>
          <Input label="Booking reference (type it, or scan the QR code with a scanner)" name="reference"
                 ref={inputRef} autoFocus autoComplete="off" placeholder="TF-20261012-0001"
                 value={reference} onChange={(e) => setReference(e.target.value)} />
          <ErrorMessage message={error} />
          <Button type="submit" loading={checking} className="w-full">Verify</Button>
        </form>
      </Card>

      {result && (
        <div role="status" aria-live="polite"
             className={`rounded-xl border-2 p-5 ${result.valid ? 'border-green-400 bg-green-50' : 'border-red-400 bg-red-50'}`}>
          <p className={`text-2xl font-bold ${result.valid ? 'text-green-700' : 'text-red-700'}`}>
            {result.valid ? '✔' : '✖'} {result.result}
          </p>
          <p className="mt-1 text-sm text-slate-700">{result.reason}</p>
          <p className="mt-3 font-mono text-sm">{result.bookingReference}</p>
          {result.valid && (
            <dl className="mt-2 space-y-1 text-sm">
              <div className="flex justify-between"><dt className="text-slate-500">Passenger</dt><dd className="font-medium">{result.passengerName}</dd></div>
              <div className="flex justify-between"><dt className="text-slate-500">Seats</dt><dd className="font-medium">{result.seatNumbers.join(', ')}</dd></div>
            </dl>
          )}
        </div>
      )}

      {history.length > 1 && (
        <Card title="Recent checks">
          <ul className="divide-y divide-slate-100 text-sm">
            {history.slice(1).map((h, i) => (
              <li key={`${h.bookingReference}-${i}`} className="flex items-center justify-between py-2">
                <span className="font-mono">{h.bookingReference}</span>
                <span className={h.valid ? 'font-medium text-green-700' : 'font-medium text-red-700'}>{h.result}</span>
              </li>
            ))}
          </ul>
        </Card>
      )}
    </div>
  );
}