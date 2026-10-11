import Card from './Card';
import LinkButton from './LinkButton';
import { formatDate, formatMoney, formatTime } from '../utils/format';

export default function TripCard({ trip }) {
  const soldOut = trip.availableSeats === 0;
  return (
    <Card className="card-hover">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-lg font-semibold">{trip.source} → {trip.destination}</p>
          <p className="text-sm text-slate-500">{trip.routeName} · Bus {trip.busNumber}</p>
          <p className="mt-2 text-sm">
            {formatDate(trip.tripDate)} · {formatTime(trip.departureTime)} – {formatTime(trip.arrivalTime)}
          </p>
        </div>
        <div className="sm:text-right">
          <p className="text-xl font-bold text-blue-700">{formatMoney(trip.fare)}</p>
          <p className={`text-sm ${soldOut ? 'text-red-600' : 'text-green-700'}`}>
            {soldOut ? 'Sold out' : `${trip.availableSeats} of ${trip.totalSeats} seats left`}
          </p>
          <div className="mt-3 flex gap-2 sm:justify-end">
            <LinkButton to={`/passenger/trips/${trip.tripId}`} variant="secondary">Details</LinkButton>
            {!soldOut && <LinkButton to={`/passenger/trips/${trip.tripId}/seats`}>Select seats</LinkButton>}
          </div>
        </div>
      </div>
    </Card>
  );
}