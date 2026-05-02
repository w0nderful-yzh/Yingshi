export interface AlarmMessageVO {
  id: number;
  deviceId: number;
  deviceSerial: string;
  deviceName: string;
  channelNo: number;
  alarmType: string;
  alarmName: string;
  alarmTime: string;
  alarmPicUrl: string;
  alarmContent: string;
  readStatus: number;
  source: string;
  createdAt: string;
}

export interface AlarmUnreadCountVO {
  count: number;
}

export interface AlarmQueryDTO {
  deviceId?: number;
  readStatus?: number;
  startTime?: string;
  endTime?: string;
  keyword?: string;
}

export interface AlarmSyncResultDTO {
  deviceCount: number;
  fetchedCount: number;
  insertedCount: number;
  message: string;
}
