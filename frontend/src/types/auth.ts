export interface AuthLoginVO {
  token: string;
  tokenType: string;
  expiresIn: number;
  userId: number;
  username: string;
  role: string;
}

export interface UserInfoVO {
  id: number;
  username: string;
  nickname: string;
  role: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
  nickname?: string;
}
