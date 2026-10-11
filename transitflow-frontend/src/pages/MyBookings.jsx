import { useCallback, useEffect, useState } from 'react';
import api, { errorMessage } from '../api/axios';
import BookingCard from '../components/BookingCard';
import Button from '../components/Button';
import Card from '../components/Card';
import ErrorMessage from '../components/ErrorMessage';
import LinkButton from '../components/LinkButton';
import LoadingSpinner from '../components/LoadingSpinner';
import Modal from '../components/Modal';

export default function MyBookings() {
  const [bookings, setBookings] = useState(null);
  const [error, setError] = useState('');
  const [target, setTarget] = useState(null);
  const [cancelling, setCancelling] = useState(false);

   const load = useCallback(() => api.get('/bookings/my')
    .then((res) => setBookings(res.data.data))
    .catch((err) => setError(errorMessage(err))), []);
    
useEffect(() => {
    let active = true;
    api.get('/bookings/my')
      .then((res) => active && setBookings(res.data.data))
      .catch((err) => active && setError(errorMessage(err)));
    return () => { active = false; };
  }, []);

  const confirmCancel = async () => {
    setCancelling(true);
    setError('');
    try {
      await api.put(`/bookings/${target.id}/cancel`);
      setTarget(null);
      await load();
    } catch (err) {
      setError(errorMessage(err));
      setTarget(null);
      await load();
    } finally {
      setCancelling(false);
    }
  };

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">My bookings</h1>
      <ErrorMessage message={error} />
      {!bookings && !error && <LoadingSpinner center />}

      {bookings && bookings.length === 0 && (
        <Card>
          <p className="mb-3 text-slate-600">You have no bookings yet.</p>
          <LinkButton to="/passenger/search">Search trips</LinkButton>
        </Card>
      )}

      <div className="space-y-3">
        {bookings?.map((b) => <BookingCard key={b.id} booking={b} onCancel={setTarget} />)}
      </div>

      <Modal
        open={Boolean(target)}
        title="Cancel this booking?"
        onClose={() => setTarget(null)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setTarget(null)}>Keep booking</Button>
            <Button variant="danger" loading={cancelling} onClick={confirmCancel}>Cancel booking</Button>
          </>
        }
      >
        <p className="text-sm text-slate-600">
          {target?.bookingReference} ({target?.source} → {target?.destination}). The seats are released
          immediately and the ticket stops being valid.
        </p>
      </Modal>
    </div>
  );
}