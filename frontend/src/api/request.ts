import axios from 'axios';
import { message } from 'antd';
import { getToken, removeToken } from '@/utils/token';
import type { ApiResponse } from '@/types';

const request = axios.create({
  baseURL: '',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

request.interceptors.request.use(
  (config) => {
    const token = getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

request.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse<unknown>;
    if (res.code === 0) {
      return res.data as any;
    }
    if (res.code === 40100) {
      removeToken();
      window.location.href = '/login';
      return Promise.reject(new Error(res.message || '登录已过期'));
    }
    return Promise.reject(new Error(res.message || '请求失败'));
  },
  (error) => {
    if (error.response?.status === 401) {
      removeToken();
      window.location.href = '/login';
      return Promise.reject(new Error('登录已过期'));
    }
    const msg = error.response?.data?.message || error.message || '网络错误';
    return Promise.reject(new Error(msg));
  }
);

export default request;
