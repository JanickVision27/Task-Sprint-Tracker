import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { authApi } from '../api/endpoints';
import AuthLayout, { MailIcon, CheckCircleIcon } from '../components/AuthLayout';

const RESEND_COOLDOWN_SECONDS = 45;

/**
 * * Step 1 of "I forgot my password".
 *
 * * The user types their email, the backend emails a single-use link through Brevo, and the
 * * UI shows a deliberately generic confirmation. The backend never says whether an account
 * * exists, so neither does this screen.
 */
export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [sentTo, setSentTo] = useState('');
  const [cooldown, setCooldown] = useState(0);

  // Simple resend countdown so a user cannot hammer the endpoint / Brevo quota.
  useEffect(() => {
    if (cooldown <= 0) return undefined;
    const timer = setTimeout(() => setCooldown((seconds) => seconds - 1), 1000);
    return () => clearTimeout(timer);
  }, [cooldown]);

  async function sendResetLink(address) {
    setError('');
    setIsLoading(true);
    try {
      await authApi.forgotPassword(address.trim());
      setSentTo(address.trim());
      setCooldown(RESEND_COOLDOWN_SECONDS);
    } catch (err) {
      if (!err.response) {
        setError('Could not reach the server. It may still be waking up (Render free tier) — please try again in ~20 seconds.');
      } else {
        setError(err.response?.data?.message || 'We could not send the reset email. Please try again.');
      }
    } finally {
      setIsLoading(false);
    }
  }

  async function handleSubmit(e) {
    e.preventDefault();
    await sendResetLink(email);
  }

  // -------------------------------------------------------------- sent state
  if (sentTo) {
    return (
      <AuthLayout
        title="Check your inbox"
        subtitle="Your reset link is on its way"
      >
        <div className="bg-white p-8 rounded-xl shadow-sm border border-slate-200">
          <div className="flex justify-center mb-4">
            <div className="w-14 h-14 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center border border-emerald-100">
              <CheckCircleIcon className="w-7 h-7" />
            </div>
          </div>

          <h2 className="text-lg font-semibold text-slate-800 text-center mb-2">
            We sent you a reset link
          </h2>
          <p className="text-sm text-slate-600 text-center">
            If an account exists for{' '}
            <span className="font-medium text-slate-900 break-all">{sentTo}</span>, you will receive an
            email with a link to choose a new password.
          </p>

          {error && (
            <div className="mt-4 p-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-sm">
              {error}
            </div>
          )}

          <div className="mt-5 p-3.5 rounded-lg bg-indigo-50 border border-indigo-100 text-indigo-900 text-xs leading-relaxed">
            <p className="font-semibold mb-1">Didn&apos;t get it?</p>
            <ul className="list-disc pl-4 space-y-0.5">
              <li>Check your spam / promotions folder.</li>
              <li>The link expires in 30 minutes and works only once.</li>
              <li>Make sure you typed the address you registered with.</li>
            </ul>
          </div>

          <button
            type="button"
            onClick={() => sendResetLink(sentTo)}
            disabled={isLoading || cooldown > 0}
            className="mt-5 w-full border border-slate-300 text-slate-700 font-medium py-2.5 rounded-lg hover:bg-slate-50 transition disabled:opacity-60 disabled:cursor-not-allowed cursor-pointer"
          >
            {isLoading
              ? 'Sending again...'
              : cooldown > 0
                ? `Resend link in ${cooldown}s`
                : 'Send the link again'}
          </button>

          <button
            type="button"
            onClick={() => {
              setSentTo('');
              setError('');
              setCooldown(0);
            }}
            className="mt-2 w-full text-sm text-slate-500 hover:text-slate-700 transition cursor-pointer py-2"
          >
            Use a different email address
          </button>

          <p className="mt-4 text-center text-sm text-slate-600">
            <Link to="/login" className="text-indigo-600 font-medium hover:underline">
              Back to sign in
            </Link>
          </p>
        </div>
      </AuthLayout>
    );
  }

  // -------------------------------------------------------------- form state
  return (
    <AuthLayout
      title="Forgot your password?"
      subtitle="We'll email you a secure link to set a new one"
    >
      <form
        onSubmit={handleSubmit}
        className="bg-white p-8 rounded-xl shadow-sm border border-slate-200"
      >
        <div className="flex items-center gap-3 mb-5">
          <div className="w-10 h-10 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center shrink-0">
            <MailIcon className="w-5 h-5" />
          </div>
          <p className="text-sm text-slate-600">
            Enter the email address linked to your account.
          </p>
        </div>

        {error && (
          <div className="mb-4 p-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-sm">
            {error}
          </div>
        )}

        <div className="mb-6">
          <label className="block text-sm font-medium text-slate-700 mb-1.5">Email address</label>
          <input
            type="email"
            autoFocus
            placeholder="you@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition"
            required
          />
        </div>

        <button
          type="submit"
          disabled={isLoading}
          className="w-full bg-indigo-600 text-white font-medium py-2.5 rounded-lg hover:bg-indigo-700 transition disabled:opacity-60 cursor-pointer"
        >
          {isLoading ? 'Sending reset link...' : 'Send reset link'}
        </button>

        <p className="mt-6 text-center text-sm text-slate-600">
          Remembered it?{' '}
          <Link to="/login" className="text-indigo-600 font-medium hover:underline">
            Sign in
          </Link>
        </p>
      </form>
    </AuthLayout>
  );
}
