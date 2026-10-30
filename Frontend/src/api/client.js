import axios from 'axios';

// Normalize optional VITE_API_URL (works whether user sets https://api.example.com or https://api.example.com/api)
const rawBackendUrl = (import.meta.env.VITE_API_URL || '').trim().replace(/\/+$/, '');
export const BACKEND_ORIGIN = rawBackendUrl.replace(/\/api$/, '');
export const API_BASE_URL = BACKEND_ORIGIN ? `${BACKEND_ORIGIN}/api` : '/api';
export const WS_BASE_URL = BACKEND_ORIGIN ? `${BACKEND_ORIGIN}/ws` : '/ws';

/**
 * * Vercel serves this SPA as static files, so a relative "/api" request would hit the
 * * Vercel domain itself. The SPA rewrite then answers with index.html (HTTP 200, text/html),
 * * which is why a missing VITE_API_URL makes the whole app look "broken" or "stale" with no
 * * obvious error. Detect that at startup and say it out loud.
 *
 * * Vite bakes VITE_* values into the bundle at BUILD time, so changing the variable in the
 * * Vercel dashboard requires a new deployment before it takes effect.
 */
export const API_CONFIG_ERROR =
  import.meta.env.PROD && !BACKEND_ORIGIN
    ? 'This deployment is missing VITE_API_URL, so the app cannot reach the backend. '
      + 'Set VITE_API_URL=https://your-backend-host (Vercel → Settings → Environment Variables) '
      + 'and then redeploy — Vite bakes that value into the bundle at build time.'
    : null;

// Base axios instance pointing to your Spring Boot backend
const api = axios.create({
  baseURL: API_BASE_URL, // Uses VITE_API_URL in production (Vercel), or '/api' via Vite proxy in local dev
});

// Endpoints are called outside React components, so this must be an interceptor on
// the shared client rather than a hook that components would have to remember to call.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// * When a request is answered with HTML, the caller is almost certainly talking to the
// * static host instead of the API (wrong/missing VITE_API_URL, or a backend that has not
// * been redeployed yet). Turn that confusing payload into a message the UI can show.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const response = error.response;
    const data = response?.data;
    const contentType = String(response?.headers?.['content-type'] || '').toLowerCase();

    if (typeof data === 'string' && (contentType.includes('text/html') || data.trimStart().startsWith('<'))) {
      error.response.data = {
        message: API_CONFIG_ERROR
          || 'The server answered with a web page instead of JSON, which means this '
            + 'deployment is not pointing at the API. Check VITE_API_URL on Vercel and that '
            + 'the backend is deployed and awake, then redeploy.',
      };
    }

    return Promise.reject(error);
  },
);

export default api;
