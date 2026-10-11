import { useState } from 'react';
import api, { errorMessage, fieldErrors } from '../api/axios';
import Button from '../components/Button';
import ErrorMessage from '../components/ErrorMessage';
import Input from '../components/Input';
import LoadingSpinner from '../components/LoadingSpinner';
import Modal from '../components/Modal';
import StatusBadge from '../components/StatusBadge';
import Table from '../components/Table';
import { useApiList } from '../hooks/useApiList';

const EMPTY = { name: '', email: '', phone: '', password: '' };

export default function ManageDrivers() {
  const { data, error, reload } = useApiList('/admin/drivers');
  const [editing, setEditing] = useState(null); // null | 'new' | a driver
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [actionError, setActionError] = useState('');

  const open = (driver) => {
    setForm(driver ? { ...EMPTY, name: driver.name, phone: driver.phone } : EMPTY);
    setErrors({}); setFormError(''); setEditing(driver || 'new');
  };
  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const save = async (e) => {
    e.preventDefault();
    setSaving(true); setErrors({}); setFormError('');
    try {
      if (editing === 'new') {
        await api.post('/admin/drivers', {
          name: form.name.trim(), email: form.email.trim(), phone: form.phone, password: form.password,
        });
      } else {
        await api.put(`/admin/drivers/${editing.id}`, {
          name: form.name.trim(), phone: form.phone, active: editing.active,
        });
      }
      setEditing(null);
      reload();
    } catch (err) {
      setErrors(fieldErrors(err));
      setFormError(errorMessage(err));
    } finally { setSaving(false); }
  };

  const toggleActive = async (d) => {
    setActionError('');
    try {
      await api.put(`/admin/drivers/${d.id}`, { name: d.name, phone: d.phone, active: !d.active });
      reload();
    } catch (err) { setActionError(errorMessage(err)); }
  };

  const columns = [
    { key: 'name', header: 'Name' },
    { key: 'email', header: 'Email' },
    { key: 'phone', header: 'Phone' },
    { key: 'status', header: 'Status', render: (d) => <StatusBadge status={d.active ? 'ACTIVE' : 'INACTIVE'} /> },
    {
      key: 'actions', header: '',
      render: (d) => (
        <div className="flex gap-2">
          <Button variant="secondary" onClick={() => open(d)}>Edit</Button>
          <Button variant={d.active ? 'danger' : 'secondary'} onClick={() => toggleActive(d)}>
            {d.active ? 'Deactivate' : 'Activate'}
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Drivers</h1>
        <Button onClick={() => open(null)}>Add driver</Button>
      </div>
      <ErrorMessage message={error || actionError} />
      {!data && !error && <LoadingSpinner center />}
      {data && <Table columns={columns} rows={data} empty="No drivers yet" />}

      <Modal open={Boolean(editing)} title={editing === 'new' ? 'Add driver' : 'Edit driver'}
             onClose={() => setEditing(null)}
             footer={<Button type="submit" form="driver-form" loading={saving}>Save</Button>}>
        <form id="driver-form" onSubmit={save} className="space-y-4">
          <ErrorMessage message={formError} />
          <Input label="Full name" name="name" value={form.name} onChange={onChange} error={errors.name} />
          {editing === 'new' && (
            <Input label="Email" name="email" type="email" value={form.email} onChange={onChange} error={errors.email} />
          )}
          <Input label="Phone (10 digits)" name="phone" maxLength={10} value={form.phone}
                 onChange={onChange} error={errors.phone} />
          {editing === 'new' && (
            <Input label="Temporary password (min 8)" name="password" type="password" value={form.password}
                   onChange={onChange} error={errors.password} />
          )}
        </form>
      </Modal>
    </div>
  );
}