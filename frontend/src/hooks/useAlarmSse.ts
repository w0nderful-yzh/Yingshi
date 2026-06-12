import { useRef, useCallback } from 'react';
import { notification } from 'antd';
import { getToken } from '@/utils/token';
import { useAlarmStore } from '@/store/alarmStore';

interface AlarmEvent {
  id?: number;
  alarmName?: string;
  alarmContent?: string;
  alarmType?: string;
  alarmTime?: string;
  deviceSerial?: string;
  source?: string;
}

/**
 * SSE 告警推送 Hook
 * 连接 /api/alarms/stream，接收实时告警推送
 */
export function useAlarmSse() {
  const eventSourceRef = useRef<EventSource | null>(null);
  const reconnectTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const { fetchUnreadCount } = useAlarmStore();

  const connect = useCallback(() => {
    const token = getToken();
    if (!token) return;

    // 关闭旧连接
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
    }

    const url = `/api/alarms/stream?token=${encodeURIComponent(token)}`;
    const es = new EventSource(url);
    eventSourceRef.current = es;

    es.addEventListener('connected', () => {
      console.log('[SSE] 告警推送已连接');
    });

    es.addEventListener('alarm', (event) => {
      try {
        const alarm: AlarmEvent = JSON.parse(event.data);
        console.log('[SSE] 收到新告警:', alarm);

        // 更新未读数
        fetchUnreadCount();

        // 弹窗提醒
        const alarmName = alarm.alarmName || '新告警';
        const alarmContent = alarm.alarmContent || '';

        notification.warning({
          message: alarmName,
          description: alarmContent,
          duration: 6,
          placement: 'topRight',
          onClick: () => {
            window.location.hash = '#/alarms';
          },
        });
      } catch (err) {
        console.warn('[SSE] 解析告警数据失败:', err);
      }
    });

    es.onerror = () => {
      console.warn('[SSE] 连接断开，5秒后重连');
      es.close();
      eventSourceRef.current = null;
      // 重连
      reconnectTimerRef.current = setTimeout(connect, 5000);
    };
  }, [fetchUnreadCount]);

  const disconnect = useCallback(() => {
    if (reconnectTimerRef.current) {
      clearTimeout(reconnectTimerRef.current);
      reconnectTimerRef.current = null;
    }
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
    }
  }, []);

  return { connect, disconnect };
}
