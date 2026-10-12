import { useMemo } from 'react';
import { Disc3 } from 'lucide-react';
import Seat from './Seat';

const LEGEND = [
  ['border-green-400 bg-green-50', 'Available'],
  ['border-blue-700 bg-blue-600', 'Selected'],
  ['border-indigo-600 bg-indigo-500', 'Held by you'],
  ['border-amber-400 bg-amber-100', 'Held by someone'],
  ['border-slate-400 bg-slate-300', 'Booked'],
];

export default function SeatMap({ seats, selectedIds, disabled, onToggle }) {
  const rows = useMemo(() => {
    const byRow = new Map();
    seats.forEach((s) => {
      if (!byRow.has(s.rowNumber)) byRow.set(s.rowNumber, []);
      byRow.get(s.rowNumber).push(s);
    });
    return [...byRow.entries()]
      .sort((a, b) => a[0] - b[0])
      .map(([row, list]) => ({ row, seats: list.sort((a, b) => a.columnNumber - b.columnNumber) }));
  }, [seats]);

  return (
    <div>
      <div className="mx-auto w-fit rounded-b-2xl rounded-t-[3rem] border-2 border-slate-300 bg-gradient-to-b from-slate-100 to-white px-5 pb-5 pt-4 shadow-inner">
        <div className="mb-3 h-2 rounded-full bg-sky-200/80" aria-hidden="true" />
        <div className="mb-4 flex items-center justify-between">
          <span className="text-[10px] font-semibold uppercase tracking-wider text-slate-400">Entrance</span>
          <span className="flex items-center gap-1.5 rounded bg-slate-200 px-3 py-1 text-xs font-semibold tracking-wide text-slate-600">
            <Disc3 size={14} /> DRIVER
          </span>
        </div>
        <div className="space-y-2">
          {rows.map(({ row, seats: rowSeats }) => (
            <div key={row} className="flex items-center justify-center gap-2">
              {rowSeats.map((seat, i) => {
                const next = rowSeats[i + 1];
                const aisleAfter = seat.seatType === 'AISLE' && next?.seatType === 'AISLE';
                return (
                  <div key={seat.tripSeatId} className="flex items-center gap-2">
                    <Seat seat={seat} selected={selectedIds.includes(seat.tripSeatId)}
                          disabled={disabled} onToggle={onToggle} />
                    {aisleAfter && <span className="w-6" aria-hidden="true" />}
                  </div>
                );
              })}
            </div>
          ))}
        </div>
      </div>

      <ul className="mt-4 flex flex-wrap justify-center gap-x-4 gap-y-2 text-xs text-slate-600">
        {LEGEND.map(([color, text]) => (
          <li key={text} className="flex items-center gap-1.5">
            <span className={`inline-block h-4 w-4 rounded border-2 ${color}`} />
            {text}
          </li>
        ))}
      </ul>
    </div>
  );
}