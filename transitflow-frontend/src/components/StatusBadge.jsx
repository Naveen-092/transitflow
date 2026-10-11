const COLORS = {
  AVAILABLE: 'bg-green-100 text-green-700',
  HELD: 'bg-amber-100 text-amber-700',
  BOOKED: 'bg-slate-200 text-slate-700',
  SCHEDULED: 'bg-blue-100 text-blue-700',
  STARTED: 'bg-green-100 text-green-700',
  DELAYED: 'bg-amber-100 text-amber-700',
  COMPLETED: 'bg-slate-200 text-slate-700',
  CANCELLED: 'bg-red-100 text-red-700',
  CONFIRMED: 'bg-green-100 text-green-700',
  ACTIVE: 'bg-green-100 text-green-700',
  INACTIVE: 'bg-slate-200 text-slate-600',
};

export default function StatusBadge({ status }) {
  return (
    <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold ${COLORS[status] || 'bg-slate-100 text-slate-600'}`}>
      {status}
    </span>
  );
}