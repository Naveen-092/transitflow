import Card from './Card';
import LinkButton from './LinkButton';
import StatusBadge from './StatusBadge';
import { isRunning } from '../utils/trip';
import { formatDate, formatTime } from '../utils/format';

export default function DriverTripCard({ trip }) {
  const running = isRunning(trip.status);
  return (
    <Card className={running ? 'ring-2 ring-green-300' : ''}>
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <p className="text-lg font-semibold">{trip.source} → {trip.destination}</p>
            <StatusBadge status={trip.status} />
          </div>
          <p className="text-sm text-slate-500">
            {trip.routeName} · Bus {trip.busNumber} ({trip.registrationNumber})
          </p>
          <p className="text-sm">
            {formatDate(trip.tripDate)} · {formatTime(trip.departureTime)} – {formatTime(trip.arrivalTime)}
          </p>
          <p className="text-sm text-slate-600">
            {trip.passengerCount} of {trip.capacity} seats booked
            {trip.canStart && <span className="ml-2 font-medium text-green-700">Ready to start</span>}
          </p>
        </div>
        <div className="flex gap-2">
          <LinkButton to={`/driver/trips/${trip.tripId}/passengers`} variant="secondary">Passengers</LinkButton>
          <LinkButton to={`/driver/trips/${trip.tripId}`}>Open</LinkButton>
        </div>
      </div>
    </Card>
  );
}