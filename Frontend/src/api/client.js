import axios from 'axios';

// Base axios instance pointing to your Spring Boot backend
const api = axios.create({
  baseURL: '/api',  // Vite proxy will forward this to localhost:8080
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
