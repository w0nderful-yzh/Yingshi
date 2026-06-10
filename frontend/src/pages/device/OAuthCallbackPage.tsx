import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { message, Result, Spin } from 'antd';
import { handleEzvizCallback } from '@/api/ezvizOAuth';

export default function OAuthCallbackPage() {
  const navigate = useNavigate();
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [errorText, setErrorText] = useState('');

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const success = params.get('success') === 'true';
    const error = params.get('error');
    const authCode = params.get('authCode') || params.get('code');
    const state = params.get('state');
    const deviceSerials = params.get('deviceSerials') || undefined;
    const deviceTrustId = params.get('deviceTrustId') || undefined;

    let timer: number | undefined;

    if (success) {
      setStatus('success');
      message.success('设备绑定成功');
      timer = window.setTimeout(() => navigate('/devices/bind'), 1500);
      return () => {
        if (timer) {
          window.clearTimeout(timer);
        }
      };
    }

    if (error) {
      const decoded = decodeURIComponent(error);
      setStatus('error');
      setErrorText(decoded);
      message.error('绑定失败: ' + decoded);
      timer = window.setTimeout(() => navigate('/devices/bind'), 2000);
      return () => {
        if (timer) {
          window.clearTimeout(timer);
        }
      };
    }

    if (!authCode || !state) {
      const missing = '授权回调参数缺失';
      setStatus('error');
      setErrorText(missing);
      message.error(missing);
      timer = window.setTimeout(() => navigate('/devices/bind'), 2000);
      return () => {
        if (timer) {
          window.clearTimeout(timer);
        }
      };
    }

    handleEzvizCallback({ authCode, state, deviceSerials, deviceTrustId })
      .then(() => {
        setStatus('success');
        message.success('设备绑定成功');
        timer = window.setTimeout(() => navigate('/devices/bind'), 1500);
      })
      .catch((err: any) => {
        const msg = err?.message || '未知错误';
        setStatus('error');
        setErrorText(msg);
        message.error('绑定失败: ' + msg);
        timer = window.setTimeout(() => navigate('/devices/bind'), 2000);
      });

    return () => {
      if (timer) {
        window.clearTimeout(timer);
      }
    };
  }, [navigate]);

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
      {status === 'loading' ? (
        <div style={{ textAlign: 'center' }}>
          <Spin size="large" />
          <div style={{ marginTop: 16, color: '#64748b' }}>正在完成授权绑定...</div>
        </div>
      ) : status === 'success' ? (
        <Result status="success" title="授权成功" subTitle="正在跳转到设备列表..." />
      ) : (
        <Result status="error" title="授权失败" subTitle={errorText || '未知错误'} />
      )}
    </div>
  );
}
