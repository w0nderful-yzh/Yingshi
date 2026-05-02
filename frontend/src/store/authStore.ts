import { create } from 'zustand';
import { login as loginApi, register as registerApi, getUserInfo, logout as logoutApi } from '@/api/auth';
import { getToken, setToken, removeToken } from '@/utils/token';
import type { UserInfoVO } from '@/types';

interface AuthState {
  token: string | null;
  user: UserInfoVO | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, password: string, nickname?: string) => Promise<void>;
  logout: () => Promise<void>;
  fetchUserInfo: () => Promise<void>;
  initialize: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  token: null,
  user: null,
  isAuthenticated: false,
  loading: true,

  login: async (username, password) => {
    const res = await loginApi({ username, password });
    setToken(res.token);
    set({ token: res.token, isAuthenticated: true });
    await get().fetchUserInfo();
  },

  register: async (username, password, nickname) => {
    const res = await registerApi({ username, password, nickname });
    setToken(res.token);
    set({ token: res.token, isAuthenticated: true });
    await get().fetchUserInfo();
  },

  logout: async () => {
    try {
      await logoutApi();
    } catch {
      // ignore
    }
    removeToken();
    set({ token: null, user: null, isAuthenticated: false });
  },

  fetchUserInfo: async () => {
    try {
      const user = await getUserInfo();
      set({ user });
    } catch {
      removeToken();
      set({ token: null, user: null, isAuthenticated: false });
    }
  },

  initialize: async () => {
    const token = getToken();
    if (!token) {
      set({ loading: false, isAuthenticated: false });
      return;
    }
    set({ token, isAuthenticated: true });
    try {
      const user = await getUserInfo();
      set({ user, loading: false });
    } catch {
      removeToken();
      set({ token: null, user: null, isAuthenticated: false, loading: false });
    }
  },
}));
