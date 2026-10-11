import { useEffect, useRef, useState } from 'react';

/** Counts down to endsAt (a local timestamp in ms) and calls onExpire once at zero. */
export default function HoldTimer({ endsAt, seatNumbers, onExpire }) {
  const [now, setNow] = useState(() => Date.now());
  const fired = useRef(false);

  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);

  const remaining = Math.max(0, Math.ceil((endsAt - now) / 1000));

  useEffect(() => {
    if (remaining === 0 && !fired.current) {
      fired.current = true;
      onExpire();
    }
  }, [remaining, onExpire]);

  const mm = String(Math.floor(remaining / 60)).padStart(2, '0');
  const ss = String(remaining % 60).padStart(2, '0');
  const urgent = remaining <= 60;

  return (
    <div className={`rounded-lg border px-4 py-3 ${urgent ? 'border-red-200 bg-red-50' : 'border-amber-200 bg-amber-50'}`}>
      <p className="text-sm text-slate-700">Seat{seatNumbers.length > 1 ? 's' : ''} {seatNumbers.join(', ')}</p>
      <p className={`text-2xl font-bold tabular-nums ${urgent ? 'text-red-600' : 'text-amber-700'}`}>
        Held for: {mm}:{ss}
      </p>
    </div>
  );
}