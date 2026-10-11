import { useState } from 'react';
import api, { errorMessage, fieldErrors } from '../api/axios';
import Button from '../components/Button';
import ConfirmModal from '../components/ConfirmModal';
import ErrorMessage from '../components/ErrorMessage';
import Input from '../components/Input';
import LinkButton from '../components/LinkButton';
import LoadingSpinner from '../components/LoadingSpinner';
import Modal from '../components/Modal';
import StatusBadge from '../components/StatusBadge';
import Table from '../components/Table';
import { useApiList } from '../hooks/useApiList';

const EMPTY = { busNumber: '', registrationNumber: '', capacity: '16' };

export default function ManageBuses() {
  const { data, error, reload } = useApiList('/admin/buses');
  const [editing, setEditing] = useState(null); // null | 'new' | a bus
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [toDeactivate, setToDeactivate] = useState(null);
  const [actionError, setActionError] = useState('');

  const openNew = () => { setForm(EMPTY); setErrors({}); setFormError(''); setEditing('new'); };
  const openEdit = (bus) => {
    setForm({ busNumber: bus.busNumber, registrationNumber: bus.registrationNumber, capacity: String(bus.capacity) });
    setErrors({}); setFormError(''); setEditing(bus);
  };
  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const save = async (e) => {
    e.preventDefault();
    setSaving(true); setErrors({}); setFormError('');
    const body = {
      busNumber: form.busNumber.trim(),
      registrationNumber: form.registrationNumber.trim(),
      capacity: Number(form.capacity),
    };
    try {
      if (editing === 'new') await api.post('/admin/buses', body);
      else await api.put(`/admin/buses/${editing.id}`, { ...body, status: editing.status });
      setEditing(null);
      reload();
    } catch (err) {
      setErrors(fieldErrors(err));
      setFormError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const activate = async (bus) => {
    setActionError('');
    try {
      await api.put(`/admin/buses/${bus.id}`, { ...bus, status: 'ACTIVE' });
      reload();
    } catch (err) { setActionError(errorMessage(err)); }
  };

  const deactivate = async () => {
    setSaving(true); setActionError('');
    try {
      await api.delete(`/admin/buses/${toDeactivate.id}`);
      reload();
    } catch (err) { setActionError(errorMessage(err)); }
    finally { setSaving(false); setToDeactivate(null); }
  };

  const columns = [
    { key: 'busNumber', header: 'Bus number' },
    { key: 'registrationNumber', header: 'Registration' },
    { key: 'capacity', header: 'Capacity' },
    { key: 'status', header: 'Status', render: (b) => <StatusBadge status={b.status} /> },
    {
      key: 'actions', header: '',
      render: (b) => (
        <div className="flex gap-2">
          <LinkButton to={`/admin/buses/${b.id}/seats`} variant="secondary">Seats</LinkButton>
          <Button variant="secondary" onClick={() => openEdit(b)}>Edit</Button>
          {b.status === 'ACTIVE'
            ? <Button variant="danger" onClick={() => setToDeactivate(b)}>Deactivate</Button>
            : <Button variant="secondary" onClick={() => activate(b)}>Activate</Button>}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Buses</h1>
        <Button onClick={openNew}>Add bus</Button>
      </div>
      <ErrorMessage message={error || actionError} />
      {!data && !error && <LoadingSpinner center />}
      {data && <Table columns={columns} rows={data} empty="No buses yet" />}

      <Modal open={Boolean(editing)} title={editing === 'new' ? 'Add bus' : 'Edit bus'}
             onClose={() => setEditing(null)}
             footer={<Button type="submit" form="bus-form" loading={saving}>Save</Button>}>
        <form id="bus-form" onSubmit={save} className="space-y-4">
          <ErrorMessage message={formError} />
          <Input label="Bus number" name="busNumber" value={form.busNumber} onChange={onChange} error={errors.busNumber} />
          <Input label="Registration number" name="registrationNumber" value={form.registrationNumber}
                 onChange={onChange} error={errors.registrationNumber} />
          <Input label="Capacity" name="capacity" type="number" min="1" max="120" value={form.capacity}
                 onChange={onChange} error={errors.capacity} disabled={editing !== 'new'} />
          {editing !== 'new' && (
            <p className="text-xs text-slate-500">Capacity follows the seat layout. Change it on the Seats page.</p>
          )}
        </form>
      </Modal>

      <ConfirmModal open={Boolean(toDeactivate)} title="Deactivate this bus?" loading={saving}
                    text={`${toDeactivate?.busNumber} can no longer be assigned to trips. Existing scheduled trips must be cancelled first.`}
                    confirmLabel="Deactivate" onConfirm={deactivate} onClose={() => setToDeactivate(null)} />
    </div>
  );
}
