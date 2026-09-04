import axios from 'axios';
import { useAuth } from '../context/AuthContext';

// Base axios instance pointing to your Spring Boot backend
const api = axios.create({
  baseURL: '/api',  // Vite proxy will forward this to localhost:8080
});

// This is a hook that returns an api instance with the JWT attached
export function useApi() {
  const { token } = useAuth();
  
  api.interceptors.request.use((config) => {
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });

  return api;
}

export default api;