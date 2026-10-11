import axios from 'axios';
import { clearSession, getToken } from './session';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  timeout: 15000,
});

// Attach "Authorization: Bearer <jwt>" to every request.
api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// An expired or invalid token anywhere sends the user back to the login page.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const url = error.config?.url || '';
    if (error.response?.status === 401 && !url.includes('/auth/login')) {
      clearSession();
      if (window.location.pathname !== '/login') {
        window.location.assign('/login');
      }
    }
    return Promise.reject(error);
  }
);

/** A readable message for any failed request. */
export function errorMessage(error) {
  if (error.response?.data?.message) return error.response.data.message;
  if (error.request) return 'Cannot reach the server. Check that the backend is running.';
  return error.message || 'Something went wrong';
}

/** Field-level validation errors from a 400 response, e.g. { email: "Email must be valid" }. */
export function fieldErrors(error) {
  const data = error.response?.data?.data;
  return error.response?.status === 400 && data && typeof data === 'object' ? data : {};
}


export default api;