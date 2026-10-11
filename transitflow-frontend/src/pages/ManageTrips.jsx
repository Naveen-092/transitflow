import { useState } from 'react';
import api, { errorMessage, fieldErrors } from '../api/axios';
import Button from '../components/Button';
import ConfirmModal from '../components/ConfirmModal';
import ErrorMessage from '../components/ErrorMessage';
import Input from '../components/Input';
import LoadingSpinner from '../components/LoadingSpinner';
import Modal from '../components/Modal';
import Select from '../components/Select';
import StatusBadge from '../components/StatusBadge';
import Table from '../components/Table';
import { useApiList } from '../hooks/useApiList';
import { formatDate, formatMoney, formatTime, todayISO } from '../utils/format';

const EMPTY = { routeId: '', busId: '', driverId: '', tripDate: '', departureTime: '', arrivalTime: '', fare: '' };
const STATUSES = ['SCHEDULED', 'STARTED', 'DELAYED', 'COMPLETED', 'CANCELLED'];

export default function ManageTrips() {
  const trips = useApiList('/admin/trips');
  const routes = useApiList('/admin/routes');
  const buses = useApiList('/admin/buses');
  const drivers = useApiList('/admin/drivers');

  const [filter, setFilter] = useState('');
  const [editing, setEditing] = useState(null); // null | 'new' | a trip
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [toCancel, setToCancel] = useState(null);
  const [actionError, setActionError] = useState('');

  const ready = routes.data && buses.data && drivers.data;
  const open = (t) => {
    setForm(t ? {
      routeId: String(t.routeId), busId: String(t.busId), driverId: String(t.driverId),
      tripDate: t.tripDate, departureTime: t.departureTime.slice(0, 5),
      arrivalTime: t.arrivalTime.slice(0, 5), fare: String(t.fare),
    } : EMPTY);
    setErrors({}); setFormError(''); setEditing(t || 'new');
  };
  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const save = async (e) => {
    e.preventDefault();
    setSaving(true); setErrors({}); setFormError('');
    const body = {
      routeId: Number(form.routeId) || null, busId: Number(form.busId) || null,
      driverId: Number(form.driverId) || null, tripDate: form.tripDate || null,
      departureTime: form.departureTime || null, arrivalTime: form.arrivalTime || null,
      fare: form.fare === '' ? null : Number(form.fare),
    };
    try {
      if (editing === 'new') await api.post('/admin/trips', body);
      else await api.put(`/admin/trips/${editing.id}`, body);
      setEditing(null);
      trips.reload();
    } catch (err) {
      setErrors(fieldErrors(err));
      setFormError(errorMessage(err)); // e.g. the bus or driver overlap message (409)
    } finally { setSaving(false); }
  };

  const cancelTrip = async () => {
    setSaving(true); setActionError('');
    try {
      await api.delete(`/admin/trips/${toCancel.id}`);
      trips.reload();
    } catch (err) { setActionError(errorMessage(err)); }
    finally { setSaving(false); setToCancel(null); }
  };

  const rows = (trips.data || []).filter((t) => !filter || t.status === filter);
  const columns = [
    { key: 'id', header: 'ID' },
    { key: 'route', header: 'Route', render: (t) => `${t.source} → ${t.destination}` },
    { key: 'when', header: 'When', render: (t) => `${formatDate(t.tripDate)}, ${formatTime(t.departureTime)} – ${formatTime(t.arrivalTime)}` },
    { key: 'busNumber', header: 'Bus' },
    { key: 'driverName', header: 'Driver' },
    { key: 'fare', header: 'Fare', render: (t) => formatMoney(t.fare) },
    { key: 'status', header: 'Status', render: (t) => <StatusBadge status={t.status} /> },
    {
      key: 'actions', header: '',
      render: (t) => (
        <div className="flex gap-2">
          {t.status === 'SCHEDULED' && <Button variant="secondary" onClick={() => open(t)}>Edit</Button>}
          {['SCHEDULED', 'STARTED', 'DELAYED'].includes(t.status) && (
            <Button variant="danger" onClick={() => setToCancel(t)}>Cancel</Button>
          )}
        </div>
      ),
    },
  ];

  const loadError = trips.error || routes.error || buses.error || drivers.error;
  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold">Trips</h1>
        <div className="flex items-center gap-3">
          <Select aria-label="Filter by status" value={filter} onChange={(e) => setFilter(e.target.value)}>
            <option value="">All statuses</option>
            {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
          </Select>
          <Button onClick={() => open(null)} disabled={!ready}>Create trip</Button>
        </div>
      </div>
      <ErrorMessage message={loadError || actionError} />
      {(!trips.data || !ready) && !loadError && <LoadingSpinner center />}
      {trips.data && <Table columns={columns} rows={rows} empty="No trips found" />}

      <Modal open={Boolean(editing)} title={editing === 'new' ? 'Create trip' : `Edit trip #${editing?.id}`}
             onClose={() => setEditing(null)}
             footer={<Button type="submit" form="trip-form" loading={saving}>Save</Button>}>
        <form id="trip-form" onSubmit={save} className="space-y-4">
          <ErrorMessage message={formError} />
          <Select label="Route" name="routeId" value={form.routeId} onChange={onChange} error={errors.routeId}>
            <option value="">Select a route</option>
            {(routes.data || []).filter((r) => r.active).map((r) => (
              <option key={r.id} value={r.id}>{r.routeName} ({r.source} → {r.destination})</option>
            ))}
          </Select>
          <Select label="Bus" name="busId" value={form.busId} onChange={onChange} error={errors.busId}>
            <option value="">Select a bus</option>
            {(buses.data || []).filter((b) => b.status === 'ACTIVE').map((b) => (
              <option key={b.id} value={b.id}>{b.busNumber} ({b.capacity} seats)</option>
            ))}
          </Select>
          <Select label="Driver" name="driverId" value={form.driverId} onChange={onChange} error={errors.driverId}>
            <option value="">Select a driver</option>
            {(drivers.data || []).filter((d) => d.active).map((d) => (
              <option key={d.id} value={d.id}>{d.name}</option>
            ))}
          </Select>
          <Input label="Date" name="tripDate" type="date" min={todayISO()} value={form.tripDate}
                 onChange={onChange} error={errors.tripDate} />
          <div className="grid grid-cols-2 gap-3">
            <Input label="Departure" name="departureTime" type="time" value={form.departureTime}
                   onChange={onChange} error={errors.departureTime} />
            <Input label="Arrival" name="arrivalTime" type="time" value={form.arrivalTime}
                   onChange={onChange} error={errors.arrivalTime} />
          </div>
          <Input label="Fare per seat (₹)" name="fare" type="number" min="1" step="0.01" value={form.fare}
                 onChange={onChange} error={errors.fare} />
        </form>
      </Modal>

      <ConfirmModal open={Boolean(toCancel)} title="Cancel this trip?" loading={saving}
                    text="All confirmed bookings on this trip are cancelled and the seats are released. This cannot be undone."
                    confirmLabel="Cancel trip" onConfirm={cancelTrip} onClose={() => setToCancel(null)} />
    </div>
  );
}