import { useState, useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { authApi } from '../api/endpoints';
import AuthLayout, { LockIcon, CheckCircleIcon, AlertIcon } from '../components/AuthLayout';

const MIN_PASSWORD_LENGTH = 8;

/**
 * * Step 3 of "I forgot my password" — the page the Brevo email links to.
 *
 * * Flow:
 * *  1. read ?token=... from the URL
 * *  2. ask the backend whether that token is still valid (expired / already used links
 * *     get a clear "request a new one" screen instead of a confusing failure on submit)
 * *  3. post the new password, which BCrypt-hashes and stores it, then burn the token
 */
export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';
  const navigate = useNavigate();

  // * Initial state is derived from the URL, so a missing token never needs a setState
  // * inside an effect (which would cause an extra render pass).
  const [status, setStatus] = useState(token ? 'checking' : 'invalid'); // checking | valid | invalid | done
  const [message, setMessage] = useState(
    token ? '' : 'This reset link is incomplete. Please request a new one.'
  );
  const [maskedEmail, setMaskedEmail] = useState('');

  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  // Verify the link once, before showing the form.
  useEffect(() => {
    let active = true;

    if (!token) {
      return () => { active = false; };
    }

    authApi
      .validateResetToken(token)
      .then((res) => {
        if (!active) return;
        if (res.data?.valid) {
          setStatus('valid');
          setMaskedEmail(res.data.email || '');
        } else {
          setStatus('invalid');
          setMessage(res.data?.message || 'This reset link is no longer valid.');
        }
      })
      .catch((err) => {
        if (!active) return;
        setStatus('invalid');
        setMessage(
          err.response?.data?.message ||
            'We could not verify this link. Please request a new password reset email.'
        );
      });

    return () => { active = false; };
  }, [token]);

  const longEnough = password.length >= MIN_PASSWORD_LENGTH;
  const passwordsMatch = password.length > 0 && password === confirmPassword;
  const canSubmit = longEnough && passwordsMatch && !isLoading;

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');

    if (!longEnough) {
      setError(`Password must be at least ${MIN_PASSWORD_LENGTH} characters long.`);
      return;
    }
    if (!passwordsMatch) {
      setError('Both passwords must match.');
      return;
    }

    setIsLoading(true);
    try {
      await authApi.resetPassword(token, password);
      setStatus('done');
      // Give the user a moment to read the confirmation, then send them to sign in.
      setTimeout(() => navigate('/login', { replace: true }), 4000);
    } catch (err) {
      if (!err.response) {
        setError('Could not reach the server. It may still be waking up (Render free tier) — please try again in ~20 seconds.');
      } else {
        setError(err.response?.data?.message || 'We could not update your password. Please try again.');
      }
    } finally {
      setIsLoading(false);
    }
  }

  // ------------------------------------------------------------ checking
  if (status === 'checking') {
    return (
      <AuthLayout title="Verifying your link" subtitle="Just a moment">
        <div className="bg-white p-8 rounded-xl shadow-sm border border-slate-200 text-center">
          <div className="inline-flex items-center justify-center w-12 h-12 rounded-full border-2 border-indigo-200 border-t-indigo-600 animate-spin mb-4" />
          <p className="text-sm text-slate-600">Checking that this reset link is still valid...</p>
        </div>
      </AuthLayout>
    );
  }

  // ------------------------------------------------------------ invalid
  if (status === 'invalid') {
    return (
      <AuthLayout title="Link expired or invalid" subtitle="Reset links can only be used once">
        <div className="bg-white p-8 rounded-xl shadow-sm border border-slate-200">
          <div className="flex justify-center mb-4">
            <div className="w-14 h-14 rounded-full bg-amber-50 text-amber-600 flex items-center justify-center border border-amber-100">
              <AlertIcon className="w-7 h-7" />
            </div>
          </div>

          <p className="text-sm text-slate-600 text-center">{message}</p>

          <Link
            to="/forgot-password"
            className="mt-6 block text-center w-full bg-indigo-600 text-white font-medium py-2.5 rounded-lg hover:bg-indigo-700 transition"
          >
            Request a new reset link
          </Link>

          <p className="mt-4 text-center text-sm text-slate-600">
            <Link to="/login" className="text-indigo-600 font-medium hover:underline">
              Back to sign in
            </Link>
          </p>
        </div>
      </AuthLayout>
    );
  }

  // ------------------------------------------------------------ success
  if (status === 'done') {
    return (
      <AuthLayout title="Password updated" subtitle="You're all set">
        <div className="bg-white p-8 rounded-xl shadow-sm border border-slate-200">
          <div className="flex justify-center mb-4">
            <div className="w-14 h-14 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center border border-emerald-100">
              <CheckCircleIcon className="w-7 h-7" />
            </div>
          </div>

          <h2 className="text-lg font-semibold text-slate-800 text-center mb-2">
            Your password has been changed
          </h2>
          <p className="text-sm text-slate-600 text-center">
            For your security, this reset link can no longer be used. Sign in with your new password —
            we&apos;ll take you to the sign-in page in a few seconds.
          </p>

          <Link
            to="/login"
            className="mt-6 block text-center w-full bg-indigo-600 text-white font-medium py-2.5 rounded-lg hover:bg-indigo-700 transition"
          >
            Go to sign in
          </Link>
        </div>
      </AuthLayout>
    );
  }

  // ------------------------------------------------------------ form
  return (
    <AuthLayout title="Choose a new password" subtitle="Make it something only you know">
      <form
        onSubmit={handleSubmit}
        className="bg-white p-8 rounded-xl shadow-sm border border-slate-200"
      >
        <div className="flex items-center gap-3 mb-5">
          <div className="w-10 h-10 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center shrink-0">
            <LockIcon className="w-5 h-5" />
          </div>
          <p className="text-sm text-slate-600">
            {maskedEmail ? (
              <>Resetting the password for <span className="font-medium text-slate-900">{maskedEmail}</span>.</>
            ) : (
              <>Enter a new password for your account.</>
            )}
          </p>
        </div>

        {error && (
          <div className="mb-4 p-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-sm">
            {error}
          </div>
        )}

        <div className="mb-4">
          <label className="block text-sm font-medium text-slate-700 mb-1.5">New password</label>
          <div className="relative">
            <input
              type={showPassword ? 'text' : 'password'}
              autoFocus
              placeholder={`At least ${MIN_PASSWORD_LENGTH} characters`}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full px-3.5 py-2.5 pr-16 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition"
              required
              minLength={MIN_PASSWORD_LENGTH}
              autoComplete="new-password"
            />
            <button
              type="button"
              onClick={() => setShowPassword((visible) => !visible)}
              className="absolute inset-y-0 right-2 my-1 px-2 text-xs font-medium text-slate-500 hover:text-slate-700 rounded cursor-pointer"
            >
              {showPassword ? 'Hide' : 'Show'}
            </button>
          </div>
        </div>

        <div className="mb-4">
          <label className="block text-sm font-medium text-slate-700 mb-1.5">Confirm new password</label>
          <input
            type={showPassword ? 'text' : 'password'}
            placeholder="Type it again"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition"
            required
            autoComplete="new-password"
          />
        </div>

        {/* Live checklist so the user knows exactly what is missing before submitting */}
        <ul className="mb-6 space-y-1.5">
          <Requirement met={longEnough}>
            At least {MIN_PASSWORD_LENGTH} characters
          </Requirement>
          <Requirement met={passwordsMatch}>Both passwords match</Requirement>
        </ul>

        <button
          type="submit"
          disabled={!canSubmit}
          className="w-full bg-indigo-600 text-white font-medium py-2.5 rounded-lg hover:bg-indigo-700 transition disabled:opacity-60 disabled:cursor-not-allowed cursor-pointer"
        >
          {isLoading ? 'Updating password...' : 'Update password'}
        </button>

        <p className="mt-6 text-center text-sm text-slate-600">
          <Link to="/login" className="text-indigo-600 font-medium hover:underline">
            Back to sign in
          </Link>
        </p>
      </form>
    </AuthLayout>
  );
}

/** One line of the live password checklist. */
function Requirement({ met, children }) {
  return (
    <li className={`flex items-center gap-2 text-xs ${met ? 'text-emerald-700' : 'text-slate-500'}`}>
      <span
        className={`inline-flex items-center justify-center w-4 h-4 rounded-full border text-[10px] leading-none ${
          met ? 'bg-emerald-50 border-emerald-300 text-emerald-700' : 'bg-slate-50 border-slate-300 text-slate-400'
        }`}
      >
        {met ? '✓' : '•'}
      </span>
      {children}
    </li>
  );
}
