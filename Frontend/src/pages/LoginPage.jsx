import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { authApi, healthApi } from '../api/endpoints';
import RoleGuideButton from '../components/RoleGuideModal';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [serverAwake, setServerAwake] = useState(null); // null = checking, true = awake, false = waking
  const { login } = useAuth();
  const navigate = useNavigate();

  // Wake up Render free-tier server immediately when user opens the Login page
  useEffect(() => {
    let active = true;
    const timer = setTimeout(() => {
      if (active && serverAwake === null) setServerAwake(false);
    }, 2000);

    healthApi
      .check()
      .then(() => {
        if (active) setServerAwake(true);
      })
      .catch(() => {
        if (active) setServerAwake(false);
      });

    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [serverAwake]);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setIsLoading(true);
    try {
      const res = await authApi.login(email, password);
      login(res.data.token, res.data.user || { email });
      navigate('/dashboard');
    } catch (err) {
      if (!err.response) {
        setError('Server is still waking up on Render Free Tier. Please wait ~20 seconds and click Sign In again.');
      } else {
        setError(err.response?.data?.message || 'Invalid email or password');
      }
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-slate-50 px-4">
      <div className="w-full max-w-md">
        {/* Brand Header */}
        <div className="text-center mb-6">
          <div className="inline-flex items-center justify-center w-12 h-12 rounded-xl bg-indigo-600 text-white font-bold text-xl shadow-sm mb-3">
            ST
          </div>
          <h1 className="text-2xl font-bold text-slate-900">Task & Sprint Tracker</h1>
          <p className="text-sm text-slate-500 mt-1">Sign in to manage your projects and Kanban boards</p>
        </div>

        {/* Server Status / Role Guide Bar */}
        <div className="flex items-center justify-between mb-3 px-1">
          <div>
            {serverAwake === true && (
              <span className="inline-flex items-center gap-1.5 text-xs font-medium text-emerald-700 bg-emerald-50 border border-emerald-200 px-2.5 py-1 rounded-full">
                <span className="w-2 h-2 rounded-full bg-emerald-500" />
                Backend Online
              </span>
            )}
            {serverAwake === false && (
              <span className="inline-flex items-center gap-1.5 text-xs font-medium text-amber-800 bg-amber-50 border border-amber-200 px-2.5 py-1 rounded-full">
                <span className="w-2 h-2 rounded-full bg-amber-500 animate-ping" />
                Waking up free Render server (~30s)...
              </span>
            )}
          </div>
          <RoleGuideButton />
        </div>

        {/* Card */}
        <form
          onSubmit={handleSubmit}
          className="bg-white p-8 rounded-xl shadow-sm border border-slate-200"
        >
          <h2 className="text-lg font-semibold text-slate-800 mb-5">Welcome back</h2>

          {error && (
            <div className="mb-4 p-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-sm">
              {error}
            </div>
          )}

          <div className="mb-4">
            <label className="block text-sm font-medium text-slate-700 mb-1.5">Email address</label>
            <input
              type="email"
              placeholder="you@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition"
              required
            />
          </div>

          <div className="mb-6">
            <label className="block text-sm font-medium text-slate-700 mb-1.5">Password</label>
            <input
              type="password"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition"
              required
            />
          </div>

          <button
            type="submit"
            disabled={isLoading}
            className="w-full bg-indigo-600 text-white font-medium py-2.5 rounded-lg hover:bg-indigo-700 transition disabled:opacity-60 cursor-pointer"
          >
            {isLoading ? 'Signing in...' : 'Sign In'}
          </button>

          <p className="mt-6 text-center text-sm text-slate-600">
            Don&apos;t have an account?{' '}
            <Link to="/register" className="text-indigo-600 font-medium hover:underline">
              Create an account
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
}
