import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import api, { errorMessage, fieldErrors } from '../api/axios';
import Button from '../components/Button';
import Card from '../components/Card';
import ConfirmModal from '../components/ConfirmModal';
import ErrorMessage from '../components/ErrorMessage';
import Input from '../components/Input';
import LoadingSpinner from '../components/LoadingSpinner';
import SeatMap from '../components/SeatMap';
import { useApiList } from '../hooks/useApiList';

export default function ManageSeats() {
  const { id } = useParams();
  const bus = useApiList(`/admin/buses/${id}`);
  const seats = useApiList(`/admin/buses/${id}/seats`);
  const [form, setForm] = useState({ rows: '4', leftColumns: '2', rightColumns: '2' });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [confirming, setConfirming] = useState(false);
  const [saving, setSaving] = useState(false);

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const generate = async () => {
    setSaving(true); setError(''); setErrors({});
    try {
      await api.post(`/admin/buses/${id}/seats/generate`, {
        rows: Number(form.rows),
        leftColumns: Number(form.leftColumns),
        rightColumns: Number(form.rightColumns),
      });
      seats.reload();
      bus.reload();
    } catch (err) {
      setErrors(fieldErrors(err));
      setError(errorMessage(err));
    } finally {
      setSaving(false);
      setConfirming(false);
    }
  };

  const onSubmit = (e) => {
    e.preventDefault();
    if (seats.data?.length > 0) setConfirming(true); // regenerating replaces the layout
    else generate();
  };

  if (!bus.data || !seats.data) {
    const message = bus.error || seats.error;
    return message ? <ErrorMessage message={message} /> : <LoadingSpinner center size="lg" />;
  }

  // The map expects trip-seat fields, so show every seat as available.
  const preview = seats.data.map((s) => ({
    tripSeatId: s.id, seatNumber: s.seatNumber, rowNumber: s.rowNumber,
    columnNumber: s.columnNumber, seatType: s.seatType, status: 'AVAILABLE', heldByMe: false,
  }));

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-bold">Seats: bus {bus.data.busNumber}</h1>
        <p className="text-sm text-slate-600">Capacity {bus.data.capacity} · {seats.data.length} seats configured</p>
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <Card title="Seat layout generator">
          <form onSubmit={onSubmit} className="space-y-4">
            <ErrorMessage message={error} />
            <Input label="Rows" name="rows" type="number" min="1" max="20" value={form.rows} onChange={onChange} error={errors.rows} />
            <Input label="Seats left of the aisle" name="leftColumns" type="number" min="1" max="3"
                   value={form.leftColumns} onChange={onChange} error={errors.leftColumns} />
            <Input label="Seats right of the aisle" name="rightColumns" type="number" min="0" max="3"
                   value={form.rightColumns} onChange={onChange} error={errors.rightColumns} />
            <p className="text-sm text-slate-600">
              Total: {(Number(form.rows) || 0) * ((Number(form.leftColumns) || 0) + (Number(form.rightColumns) || 0))} seats
            </p>
            <Button type="submit" loading={saving} className="w-full">
              {seats.data.length > 0 ? 'Regenerate seats' : 'Generate seats'}
            </Button>
            <p className="text-xs text-slate-500">
              The layout cannot be changed once the bus has any trips. Create a new bus instead.
            </p>
          </form>
        </Card>

        <Card title="Preview" className="lg:col-span-2">
          {preview.length === 0
            ? <p className="text-sm text-slate-500">No seats yet. Generate a layout to see it here.</p>
            : <SeatMap seats={preview} selectedIds={[]} disabled onToggle={() => {}} />}
        </Card>
      </div>

      <Link to="/admin/buses" className="text-sm text-blue-600 hover:underline">Back to buses</Link>

      <ConfirmModal open={confirming} title="Replace the seat layout?" loading={saving}
                    text="The existing seats of this bus are deleted and generated again."
                    confirmLabel="Replace" onConfirm={generate} onClose={() => setConfirming(false)} />
    </div>
  );
}