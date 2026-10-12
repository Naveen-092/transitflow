import { useEffect, useRef, useState } from 'react';

const R = 28;
const CIRC = 2 * Math.PI * R;

/** Counts down to endsAt (a local timestamp in ms) and calls onExpire once at zero. */
export default function HoldTimer({ endsAt, seatNumbers, onExpire, totalSeconds = 300 }) {
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

  const fraction = Math.min(1, remaining / totalSeconds);
  const mm = String(Math.floor(remaining / 60)).padStart(2, '0');
  const ss = String(remaining % 60).padStart(2, '0');
  const urgent = remaining <= 60;

  return (
    <div className={`flex items-center gap-4 rounded-xl border px-4 py-3 ${urgent ? 'border-red-200 bg-red-50' : 'border-amber-200 bg-amber-50'}`}>
      <div className="relative h-[72px] w-[72px] shrink-0">
        <svg width="72" height="72" viewBox="0 0 72 72" className="-rotate-90" aria-hidden="true">
          <circle cx="36" cy="36" r={R} fill="none" strokeWidth="6" className="stroke-slate-200" />
          <circle cx="36" cy="36" r={R} fill="none" strokeWidth="6" strokeLinecap="round"
                  strokeDasharray={CIRC} strokeDashoffset={CIRC * (1 - fraction)}
                  className={`transition-all duration-1000 ease-linear ${urgent ? 'stroke-red-500' : 'stroke-amber-500'}`} />
        </svg>
        <span className={`absolute inset-0 flex items-center justify-center text-sm font-bold tabular-nums ${urgent ? 'text-red-600' : 'text-amber-700'}`}>
          {mm}:{ss}
        </span>
      </div>
      <div>
        <p className="text-xs uppercase tracking-wide text-slate-500">Seats held for you</p>
        <p className="text-lg font-bold text-slate-800">{seatNumbers.join(', ')}</p>
        <p className="text-xs text-slate-500">{urgent ? 'Hurry, confirm now' : 'Confirm before the timer ends'}</p>
      </div>
    </div>
  );
}