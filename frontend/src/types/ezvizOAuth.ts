export interface UserDeviceVO {
  id: number;
  deviceSerial: string;
  deviceName: string;
  deviceType: string;
  channelNo: number;
  boundAt: string;
  status: number;
}

export interface EzvizAuthUrlVO {
  authUrl: string;
  state: string;
}
