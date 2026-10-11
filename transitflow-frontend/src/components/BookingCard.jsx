import Button from './Button';
import Card from './Card';
import LinkButton from './LinkButton';
import StatusBadge from './StatusBadge';
import { formatDate, formatMoney, formatTime } from '../utils/format';

export default function BookingCard({ booking, onCancel }) {
  return (
    <Card>
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <span className="font-mono text-sm font-semibold">{booking.bookingReference}</span>
            <StatusBadge status={booking.status} />
          </div>
          <p className="text-lg font-semibold">{booking.source} → {booking.destination}</p>
          <p className="text-sm text-slate-600">
            {formatDate(booking.tripDate)} · {formatTime(booking.departureTime)} · Bus {booking.busNumber}
          </p>
          <p className="text-sm text-slate-600">
            Seats: {booking.seatNumbers.join(', ')} · Total {formatMoney(booking.totalAmount)}
          </p>
        </div>
        <div className="flex gap-2">
          <LinkButton to={`/passenger/bookings/${booking.id}`} variant="secondary">
            {booking.status === 'CONFIRMED' ? 'View ticket' : 'View'}
          </LinkButton>
          {booking.cancellable && onCancel && (
            <Button variant="danger" onClick={() => onCancel(booking)}>Cancel</Button>
          )}
        </div>
      </div>
    </Card>
  );
}