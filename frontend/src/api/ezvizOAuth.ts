import request from './request';
import type { UserDeviceVO, EzvizAuthUrlVO } from '@/types';

export function getEzvizAuthUrl() {
  return request.get<any, EzvizAuthUrlVO>('/api/ezviz/oauth/auth-url');
}

export function handleEzvizCallback(data: {
  authCode: string;
  state: string;
  deviceSerials?: string;
  deviceTrustId?: string;
}) {
  return request.post<any, UserDeviceVO[]>('/api/ezviz/oauth/callback', data);
}

export function getUserDevices() {
  return request.get<any, UserDeviceVO[]>('/api/ezviz/oauth/devices');
}

export function unbindUserDevice(id: number) {
  return request.delete<any, null>(`/api/ezviz/oauth/devices/${id}`);
}

export function getEzvizOAuthStatus() {
  return request.get<any, { authorized: boolean }>('/api/ezviz/oauth/status');
}

export function revokeEzvizOAuth() {
  return request.delete<any, null>('/api/ezviz/oauth/revoke');
}
