export interface PetDetectionConfigVO {
  id: number;
  userId: number;
  petId: number;
  petName: string;
  deviceId: number;
  deviceName: string;
  deviceSerial: string;
  enabled: number;
  cooldownSeconds: number;
  remark: string;
  petAbsentMinutes: number;
  activityWindowMinutes: number;
  activityCountThreshold: number;
  stillnessMinutes: number;
  safeZones: PetSafeZoneVO[];
  createdAt: string;
  updatedAt: string;
}

export interface PetDetectionConfigRequest {
  petId: number;
  deviceId: number;
  enabled?: boolean;
  cooldownSeconds?: number;
  remark?: string;
  petAbsentMinutes?: number;
  activityWindowMinutes?: number;
  activityCountThreshold?: number;
  stillnessMinutes?: number;
}

export interface PetSafeZoneVO {
  id: number;
  detectionConfigId: number;
  zoneName: string;
  zoneType: 'RECTANGLE' | 'POLYGON';
  rectLeft: number;
  rectTop: number;
  rectRight: number;
  rectBottom: number;
  polygonPoints: Array<{ x: number; y: number }>;
  createdAt: string;
}

export interface PetSafeZoneRequest {
  detectionConfigId: number;
  zoneName?: string;
  zoneType: 'RECTANGLE' | 'POLYGON';
  rectLeft?: number;
  rectTop?: number;
  rectRight?: number;
  rectBottom?: number;
  polygonPoints?: Array<{ x: number; y: number }>;
}

export interface PetDetectionRecordVO {
  id: number;
  detectionConfigId: number;
  petId: number;
  petName: string;
  deviceId: number;
  deviceName: string;
  deviceSerial: string;
  detectTime: string;
  petCoordX: number;
  petCoordY: number;
  petWidth: number;
  petHeight: number;
  inSafeZone: number;
  alarmTriggered: number;
  snapshotUrl: string;
  createdAt: string;
}

export interface PetDetectionResultVO {
  recordId: number;
  petId: number;
  petName: string;
  deviceId: number;
  deviceName: string;
  detectTime: string;
  inSafeZone: boolean;
  alarmTriggered: boolean;
  snapshotUrl: string;
  message: string;
}

export interface PetDetectionRecordQueryDTO {
  detectionConfigId?: number;
  petId?: number;
  deviceId?: number;
  alarmTriggered?: number;
  startTime?: string;
  endTime?: string;
}
