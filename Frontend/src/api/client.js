import axios from 'axios';

// Normalize optional VITE_API_URL (works whether user sets https://api.example.com or https://api.example.com/api)
const rawBackendUrl = (import.meta.env.VITE_API_URL || '').trim().replace(/\/+$/, '');
export const BACKEND_ORIGIN = rawBackendUrl.replace(/\/api$/, '');
export const API_BASE_URL = BACKEND_ORIGIN ? `${BACKEND_ORIGIN}/api` : '/api';
export const WS_BASE_URL = BACKEND_ORIGIN ? `${BACKEND_ORIGIN}/ws` : '/ws';

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

export default api;
