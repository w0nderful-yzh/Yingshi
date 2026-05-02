export interface DeviceVO {
  id: number;
  deviceSerial: string;
  channelNo: number;
  deviceName: string;
  deviceType: string;
  sourceType: string;
  status: 'ONLINE' | 'OFFLINE' | 'DISABLED';
  remark: string;
  createdAt: string;
  updatedAt: string;
}

export interface DeviceUpdateDTO {
  deviceName: string;
  remark?: string;
  status?: string;
}

export interface DeviceSyncResultDTO {
  total: number;
  inserted: number;
  updated: number;
  message: string;
}
