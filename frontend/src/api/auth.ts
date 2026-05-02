import request from './request';
import type { AuthLoginVO, UserInfoVO, LoginRequest, RegisterRequest } from '@/types';

export function login(data: LoginRequest) {
  return request.post<any, AuthLoginVO>('/api/auth/login', data);
}

export function register(data: RegisterRequest) {
  return request.post<any, AuthLoginVO>('/api/auth/register', data);
}

export function getUserInfo() {
  return request.get<any, UserInfoVO>('/api/auth/me');
}

export function logout() {
  return request.post<any, null>('/api/auth/logout');
}
