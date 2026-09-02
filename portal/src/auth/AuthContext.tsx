import { createContext, useContext, useState, type ReactNode } from 'react';
import { api, TOKEN_KEY, REFRESH_KEY, USER_KEY } from '../api/client';
import type { AuthResponse, User } from '../types';

interface AuthState {
  user: User | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | undefined>(undefined);

function loadUser(): User | null {
  const raw = localStorage.getItem(USER_KEY);
  return raw ? (JSON.parse(raw) as User) : null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(loadUser());

  async function login(username: string, password: string) {
    const { data } = await api.post<AuthResponse>('/auth/login', { username, password });
    localStorage.setItem(TOKEN_KEY, data.accessToken);
    localStorage.setItem(REFRESH_KEY, data.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(data.user));
    setUser(data.user);
  }

  function logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
    setUser(null);
    window.location.href = '/login';
  }

  return <AuthContext.Provider value={{ user, login, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
