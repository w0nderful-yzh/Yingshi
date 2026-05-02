import request from './request';
import type { DeviceVO, DeviceUpdateDTO, DeviceSyncResultDTO } from '@/types';

export function getDevices(params?: { status?: string; sourceType?: string; keyword?: string }) {
  return request.get<any, DeviceVO[]>('/api/devices', { params });
}

export function getDeviceById(id: number) {
  return request.get<any, DeviceVO>(`/api/devices/${id}`);
}

export function updateDevice(id: number, data: DeviceUpdateDTO) {
  return request.put<any, DeviceVO>(`/api/devices/${id}`, data);
}

export function disableDevice(id: number) {
  return request.put<any, null>(`/api/devices/${id}/disable`);
}

export function enableDevice(id: number) {
  return request.put<any, null>(`/api/devices/${id}/enable`);
}

export function deleteDevice(id: number) {
  return request.delete<any, null>(`/api/devices/${id}`);
}

export function syncDevices() {
  return request.post<any, DeviceSyncResultDTO>('/api/devices/sync');
}
