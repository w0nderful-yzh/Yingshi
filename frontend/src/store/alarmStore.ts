import { create } from 'zustand';
import { getUnreadCount } from '@/api/alarm';

interface AlarmState {
  unreadCount: number;
  pollingTimer: ReturnType<typeof setInterval> | null;
  fetchUnreadCount: () => Promise<void>;
  startPolling: () => void;
  stopPolling: () => void;
  decrementCount: () => void;
  resetCount: () => void;
}

export const useAlarmStore = create<AlarmState>((set, get) => ({
  unreadCount: 0,
  pollingTimer: null,

  fetchUnreadCount: async () => {
    try {
      const res = await getUnreadCount();
      set({ unreadCount: res.count });
    } catch {
      // ignore
    }
  },

  startPolling: () => {
    const { pollingTimer, fetchUnreadCount } = get();
    if (pollingTimer) return;
    fetchUnreadCount();
    const timer = setInterval(fetchUnreadCount, 30000);
    set({ pollingTimer: timer });
  },

  stopPolling: () => {
    const { pollingTimer } = get();
    if (pollingTimer) {
      clearInterval(pollingTimer);
      set({ pollingTimer: null });
    }
  },

  decrementCount: () => {
    set((state) => ({ unreadCount: Math.max(0, state.unreadCount - 1) }));
  },

  resetCount: () => {
    set({ unreadCount: 0 });
  },
}));
