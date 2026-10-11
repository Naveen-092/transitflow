import { useState } from 'react';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { errorMessage } from '../api/axios';
import Button from '../components/Button';
import ErrorMessage from '../components/ErrorMessage';
import Input from '../components/Input';
import { useAuth } from '../hooks/useAuth';
import { roleHome } from '../navConfig';
import { Bus, ShieldCheck, Zap } from 'lucide-react';

export default function Login() {
  const { user, login } = useAuth();
  const location = useLocation();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (user) {
    const home = roleHome(user.role);
    const from = location.state?.from?.pathname;
    return <Navigate to={from && from.startsWith(home) ? from : home} replace />;
  }

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await login(form.email.trim(), form.password);
    } catch (err) {
      setError(errorMessage(err));
      setSubmitting(false);
    }
  };

  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <div className="relative hidden flex-col justify-between overflow-hidden bg-gradient-to-br from-blue-700 via-blue-600 to-indigo-700 p-10 text-white lg:flex">
        <div className="flex items-center gap-2 text-xl font-bold">
          <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-white/15"><Bus size={22} /></span>
          TransitFlow
        </div>
        <div className="space-y-6">
          <h2 className="text-4xl font-bold leading-tight">Every seat. Every bus.<br />Always in sync.</h2>
          <ul className="space-y-3 text-blue-100">
            <li className="flex items-center gap-3"><Zap size={18} /> Live seat availability across every screen</li>
            <li className="flex items-center gap-3"><ShieldCheck size={18} /> No double bookings, ever</li>
            <li className="flex items-center gap-3"><Bus size={18} /> Real-time bus tracking and QR tickets</li>
          </ul>
        </div>
        <p className="text-sm text-blue-200">Campus and corporate shuttle management</p>
        <div className="pointer-events-none absolute -bottom-24 -right-24 h-80 w-80 rounded-full bg-white/10" />
        <div className="pointer-events-none absolute -right-8 top-1/3 h-40 w-40 rounded-full bg-white/10" />
      </div>

      <div className="flex items-center justify-center p-6">
        <div className="w-full max-w-sm">
          <div className="mb-8 flex items-center gap-2 lg:hidden">
            <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-600 text-white"><Bus size={20} /></span>
            <span className="text-xl font-bold">TransitFlow</span>
          </div>
          <h1 className="text-2xl font-bold">Welcome back</h1>
          <p className="mb-6 mt-1 text-sm text-slate-500">Sign in to manage or book your trips.</p>

          {location.state?.registered && (
            <p className="mb-4 rounded-lg bg-green-50 px-4 py-3 text-sm text-green-700">Account created. Please sign in.</p>
          )}
          <form onSubmit={onSubmit} className="space-y-4">
            <ErrorMessage message={error} />
            <Input label="Email" name="email" type="email" autoComplete="email" required
                   value={form.email} onChange={onChange} />
            <Input label="Password" name="password" type="password" autoComplete="current-password" required
                   value={form.password} onChange={onChange} />
            <Button type="submit" loading={submitting} className="w-full">Sign in</Button>
          </form>
          <p className="mt-6 text-center text-sm text-slate-600">
            New passenger?{' '}
            <Link to="/register" className="font-medium text-blue-600 hover:underline">Create an account</Link>
          </p>
        </div>
      </div>
    </div>
  );
}