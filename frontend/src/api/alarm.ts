import request from './request';
import type { AlarmMessageVO, AlarmUnreadCountVO, AlarmSyncResultDTO, AlarmQueryDTO } from '@/types';

export function getAlarms(params?: AlarmQueryDTO) {
  return request.get<any, AlarmMessageVO[]>('/api/alarms', { params });
}

export function getUnreadCount() {
  return request.get<any, AlarmUnreadCountVO>('/api/alarms/unread-count');
}

export function markAlarmRead(id: number) {
  return request.put<any, null>(`/api/alarms/${id}/read`);
}

export function markAllRead(deviceId?: number) {
  return request.put<any, null>('/api/alarms/read-all', null, { params: deviceId ? { deviceId } : undefined });
}

export function deleteAlarm(id: number) {
  return request.delete<any, null>(`/api/alarms/${id}`);
}

export function syncAlarms() {
  return request.post<any, AlarmSyncResultDTO>('/api/alarms/sync');
}
