import { useState } from 'react';
import Button from '../components/Button';
import ErrorMessage from '../components/ErrorMessage';
import Input from '../components/Input';
import LoadingSpinner from '../components/LoadingSpinner';
import Modal from '../components/Modal';
import Select from '../components/Select';
import StatusBadge from '../components/StatusBadge';
import Table from '../components/Table';
import { useApiList } from '../hooks/useApiList';
import { formatDate, formatDateTime, formatMoney, formatTime } from '../utils/format';

function Row({ label, children }) {
  return (
    <div className="flex justify-between gap-4 py-1.5 text-sm">
      <dt className="text-slate-500">{label}</dt>
      <dd className="text-right font-medium">{children}</dd>
    </div>
  );
}

export default function AdminBookings() {
  const [q, setQ] = useState('');
  const [status, setStatus] = useState('');
  const [params, setParams] = useState({});
  const [selected, setSelected] = useState(null);
  const { data, error } = useApiList('/admin/bookings', params);

  const onSearch = (e) => {
    e.preventDefault();
    setParams({ ...(q.trim() && { q: q.trim() }), ...(status && { status }) });
  };

  const columns = [
    { key: 'bookingReference', header: 'Reference', render: (b) => <span className="font-mono">{b.bookingReference}</span> },
    { key: 'passengerName', header: 'Passenger' },
    { key: 'trip', header: 'Trip', render: (b) => `${b.source} → ${b.destination}` },
    { key: 'date', header: 'Travel date', render: (b) => `${formatDate(b.tripDate)}, ${formatTime(b.departureTime)}` },
    { key: 'seats', header: 'Seats', render: (b) => b.seatNumbers.join(', ') },
    { key: 'totalAmount', header: 'Total', render: (b) => formatMoney(b.totalAmount) },
    { key: 'status', header: 'Status', render: (b) => <StatusBadge status={b.status} /> },
    { key: 'actions', header: '', render: (b) => <Button variant="secondary" onClick={() => setSelected(b)}>Details</Button> },
  ];

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">Bookings</h1>
      <form onSubmit={onSearch} className="flex flex-wrap items-end gap-3">
        <Input className="min-w-64 flex-1" label="Search" name="q" placeholder="Reference, passenger name or email"
               value={q} onChange={(e) => setQ(e.target.value)} />
        <Select label="Status" value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">All</option>
          <option value="CONFIRMED">CONFIRMED</option>
          <option value="CANCELLED">CANCELLED</option>
        </Select>
        <Button type="submit">Search</Button>
      </form>

      <ErrorMessage message={error} />
      {!data && !error && <LoadingSpinner center />}
      {data && (
        <>
          <p className="text-sm text-slate-500">Showing the latest {data.length} matching bookings (up to 200).</p>
          <Table columns={columns} rows={data} empty="No bookings match" />
        </>
      )}

      <Modal open={Boolean(selected)} title={selected?.bookingReference} onClose={() => setSelected(null)}>
        {selected && (
          <dl className="divide-y divide-slate-100">
            <Row label="Status"><StatusBadge status={selected.status} /></Row>
            <Row label="Passenger">{selected.passengerName}</Row>
            <Row label="Email">{selected.passengerEmail}</Row>
            <Row label="Phone">{selected.passengerPhone}</Row>
            <Row label="Route">{selected.routeName}</Row>
            <Row label="Journey">{selected.source} → {selected.destination}</Row>
            <Row label="Bus">{selected.busNumber}</Row>
            <Row label="Travel date">{formatDate(selected.tripDate)}, {formatTime(selected.departureTime)}</Row>
            <Row label="Seats">{selected.seatNumbers.join(', ')}</Row>
            <Row label="Total">{formatMoney(selected.totalAmount)}</Row>
            <Row label="Booked on">{formatDateTime(selected.bookingDate)}</Row>
            {selected.cancelledAt && <Row label="Cancelled on">{formatDateTime(selected.cancelledAt)}</Row>}
          </dl>
        )}
      </Modal>
    </div>
  );
}