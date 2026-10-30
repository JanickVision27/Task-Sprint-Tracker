import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { authApi, healthApi } from '../api/endpoints';
import RoleGuideButton from '../components/RoleGuideModal';
import AuthLayout, { MailIcon, CheckCircleIcon } from '../components/AuthLayout';

export default function RegisterPage() {
  const [form, setForm] = useState({ name: '', email: '', password: '', role: 'MANAGER' });
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  // Holds the created account's email once the backend accepted the registration,
  // which switches the page to the "check your inbox" confirmation state.
  const [registered, setRegistered] = useState(null);

  // Wake up Render container on page load
  useEffect(() => {
    healthApi.check().catch(() => {});
  }, []);

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value });
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setIsLoading(true);
    try {
      const res = await authApi.register(form);
      // The backend creates the account and fires the Brevo welcome email in the background,
      // so we can confirm immediately.
      setRegistered({
        name: res.data?.user?.name || form.name,
        email: res.data?.user?.email || form.email,
      });
    } catch (err) {
      if (!err.response) {
        setError('Server is waking up on Render Free Tier. Please wait ~20 seconds and try again.');
      } else {
        setError(err.response?.data?.message || 'Registration failed');
      }
    } finally {
      setIsLoading(false);
    }
  }

  // ------------------------------------------------------- confirmation state
  if (registered) {
    return (
      <AuthLayout title="Welcome to the team!" subtitle="Your account has been created">
        <div className="bg-white p-8 rounded-xl shadow-sm border border-slate-200">
          <div className="flex justify-center mb-4">
            <div className="w-14 h-14 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center border border-emerald-100">
              <CheckCircleIcon className="w-7 h-7" />
            </div>
          </div>

          <h2 className="text-lg font-semibold text-slate-800 text-center mb-2">
            You&apos;re all set, {registered.name}
          </h2>
          <p className="text-sm text-slate-600 text-center">
            We just sent a welcome email to{' '}
            <span className="font-medium text-slate-900 break-all">{registered.email}</span> with a quick
            tour of projects, sprints and your live Kanban board.
          </p>

          <div className="mt-5 flex items-start gap-3 p-3.5 rounded-lg bg-indigo-50 border border-indigo-100">
            <MailIcon className="w-5 h-5 text-indigo-600 shrink-0 mt-0.5" />
            <p className="text-xs text-indigo-900 leading-relaxed">
              Can&apos;t find it? Give it a minute, then check your spam or promotions folder.
              Your account already works — you can sign in right now.
            </p>
          </div>

          <Link
            to="/login"
            className="mt-6 block text-center w-full bg-indigo-600 text-white font-medium py-2.5 rounded-lg hover:bg-indigo-700 transition"
          >
            Continue to sign in
          </Link>

          <p className="mt-4 text-center text-sm text-slate-600">
            Wrong email address?{' '}
            <button
              type="button"
              onClick={() => {
                setRegistered(null);
                setForm({ ...form, email: '' });
              }}
              className="text-indigo-600 font-medium hover:underline cursor-pointer"
            >
              Register again
            </button>
          </p>
        </div>
      </AuthLayout>
    );
  }

  // ------------------------------------------------------- form state
  return (
    <AuthLayout title="Create Your Account" subtitle="Join your team on Task & Sprint Tracker">
      <form
        onSubmit={handleSubmit}
        className="bg-white p-8 rounded-xl shadow-sm border border-slate-200"
      >
        {error && (
          <div className="mb-4 p-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-sm">
            {error}
          </div>
        )}

        <div className="mb-4">
          <label className="block text-sm font-medium text-slate-700 mb-1.5">Full Name</label>
          <input
            name="name"
            placeholder="e.g. Vikranth"
            value={form.name}
            onChange={handleChange}
            className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition"
            required
          />
        </div>

        <div className="mb-4">
          <label className="block text-sm font-medium text-slate-700 mb-1.5">Email address</label>
          <input
            name="email"
            type="email"
            placeholder="you@example.com"
            value={form.email}
            onChange={handleChange}
            className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition"
            required
          />
          <p className="mt-1 text-xs text-slate-500">
            We&apos;ll send a welcome email to this address.
          </p>
        </div>

        <div className="mb-4">
          <label className="block text-sm font-medium text-slate-700 mb-1.5">Password</label>
          <input
            name="password"
            type="password"
            placeholder="At least 8 characters"
            value={form.password}
            onChange={handleChange}
            className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition"
            required
            minLength={8}
            autoComplete="new-password"
          />
        </div>

        <div className="mb-6">
          <div className="flex items-center justify-between mb-1.5">
            <label className="block text-sm font-medium text-slate-700">Team Role</label>
            <RoleGuideButton compact />
          </div>
          <select
            name="role"
            value={form.role}
            onChange={handleChange}
            className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition"
          >
            <option value="MANAGER">Manager (Create &amp; manage sprints &amp; tasks)</option>
            <option value="ADMIN">Admin (Full workspace access)</option>
            <option value="MEMBER">Member (Update &amp; move assigned tasks)</option>
          </select>
        </div>

        <button
          type="submit"
          disabled={isLoading}
          className="w-full bg-indigo-600 text-white font-medium py-2.5 rounded-lg hover:bg-indigo-700 transition disabled:opacity-60 cursor-pointer"
        >
          {isLoading ? 'Creating account...' : 'Create Account'}
        </button>

        <p className="mt-6 text-center text-sm text-slate-600">
          Already have an account?{' '}
          <Link to="/login" className="text-indigo-600 font-medium hover:underline">
            Sign in
          </Link>
        </p>
      </form>
    </AuthLayout>
  );
}
