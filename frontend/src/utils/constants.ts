export const DeviceStatus = {
  ONLINE: 'ONLINE',
  OFFLINE: 'OFFLINE',
  DISABLED: 'DISABLED',
} as const;

export const AlarmSource = {
  EZVIZ: 'EZVIZ',
  PET_DETECT: 'PET_DETECT',
} as const;

export const PetAlarmType = {
  PET_OUT_OF_ZONE: 'PET_OUT_OF_ZONE',
  PET_ABSENT: 'PET_ABSENT',
  PET_ABNORMAL_ACTIVITY: 'PET_ABNORMAL_ACTIVITY',
  PET_LONG_STILLNESS: 'PET_LONG_STILLNESS',
} as const;

export const ZoneType = {
  RECTANGLE: 'RECTANGLE',
  POLYGON: 'POLYGON',
} as const;

export const ReadStatus = {
  UNREAD: 0,
  READ: 1,
} as const;

export const VideoProtocol = {
  EZOPEN: 1,
  HLS: 2,
  RTMP: 3,
  FLV: 4,
} as const;

export const VideoQuality = {
  HD: 1,
  SMOOTH: 2,
} as const;

export const PetType = {
  DOG: 'DOG',
  CAT: 'CAT',
  OTHER: 'OTHER',
} as const;

export const PetGender = {
  MALE: 'MALE',
  FEMALE: 'FEMALE',
  UNKNOWN: 'UNKNOWN',
} as const;

export const DeviceStatusMap: Record<string, { label: string; color: string }> = {
  ONLINE: { label: '在线', color: 'green' },
  OFFLINE: { label: '离线', color: 'default' },
  DISABLED: { label: '已禁用', color: 'red' },
};

export const AlarmTypeMap: Record<string, string> = {
  PET_OUT_OF_ZONE: '离开安全区域',
  PET_ABSENT: '宠物消失',
  PET_ABNORMAL_ACTIVITY: '异常活动',
  PET_LONG_STILLNESS: '长时间静止',
};

export const PetTypeMap: Record<string, string> = {
  DOG: '狗',
  CAT: '猫',
  OTHER: '其他',
};

export const GenderMap: Record<string, string> = {
  MALE: '公',
  FEMALE: '母',
  UNKNOWN: '未知',
};
