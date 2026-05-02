export interface LiveUrlVO {
  deviceId: number;
  deviceSerial: string;
  channelNo: number;
  protocol: number;
  quality: number;
  url: string;
  expireTime: string;
}

export interface CloudRecordFileVO {
  deviceId: number;
  deviceSerial: string;
  channelNo: number;
  startTime: string;
  endTime: string;
  recordType: string;
  fileType: string;
  source: string;
}

export interface CloudPlaybackUrlVO {
  deviceId: number;
  deviceSerial: string;
  channelNo: number;
  protocol: number;
  quality: number;
  startTime: string;
  endTime: string;
  url: string;
  expireTime: string;
}
