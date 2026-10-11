import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import api, { errorMessage } from '../api/axios';
import Button from '../components/Button';
import Card from '../components/Card';
import ErrorMessage from '../components/ErrorMessage';
import LinkButton from '../components/LinkButton';
import LoadingSpinner from '../components/LoadingSpinner';
import Modal from '../components/Modal';
import StatusBadge from '../components/StatusBadge';
import { formatDate, formatDateTime, formatMoney, formatTime } from '../utils/format';

function Row({ label, children }) {
  return (
    <div className="flex justify-between gap-4 py-1.5 text-sm">
      <dt className="text-slate-500">{label}</dt>
      <dd className="text-right font-medium">{children}</dd>
    </div>
  );
}

export default function Ticket() {
  const { id } = useParams();
  const [booking, setBooking] = useState(null);
  const [error, setError] = useState('');
  const [confirming, setConfirming] = useState(false);
  const [cancelling, setCancelling] = useState(false);

  useEffect(() => {
    let active = true;
    api.get(`/bookings/${id}`)
      .then((res) => active && setBooking(res.data.data))
      .catch((err) => active && setError(errorMessage(err)));
    return () => { active = false; };
  }, [id]);

  const cancel = async () => {
    setCancelling(true);
    try {
      const res = await api.put(`/bookings/${id}/cancel`);
      setBooking(res.data.data);
      setError('');
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setCancelling(false);
      setConfirming(false);
    }
  };

  if (!booking) {
    return error ? <ErrorMessage message={error} /> : <LoadingSpinner center size="lg" />;
  }

  const confirmed = booking.status === 'CONFIRMED';

  return (
    <div className="mx-auto max-w-xl space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Your ticket</h1>
        <Link to="/passenger/bookings" className="text-sm text-blue-600 hover:underline">All bookings</Link>
      </div>
      <ErrorMessage message={error} />

      {!confirmed && (
        <ErrorMessage message={`This booking was cancelled${booking.cancelledAt ? ` on ${formatDateTime(booking.cancelledAt)}` : ''}. The ticket is no longer valid.`} />
      )}

      <Card>
        <div className="flex items-center justify-between border-b border-dashed border-slate-300 pb-3">
          <div>
            <p className="text-xs uppercase tracking-wide text-slate-500">Booking reference</p>
            <p className="font-mono text-lg font-bold">{booking.bookingReference}</p>
          </div>
          <StatusBadge status={booking.status} />
        </div>

        <dl className="py-3">
          <Row label="Passenger">{booking.passengerName}</Row>
          <Row label="Route">{booking.routeName}</Row>
          <Row label="From">{booking.source}</Row>
          <Row label="To">{booking.destination}</Row>
          <Row label="Travel date">{formatDate(booking.tripDate)}</Row>
          <Row label="Departure">{formatTime(booking.departureTime)}</Row>
          <Row label="Arrival">{formatTime(booking.arrivalTime)}</Row>
          <Row label="Bus">{booking.busNumber}</Row>
          <Row label="Seats">{booking.seatNumbers.join(', ')}</Row>
          <Row label="Fare per seat">{formatMoney(booking.farePerSeat)}</Row>
          <Row label="Total fare">{formatMoney(booking.totalAmount)}</Row>
          <Row label="Booked on">{formatDateTime(booking.bookingDate)}</Row>
        </dl>

        {confirmed && booking.qrCode && (
          <div className="flex flex-col items-center border-t border-dashed border-slate-300 pt-4">
            <img src={booking.qrCode} alt={`QR code for ${booking.bookingReference}`} className="h-44 w-44" />
            <p className="mt-2 text-xs text-slate-500">Show this code to the driver when boarding</p>
          </div>
        )}
      </Card>

      <div className="flex flex-wrap gap-2 print:hidden">
        {confirmed && <LinkButton to={`/passenger/trips/${booking.tripId}/track`}>Track bus</LinkButton>}
        {confirmed && <Button variant="secondary" onClick={() => window.print()}>Print</Button>}
        {booking.cancellable && <Button variant="danger" onClick={() => setConfirming(true)}>Cancel booking</Button>}
      </div>

      <Modal
        open={confirming}
        title="Cancel this booking?"
        onClose={() => setConfirming(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setConfirming(false)}>Keep booking</Button>
            <Button variant="danger" loading={cancelling} onClick={cancel}>Cancel booking</Button>
          </>
        }
      >
        <p className="text-sm text-slate-600">The seats are released immediately and the ticket stops being valid.</p>
      </Modal>
    </div>
  );
}