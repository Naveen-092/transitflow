import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { errorMessage, fieldErrors } from '../api/axios';
import Button from '../components/Button';
import Card from '../components/Card';
import ErrorMessage from '../components/ErrorMessage';
import Input from '../components/Input';
import { useAuth } from '../hooks/useAuth';
import { roleHome } from '../navConfig';

function validate(form) {
  const errors = {};
  if (!form.name.trim()) errors.name = 'Name is required';
  if (!/^\S+@\S+\.\S+$/.test(form.email)) errors.email = 'Enter a valid email';
  if (!/^[0-9]{10}$/.test(form.phone)) errors.phone = 'Phone must be exactly 10 digits';
  if (form.password.length < 8) errors.password = 'Password must be at least 8 characters';
  if (form.password !== form.confirm) errors.confirm = 'Passwords do not match';
  return errors;
}

export default function Register() {
  const { user, register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: '', email: '', phone: '', password: '', confirm: '' });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (user) return <Navigate to={roleHome(user.role)} replace />;

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    setError('');
    const found = validate(form);
    setErrors(found);
    if (Object.keys(found).length > 0) return;

    setSubmitting(true);
    try {
      await register({
        name: form.name.trim(),
        email: form.email.trim(),
        phone: form.phone,
        password: form.password,
      });
      navigate('/login', { replace: true, state: { registered: true } });
    } catch (err) {
      setErrors(fieldErrors(err));
      setError(errorMessage(err));
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 p-4">
      <div className="w-full max-w-sm">
        <h1 className="mb-6 text-center text-3xl font-bold text-blue-700">🚌 TransitFlow</h1>
        <Card title="Create a passenger account">
          <form onSubmit={onSubmit} className="space-y-4" noValidate>
            <ErrorMessage message={error} />
            <Input label="Full name" name="name" value={form.name} onChange={onChange} error={errors.name} />
            <Input label="Email" name="email" type="email" value={form.email} onChange={onChange} error={errors.email} />
            <Input label="Phone (10 digits)" name="phone" inputMode="numeric" maxLength={10}
                   value={form.phone} onChange={onChange} error={errors.phone} />
            <Input label="Password" name="password" type="password" value={form.password}
                   onChange={onChange} error={errors.password} />
            <Input label="Confirm password" name="confirm" type="password" value={form.confirm}
                   onChange={onChange} error={errors.confirm} />
            <Button type="submit" loading={submitting} className="w-full">Register</Button>
          </form>
          <p className="mt-4 text-center text-sm text-slate-600">
            Already have an account?{' '}
            <Link to="/login" className="font-medium text-blue-600 hover:underline">Sign in</Link>
          </p>
        </Card>
      </div>
    </div>
  );
}