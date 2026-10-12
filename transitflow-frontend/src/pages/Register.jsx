import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { errorMessage, fieldErrors } from '../api/axios';
import Button from '../components/Button';
import ErrorMessage from '../components/ErrorMessage';
import Input from '../components/Input';
import { useAuth } from '../hooks/useAuth';
import { roleHome } from '../navConfig';
import { Bus } from 'lucide-react';

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
    <div className="grid min-h-screen lg:grid-cols-2">
      <div className="relative hidden flex-col justify-between overflow-hidden bg-gradient-to-br from-blue-700 via-blue-600 to-indigo-700 p-10 text-white lg:flex">
        <div className="flex items-center gap-2 text-xl font-bold">
          <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-white/15"><Bus size={22} /></span>
          TransitFlow
        </div>
        <div className="space-y-3">
          <h2 className="text-4xl font-bold leading-tight">Book your seat<br />in seconds.</h2>
          <p className="text-blue-100">Pick your exact seat, get a QR ticket, and watch your bus arrive live.</p>
        </div>
        <p className="text-sm text-blue-200">Campus and corporate shuttle management</p>
        <div className="pointer-events-none absolute -bottom-24 -right-24 h-80 w-80 rounded-full bg-white/10" />
      </div>

      <div className="flex items-center justify-center p-6">
        <div className="w-full max-w-sm">
          <div className="mb-6 flex items-center gap-2 lg:hidden">
            <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-600 text-white"><Bus size={20} /></span>
            <span className="text-xl font-bold">TransitFlow</span>
          </div>
          <h1 className="text-2xl font-bold">Create your account</h1>
          <p className="mb-6 mt-1 text-sm text-slate-500">Passenger registration takes under a minute.</p>
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
            <Button type="submit" loading={submitting} className="w-full">Create account</Button>
          </form>
          <p className="mt-6 text-center text-sm text-slate-600">
            Already have an account?{' '}
            <Link to="/login" className="font-medium text-blue-600 hover:underline">Sign in</Link>
          </p>
        </div>
      </div>
    </div>
  );
}