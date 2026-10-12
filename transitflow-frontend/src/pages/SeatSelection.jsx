import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import api, { errorMessage } from '../api/axios';
import Button from '../components/Button';
import Card from '../components/Card';
import ErrorMessage from '../components/ErrorMessage';
import HoldTimer from '../components/HoldTimer';
import LoadingSpinner from '../components/LoadingSpinner';
import SeatMap from '../components/SeatMap';
import StatusBadge from '../components/StatusBadge';
import { useTripSocket } from '../hooks/useTripSocket';
import { formatDate, formatMoney, formatTime } from '../utils/format';
import { useToast } from '../hooks/useToast';


const MAX_SEATS = 6;

export default function SeatSelection() {
  const toast = useToast();
  const { id } = useParams();
  const navigate = useNavigate();

  const [trip, setTrip] = useState(null);
  const [seats, setSeats] = useState([]);
  const [selected, setSelected] = useState([]);
  const [holdEndsAt, setHoldEndsAt] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  // Replace the seat list with the server's version and rebuild the hold deadline from it.
  const applySeats = useCallback((list) => {
    setSeats(list);
    const mine = list.filter((s) => s.heldByMe && s.status === 'HELD');
    if (mine.length > 0) {
      const secs = Math.min(...mine.map((s) => s.holdSecondsRemaining ?? 0));
      setHoldEndsAt(Date.now() + secs * 1000);
    } else {
      setHoldEndsAt(null);
    }
    setSelected((prev) => prev.filter((sid) => list.some((s) => s.tripSeatId === sid && s.status === 'AVAILABLE')));
  }, []);

  const loadSeats = useCallback(async () => {
    const res = await api.get(`/trips/${id}/seats`);
    applySeats(res.data.data);
  }, [id, applySeats]);

  const refresh = useCallback(() => {
    loadSeats().catch((err) => setError(errorMessage(err)));
  }, [loadSeats]);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const [detail, seatRes] = await Promise.all([api.get(`/trips/${id}`), api.get(`/trips/${id}/seats`)]);
        if (!active) return;
        setTrip(detail.data.data.trip);
        applySeats(seatRes.data.data);
      } catch (err) {
        if (active) setError(errorMessage(err));
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, [id, applySeats]);

  // ---- live updates -------------------------------------------------------
  const live = useTripSocket(id, {
    onSeats: (msg) => {
      const changed = new Map(msg.seats.map((c) => [c.tripSeatId, c]));
      const taken = msg.seats.filter((c) => c.status !== 'AVAILABLE' && selected.includes(c.tripSeatId));
      if (taken.length > 0 && !busy) {
        setNotice(`Seat ${taken.map((t) => t.seatNumber).join(', ')} was just taken by someone else.`);
      }
      setSeats((prev) => prev.map((s) => {
        const c = changed.get(s.tripSeatId);
        if (!c) return s;
        return { ...s, status: c.status, heldByMe: c.status === 'HELD' && s.heldByMe };
      }));
      setSelected((prev) => prev.filter((sid) => {
        const c = changed.get(sid);
        return !c || c.status === 'AVAILABLE';
      }));
    },
    onStatus: (msg) => setTrip((t) => (t ? { ...t, status: msg.status } : t)),
    onConnect: refresh, // catch up after the first connect and after every reconnect
  });

  // ---- derived state ------------------------------------------------------
  const myHeld = seats.filter((s) => s.heldByMe && s.status === 'HELD');
  const holding = myHeld.length > 0;
  const bookable = trip?.status === 'SCHEDULED';
  const count = holding ? myHeld.length : selected.length;
  const total = trip ? Number(trip.fare) * count : 0;
  const selectedNumbers = seats.filter((s) => selected.includes(s.tripSeatId)).map((s) => s.seatNumber);

  const toggle = (seatId) => {
    setNotice('');
    setSelected((prev) => {
      if (prev.includes(seatId)) return prev.filter((x) => x !== seatId);
      if (prev.length >= MAX_SEATS) {
        setNotice(`You can select at most ${MAX_SEATS} seats.`);
        return prev;
      }
      return [...prev, seatId];
    });
  };

  // ---- actions ------------------------------------------------------------
  const run = async (name, action) => {
    setBusy(name);
    setError('');
    setNotice('');
    try {
      await action();
    } catch (err) {
      setError(errorMessage(err));
      refresh(); // the server may have changed things (e.g. expired hold, lost race)
    } finally {
      setBusy('');
    }
  };

  const holdSeats = () => run('hold', async () => {
    await api.post('/bookings/hold', { tripId: Number(id), tripSeatIds: selected });
    toast.success('Seats held for 5 minutes');
    setSelected([]);
    await loadSeats();
  });

  const releaseSeats = () => run('release', async () => {
    await api.delete(`/bookings/hold/${id}`);
    await loadSeats();
  });

  const confirmBooking = () => run('confirm', async () => {
    const res = await api.post('/bookings/confirm', {
      tripId: Number(id),
      tripSeatIds: myHeld.map((s) => s.tripSeatId),
    });
    toast.success('Booking confirmed!');
    navigate(`/passenger/bookings/${res.data.data.id}`, { replace: true });
  });

  // ---- render -------------------------------------------------------------
  if (loading) return <LoadingSpinner center size="lg" />;
  if (!trip) return <ErrorMessage message={error || 'Trip not found'} />;

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">{trip.source} → {trip.destination}</h1>
          <p className="text-sm text-slate-600">
            {formatDate(trip.tripDate)} · {formatTime(trip.departureTime)} – {formatTime(trip.arrivalTime)} · Bus {trip.busNumber}
          </p>
        </div>
        <div className="flex items-center gap-3">
          <StatusBadge status={trip.status} />
          <span className={`text-xs font-medium ${live ? 'text-green-600' : 'text-slate-400'}`}>
            ● {live ? 'Live' : 'Connecting…'}
          </span>
        </div>
      </div>

      {!bookable && (
        <ErrorMessage message={`This trip is ${trip.status.toLowerCase()} and no longer open for booking.`} />
      )}

      <div className="grid gap-4 lg:grid-cols-3">
        <Card title="Choose your seats" className="lg:col-span-2">
          <SeatMap seats={seats} selectedIds={selected} disabled={holding || !bookable} onToggle={toggle} />
        </Card>

        <Card title="Your booking">
          <div className="space-y-4">
            <ErrorMessage message={error} />
            {notice && (
              <p className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">{notice}</p>
            )}

            {holding && holdEndsAt && (
              <HoldTimer key={holdEndsAt} endsAt={holdEndsAt}
                         seatNumbers={myHeld.map((s) => s.seatNumber)} onExpire={refresh} />
            )}

            {!holding && (
              <div className="text-sm">
                <p className="text-slate-500">Selected seats</p>
                <p className="font-medium">{selectedNumbers.length ? selectedNumbers.join(', ') : 'None yet'}</p>
              </div>
            )}

            <dl className="space-y-1 border-t border-slate-100 pt-3 text-sm">
              <div className="flex justify-between"><dt className="text-slate-500">Fare per seat</dt><dd>{formatMoney(trip.fare)}</dd></div>
              <div className="flex justify-between"><dt className="text-slate-500">Seats</dt><dd>{count}</dd></div>
              <div className="flex justify-between text-base font-bold"><dt>Total fare</dt><dd>{formatMoney(total)}</dd></div>
            </dl>

            {holding ? (
              <div className="space-y-2">
                <Button className="w-full" loading={busy === 'confirm'} disabled={Boolean(busy)} onClick={confirmBooking}>
                  Confirm booking
                </Button>
                <Button variant="secondary" className="w-full" loading={busy === 'release'}
                        disabled={Boolean(busy)} onClick={releaseSeats}>
                  Release seats
                </Button>
                <p className="text-xs text-slate-500">No payment is taken. Confirming completes the booking.</p>
              </div>
            ) : (
              <Button className="w-full" loading={busy === 'hold'}
                      disabled={selected.length === 0 || !bookable || Boolean(busy)} onClick={holdSeats}>
                Hold selected seats
              </Button>
            )}

            <Link to={`/passenger/trips/${id}`} className="block text-center text-sm text-blue-600 hover:underline">
              Trip details and stops
            </Link>
          </div>
        </Card>
      </div>
    </div>
  );
}