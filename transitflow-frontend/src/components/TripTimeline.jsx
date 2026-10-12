import { useEffect, useState } from 'react';
import { Bus, Check } from 'lucide-react';

/** Progress is ESTIMATED from the schedule: stops have no coordinates, so GPS cannot place the bus on them. */
export default function TripTimeline({ stops, trip }) {
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 30000);
    return () => clearInterval(timer);
  }, []);

  if (stops.length === 0) {
    return <p className="text-sm text-slate-500">No stops listed for this route.</p>;
  }

  const running = trip.status === 'STARTED' || trip.status === 'DELAYED';
  const done = trip.status === 'COMPLETED';
  const start = new Date(`${trip.tripDate}T${trip.departureTime}`).getTime();
  const end = new Date(`${trip.tripDate}T${trip.arrivalTime}`).getTime();
  const fraction = done ? 1 : running ? Math.min(1, Math.max(0, (now - start) / (end - start))) : 0;
  const last = stops.length - 1;
  // index of the last stop the bus has reached (-1 = not started)
  const reached = done ? last : running ? Math.floor(fraction * last) : -1;

  return (
    <div>
      <ol>
        {stops.map((stop, i) => {
          const passed = i <= reached;
          const isBus = running && i === reached;
          const isNext = running && i === reached + 1;
          return (
            <li key={stop.stopId} className="relative flex gap-3 pb-6 last:pb-0">
              {i < last && (
                <span className={`absolute left-[11px] top-6 h-full w-0.5 ${i < reached ? 'bg-green-500' : 'bg-slate-200'}`} />
              )}
              <span className={`z-10 flex h-6 w-6 shrink-0 items-center justify-center rounded-full border-2 ${
                isBus ? 'border-blue-600 bg-blue-600 text-white'
                  : passed ? 'border-green-500 bg-green-500 text-white'
                  : isNext ? 'animate-pulse border-blue-400 bg-white'
                  : 'border-slate-300 bg-white'}`}>
                {isBus ? <Bus size={12} /> : passed ? <Check size={12} /> : null}
              </span>
              <div>
                <p className={`text-sm font-medium ${passed || isNext ? 'text-slate-900' : 'text-slate-500'}`}>{stop.stopName}</p>
                {isBus && <p className="text-xs text-blue-600">Bus is here or just passed</p>}
                {isNext && <p className="text-xs text-slate-500">Next stop</p>}
              </div>
            </li>
          );
        })}
      </ol>
      <p className="mt-4 text-xs text-slate-400">Progress is estimated from the schedule.</p>
    </div>
  );
}