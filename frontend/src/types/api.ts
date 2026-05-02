export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  requestId: string;
  timestamp: string;
}

export enum BusinessCode {
  SUCCESS = 0,
  PARAM_INVALID = 40001,
  RESOURCE_NOT_FOUND = 40004,
  STATUS_CONFLICT = 40009,
  UNAUTHORIZED = 40100,
  FORBIDDEN = 40300,
  INTERNAL_ERROR = 50000,
  MODEL_SERVICE_ERROR = 50010,
}
