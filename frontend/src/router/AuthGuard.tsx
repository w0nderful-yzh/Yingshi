import { useEffect } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import { Spin } from 'antd';
import { useAuthStore } from '@/store/authStore';
import { useAlarmStore } from '@/store/alarmStore';

export default function AuthGuard() {
  const { isAuthenticated, loading, initialize } = useAuthStore();
  const { startPolling, stopPolling } = useAlarmStore();
  const navigate = useNavigate();

  useEffect(() => {
    initialize();
  }, [initialize]);

  useEffect(() => {
    if (!loading && !isAuthenticated) {
      navigate('/login', { replace: true });
    }
  }, [loading, isAuthenticated, navigate]);

  useEffect(() => {
    if (isAuthenticated) {
      startPolling();
      return () => stopPolling();
    }
  }, [isAuthenticated, startPolling, stopPolling]);

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Spin size="large" tip="加载中..." />
      </div>
    );
  }

  if (!isAuthenticated) return null;

  return <Outlet />;
}
