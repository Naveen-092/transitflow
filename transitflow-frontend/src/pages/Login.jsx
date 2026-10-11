import { useState } from 'react';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { errorMessage } from '../api/axios';
import Button from '../components/Button';
import Card from '../components/Card';
import ErrorMessage from '../components/ErrorMessage';
import Input from '../components/Input';
import { useAuth } from '../hooks/useAuth';
import { roleHome } from '../navConfig';

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
    <div className="flex min-h-screen items-center justify-center bg-slate-50 p-4">
      <div className="w-full max-w-sm">
        <h1 className="mb-6 text-center text-3xl font-bold text-blue-700">🚌 TransitFlow</h1>
        <Card title="Sign in">
          {location.state?.registered && (
            <p className="mb-4 rounded-lg bg-green-50 px-4 py-3 text-sm text-green-700">
              Account created. Please sign in.
            </p>
          )}
          <form onSubmit={onSubmit} className="space-y-4">
            <ErrorMessage message={error} />
            <Input label="Email" name="email" type="email" autoComplete="email" required
                   value={form.email} onChange={onChange} />
            <Input label="Password" name="password" type="password" autoComplete="current-password" required
                   value={form.password} onChange={onChange} />
            <Button type="submit" loading={submitting} className="w-full">Sign in</Button>
          </form>
          <p className="mt-4 text-center text-sm text-slate-600">
            New passenger?{' '}
            <Link to="/register" className="font-medium text-blue-600 hover:underline">Create an account</Link>
          </p>
        </Card>
      </div>
    </div>
  );
}