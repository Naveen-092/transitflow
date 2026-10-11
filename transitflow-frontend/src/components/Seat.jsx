/** One seat button. Colours: available (green), selected (blue), yours-held (indigo), held (amber), booked (grey). */
export default function Seat({ seat, selected, disabled, onToggle }) {
  const mine = seat.status === 'HELD' && seat.heldByMe;
  const clickable = seat.status === 'AVAILABLE' && !disabled;

  let style;
  if (selected) style = 'border-blue-700 bg-blue-600 text-white';
  else if (mine) style = 'border-indigo-600 bg-indigo-500 text-white';
  else if (seat.status === 'AVAILABLE') style = 'border-green-400 bg-green-50 text-green-800 hover:bg-green-100';
  else if (seat.status === 'HELD') style = 'border-amber-400 bg-amber-100 text-amber-800';
  else style = 'border-slate-400 bg-slate-300 text-slate-500';

  const label = mine ? 'held by you' : seat.status.toLowerCase();
  return (
    <button
      type="button"
      disabled={!clickable}
      onClick={() => onToggle(seat.tripSeatId)}
      aria-label={`Seat ${seat.seatNumber}, ${label}`}
      aria-pressed={selected}
      title={`Seat ${seat.seatNumber} (${seat.seatType.toLowerCase()}) - ${label}`}
      className={`h-11 w-11 rounded-md border-2 text-sm font-semibold transition disabled:cursor-not-allowed ${style}`}
    >
      {seat.seatNumber}
    </button>
  );
}