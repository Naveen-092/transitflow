import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Bus } from 'lucide-react';
import api, { errorMessage } from '../api/axios';
import Button from '../components/Button';
import ErrorMessage from '../components/ErrorMessage';
import LinkButton from '../components/LinkButton';
import LoadingSpinner from '../components/LoadingSpinner';
import Modal from '../components/Modal';
import StatusBadge from '../components/StatusBadge';
import { useToast } from '../hooks/useToast';
import { formatDate, formatDateTime, formatMoney, formatTime } from '../utils/format';

function Field({ label, children }) {
  return (
    <div>
      <p className="text-[11px] uppercase tracking-wide text-slate-400">{label}</p>
      <p className="text-sm font-semibold">{children}</p>
    </div>
  );
}

export default function Ticket() {
  const toast = useToast();
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
      toast.success('Booking cancelled. Your seats were released.');
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
      <div className="flex items-center justify-between print:hidden">
        <h1 className="text-2xl font-bold">Your ticket</h1>
        <Link to="/passenger/bookings" className="text-sm text-blue-600 hover:underline">All bookings</Link>
      </div>
      <ErrorMessage message={error} />

      {!confirmed && (
        <ErrorMessage message={`This booking was cancelled${booking.cancelledAt ? ` on ${formatDateTime(booking.cancelledAt)}` : ''}. The ticket is no longer valid.`} />
      )}

      <div className={`overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-lg ${confirmed ? '' : 'opacity-70 grayscale'}`}>
        <div className="flex items-center justify-between bg-gradient-to-r from-blue-700 to-indigo-600 px-6 py-4 text-white">
          <div className="flex items-center gap-2 font-bold"><Bus size={20} /> TransitFlow</div>
          <span className="text-xs uppercase tracking-widest text-blue-100">Boarding pass</span>
        </div>

        <div className="space-y-5 px-6 py-5">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="text-[11px] uppercase tracking-wide text-slate-400">From</p>
              <p className="text-2xl font-bold">{booking.source}</p>
            </div>
            <div className="flex flex-1 items-center">
              <span className="h-px flex-1 bg-slate-300" />
              <Bus size={20} className="mx-2 text-blue-600" />
              <span className="h-px flex-1 bg-slate-300" />
            </div>
            <div className="text-right">
              <p className="text-[11px] uppercase tracking-wide text-slate-400">To</p>
              <p className="text-2xl font-bold">{booking.destination}</p>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
            <Field label="Passenger">{booking.passengerName}</Field>
            <Field label="Date">{formatDate(booking.tripDate)}</Field>
            <Field label="Departure">{formatTime(booking.departureTime)}</Field>
            <Field label="Arrival">{formatTime(booking.arrivalTime)}</Field>
            <Field label="Bus">{booking.busNumber}</Field>
            <Field label="Route">{booking.routeName}</Field>
          </div>
        </div>

        <div className="relative border-t-2 border-dashed border-slate-300">
          <span className="absolute -left-3 -top-3 h-6 w-6 rounded-full bg-slate-50" />
          <span className="absolute -right-3 -top-3 h-6 w-6 rounded-full bg-slate-50" />
        </div>

        <div className="flex items-center justify-between gap-4 px-6 py-5">
          <div className="space-y-3">
            <div>
              <p className="text-[11px] uppercase tracking-wide text-slate-400">Seats</p>
              <p className="text-3xl font-bold text-blue-700">{booking.seatNumbers.join(', ')}</p>
            </div>
            <Field label="Total fare">{formatMoney(booking.totalAmount)}</Field>
            <div>
              <p className="text-[11px] uppercase tracking-wide text-slate-400">Reference</p>
              <p className="font-mono text-sm font-bold">{booking.bookingReference}</p>
            </div>
            <StatusBadge status={booking.status} />
          </div>
          {confirmed && booking.qrCode && (
            <div className="text-center">
              <img src={booking.qrCode} alt={`QR code for ${booking.bookingReference}`}
                   className="h-36 w-36 rounded-lg border border-slate-200 p-1" />
              <p className="mt-1 text-[11px] text-slate-500">Show to the driver</p>
            </div>
          )}
        </div>
      </div>

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