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

const EMPTY = { routeName: '', source: '', destination: '', stopIds: [] };

export default function ManageRoutes() {
  const routes = useApiList('/admin/routes');
  const stops = useApiList('/admin/stops');
  const [editing, setEditing] = useState(null); // null | 'new' | a route
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [toDeactivate, setToDeactivate] = useState(null);
  const [actionError, setActionError] = useState('');

  const stopName = Object.fromEntries((stops.data || []).map((s) => [s.id, s.stopName]));
  const unused = (stops.data || []).filter((s) => !form.stopIds.includes(s.id));

  const open = (route) => {
    setForm(route
      ? { routeName: route.routeName, source: route.source, destination: route.destination,
          stopIds: route.stops.map((s) => s.stopId) }
      : EMPTY);
    setErrors({}); setFormError(''); setEditing(route || 'new');
  };
  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const addStop = (e) => {
    if (e.target.value) setForm((f) => ({ ...f, stopIds: [...f.stopIds, Number(e.target.value)] }));
  };
  const removeStop = (i) => setForm((f) => ({ ...f, stopIds: f.stopIds.filter((_, idx) => idx !== i) }));
  const move = (i, delta) => setForm((f) => {
    const j = i + delta;
    if (j < 0 || j >= f.stopIds.length) return f;
    const ids = [...f.stopIds];
    [ids[i], ids[j]] = [ids[j], ids[i]];
    return { ...f, stopIds: ids };
  });

  const save = async (e) => {
    e.preventDefault();
    setSaving(true); setErrors({}); setFormError('');
    const body = {
      routeName: form.routeName.trim(), source: form.source.trim(),
      destination: form.destination.trim(), stopIds: form.stopIds,
    };
    try {
      if (editing === 'new') await api.post('/admin/routes', body);
      else await api.put(`/admin/routes/${editing.id}`, { ...body, active: editing.active });
      setEditing(null);
      routes.reload();
    } catch (err) {
      setErrors(fieldErrors(err));
      setFormError(errorMessage(err));
    } finally { setSaving(false); }
  };

  const activate = async (r) => {
    setActionError('');
    try {
      await api.put(`/admin/routes/${r.id}`, {
        routeName: r.routeName, source: r.source, destination: r.destination,
        stopIds: r.stops.map((s) => s.stopId), active: true,
      });
      routes.reload();
    } catch (err) { setActionError(errorMessage(err)); }
  };

  const deactivate = async () => {
    setSaving(true); setActionError('');
    try {
      await api.delete(`/admin/routes/${toDeactivate.id}`);
      routes.reload();
    } catch (err) { setActionError(errorMessage(err)); }
    finally { setSaving(false); setToDeactivate(null); }
  };

  const columns = [
    { key: 'routeName', header: 'Route' },
    { key: 'path', header: 'From → To', render: (r) => `${r.source} → ${r.destination}` },
    { key: 'stops', header: 'Stops', render: (r) => r.stops.map((s) => s.stopName).join(' → ') || '-' },
    { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.active ? 'ACTIVE' : 'INACTIVE'} /> },
    {
      key: 'actions', header: '',
      render: (r) => (
        <div className="flex gap-2">
          <Button variant="secondary" onClick={() => open(r)}>Edit</Button>
          {r.active
            ? <Button variant="danger" onClick={() => setToDeactivate(r)}>Deactivate</Button>
            : <Button variant="secondary" onClick={() => activate(r)}>Activate</Button>}
        </div>
      ),
    },
  ];

  const loadError = routes.error || stops.error;
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Routes</h1>
        <Button onClick={() => open(null)} disabled={!stops.data}>Add route</Button>
      </div>
      <ErrorMessage message={loadError || actionError} />
      {(!routes.data || !stops.data) && !loadError && <LoadingSpinner center />}
      {routes.data && <Table columns={columns} rows={routes.data} empty="No routes yet" />}

      <Modal open={Boolean(editing)} title={editing === 'new' ? 'Add route' : 'Edit route'}
             onClose={() => setEditing(null)}
             footer={<Button type="submit" form="route-form" loading={saving}>Save</Button>}>
        <form id="route-form" onSubmit={save} className="space-y-4">
          <ErrorMessage message={formError} />
          <Input label="Route name" name="routeName" value={form.routeName} onChange={onChange} error={errors.routeName} />
          <div className="grid grid-cols-2 gap-3">
            <Input label="Source" name="source" value={form.source} onChange={onChange} error={errors.source} />
            <Input label="Destination" name="destination" value={form.destination} onChange={onChange} error={errors.destination} />
          </div>

          <div>
            <p className="mb-1 text-sm font-medium text-slate-700">Stops, in order</p>
            {form.stopIds.length === 0 && <p className="text-sm text-slate-500">No stops added.</p>}
            <ol className="space-y-1">
              {form.stopIds.map((sid, i) => (
                <li key={sid} className="flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-1.5 text-sm">
                  <span className="w-5 text-slate-500">{i + 1}.</span>
                  <span className="flex-1">{stopName[sid] || `Stop ${sid}`}</span>
                  <button type="button" onClick={() => move(i, -1)} disabled={i === 0}
                          aria-label="Move up" className="px-1 disabled:opacity-30">↑</button>
                  <button type="button" onClick={() => move(i, 1)} disabled={i === form.stopIds.length - 1}
                          aria-label="Move down" className="px-1 disabled:opacity-30">↓</button>
                  <button type="button" onClick={() => removeStop(i)} aria-label="Remove" className="px-1 text-red-600">✕</button>
                </li>
              ))}
            </ol>
            <Select className="mt-2" aria-label="Add a stop" value="" onChange={addStop}>
              <option value="">{unused.length ? 'Add a stop…' : 'All stops added'}</option>
              {unused.map((s) => <option key={s.id} value={s.id}>{s.stopName}</option>)}
            </Select>
          </div>
        </form>
      </Modal>

      <ConfirmModal open={Boolean(toDeactivate)} title="Deactivate this route?" loading={saving}
                    text={`${toDeactivate?.routeName} can no longer be used for new trips. Scheduled trips on it must be cancelled first.`}
                    confirmLabel="Deactivate" onConfirm={deactivate} onClose={() => setToDeactivate(null)} />
    </div>
  );
}