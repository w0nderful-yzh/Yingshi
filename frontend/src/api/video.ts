import request from './request';
import type { LiveUrlVO, CloudRecordFileVO, CloudPlaybackUrlVO } from '@/types';

export function getLiveUrl(params: { deviceId: number; protocol?: number; quality?: number; expireTime?: number }) {
  return request.get<any, LiveUrlVO>('/api/video/live-url', { params });
}

export function getCloudRecords(params: { deviceId: number; startTime: string; endTime: string }) {
  return request.get<any, CloudRecordFileVO[]>('/api/video/cloud/records', { params });
}

export function getCloudPlaybackUrl(params: {
  deviceId: number;
  startTime: string;
  endTime: string;
  protocol?: number;
  quality?: number;
  expireTime?: number;
}) {
  return request.get<any, CloudPlaybackUrlVO>('/api/video/cloud/playback-url', { params });
}
