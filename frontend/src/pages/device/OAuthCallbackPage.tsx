import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { message, Spin, Result } from 'antd';

export default function OAuthCallbackPage() {
  const navigate = useNavigate();

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const success = params.get('success');
    const error = params.get('error');

    if (success === 'true') {
      message.success('设备绑定成功');
    } else if (error) {
      message.error('绑定失败: ' + decodeURIComponent(error));
    } else {
      message.error('授权回调参数缺失');
    }

    const timer = setTimeout(() => navigate('/devices/bind'), 2000);
    return () => clearTimeout(timer);
  }, [navigate]);

  const params = new URLSearchParams(window.location.search);
  const isSuccess = params.get('success') === 'true';
  const error = params.get('error');

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
      {isSuccess ? (
        <Result status="success" title="授权成功" subTitle="正在跳转到设备列表..." />
      ) : (
        <Result status="error" title="授权失败" subTitle={error ? decodeURIComponent(error) : '未知错误'} />
      )}
    </div>
  );
}
