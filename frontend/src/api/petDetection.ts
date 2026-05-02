import request from './request';
import type {
  PetDetectionConfigVO,
  PetDetectionConfigRequest,
  PetSafeZoneVO,
  PetSafeZoneRequest,
  PetDetectionRecordVO,
  PetDetectionResultVO,
  PetDetectionRecordQueryDTO,
  AlarmMessageVO,
} from '@/types';

// Detection Configs
export function getDetectionConfigs() {
  return request.get<any, PetDetectionConfigVO[]>('/api/pet-detection/configs');
}

export function getDetectionConfigById(id: number) {
  return request.get<any, PetDetectionConfigVO>(`/api/pet-detection/configs/${id}`);
}

export function createDetectionConfig(data: PetDetectionConfigRequest) {
  return request.post<any, PetDetectionConfigVO>('/api/pet-detection/configs', data);
}

export function updateDetectionConfig(id: number, data: PetDetectionConfigRequest) {
  return request.put<any, PetDetectionConfigVO>(`/api/pet-detection/configs/${id}`, data);
}

export function deleteDetectionConfig(id: number) {
  return request.delete<any, null>(`/api/pet-detection/configs/${id}`);
}

// Safe Zones
export function getSafeZones(detectionConfigId: number) {
  return request.get<any, PetSafeZoneVO[]>('/api/pet-detection/zones', { params: { detectionConfigId } });
}

export function createSafeZone(data: PetSafeZoneRequest) {
  return request.post<any, PetSafeZoneVO>('/api/pet-detection/zones', data);
}

export function updateSafeZone(id: number, data: PetSafeZoneRequest) {
  return request.put<any, PetSafeZoneVO>(`/api/pet-detection/zones/${id}`, data);
}

export function deleteSafeZone(id: number) {
  return request.delete<any, null>(`/api/pet-detection/zones/${id}`);
}

// Detection Records
export function getDetectionRecords(params?: PetDetectionRecordQueryDTO) {
  return request.get<any, PetDetectionRecordVO[]>('/api/pet-detection/records', { params });
}

// Manual Triggers
export function triggerDetection(configId: number) {
  return request.post<any, PetDetectionResultVO>(`/api/pet-detection/configs/${configId}/detect`);
}

export function triggerAnalysis(configId: number) {
  return request.post<any, string>(`/api/pet-detection/configs/${configId}/analyze`);
}

// Pet Alarms
export function getPetAlarms(params?: { alarmType?: string; readStatus?: number }) {
  return request.get<any, AlarmMessageVO[]>('/api/pet-detection/alarms', { params });
}

export function getAbsentAlarms() {
  return request.get<any, AlarmMessageVO[]>('/api/pet-detection/alarms/absent');
}

export function getAbnormalActivityAlarms() {
  return request.get<any, AlarmMessageVO[]>('/api/pet-detection/alarms/abnormal-activity');
}

export function getStillnessAlarms() {
  return request.get<any, AlarmMessageVO[]>('/api/pet-detection/alarms/stillness');
}
