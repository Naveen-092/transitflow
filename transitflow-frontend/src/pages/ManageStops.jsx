import { useState } from 'react';
import api, { errorMessage, fieldErrors } from '../api/axios';
import Button from '../components/Button';
import ConfirmModal from '../components/ConfirmModal';
import ErrorMessage from '../components/ErrorMessage';
import Input from '../components/Input';
import LoadingSpinner from '../components/LoadingSpinner';
import Modal from '../components/Modal';
import Table from '../components/Table';
import { useApiList } from '../hooks/useApiList';

export default function ManageStops() {
  const { data, error, reload } = useApiList('/admin/stops');
  const [editing, setEditing] = useState(null); // null | 'new' | a stop
  const [form, setForm] = useState({ stopName: '', location: '' });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [toDelete, setToDelete] = useState(null);
  const [actionError, setActionError] = useState('');

  const open = (stop) => {
    setForm({ stopName: stop?.stopName || '', location: stop?.location || '' });
    setErrors({}); setFormError(''); setEditing(stop || 'new');
  };
  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const save = async (e) => {
    e.preventDefault();
    setSaving(true); setErrors({}); setFormError('');
    try {
      if (editing === 'new') await api.post('/admin/stops', form);
      else await api.put(`/admin/stops/${editing.id}`, form);
      setEditing(null);
      reload();
    } catch (err) {
      setErrors(fieldErrors(err));
      setFormError(errorMessage(err));
    } finally { setSaving(false); }
  };

  const remove = async () => {
    setSaving(true); setActionError('');
    try {
      await api.delete(`/admin/stops/${toDelete.id}`);
      reload();
    } catch (err) { setActionError(errorMessage(err)); }
    finally { setSaving(false); setToDelete(null); }
  };

  const columns = [
    { key: 'stopName', header: 'Stop name' },
    { key: 'location', header: 'Location', render: (s) => s.location || '-' },
    {
      key: 'actions', header: '',
      render: (s) => (
        <div className="flex gap-2">
          <Button variant="secondary" onClick={() => open(s)}>Edit</Button>
          <Button variant="danger" onClick={() => setToDelete(s)}>Delete</Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Stops</h1>
        <Button onClick={() => open(null)}>Add stop</Button>
      </div>
      <ErrorMessage message={error || actionError} />
      {!data && !error && <LoadingSpinner center />}
      {data && <Table columns={columns} rows={data} empty="No stops yet" />}

      <Modal open={Boolean(editing)} title={editing === 'new' ? 'Add stop' : 'Edit stop'}
             onClose={() => setEditing(null)}
             footer={<Button type="submit" form="stop-form" loading={saving}>Save</Button>}>
        <form id="stop-form" onSubmit={save} className="space-y-4">
          <ErrorMessage message={formError} />
          <Input label="Stop name" name="stopName" value={form.stopName} onChange={onChange} error={errors.stopName} />
          <Input label="Location (optional)" name="location" value={form.location} onChange={onChange} error={errors.location} />
        </form>
      </Modal>

      <ConfirmModal open={Boolean(toDelete)} title="Delete this stop?" loading={saving}
                    text={`${toDelete?.stopName} is removed. A stop used by a route cannot be deleted.`}
                    confirmLabel="Delete" onConfirm={remove} onClose={() => setToDelete(null)} />
    </div>
  );
}