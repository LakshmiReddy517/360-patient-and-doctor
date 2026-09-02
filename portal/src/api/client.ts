import axios from 'axios';

/** Base URL of the Spring Boot backend. Override with VITE_API_URL if needed. */
export const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export const TOKEN_KEY = 'pcare.accessToken';
export const REFRESH_KEY = 'pcare.refreshToken';
export const USER_KEY = 'pcare.user';

export const api = axios.create({ baseURL: API_BASE });

// Attach the bearer token to every request.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// On 401/expired token, clear session and bounce to login.
api.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export function apiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.message || error.message || 'Request failed';
  }
  return 'Unexpected error';
}
