import React from 'react';
import ReactDOM from 'react-dom/client';
import { RouterProvider } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { router } from '@/router';
import AppErrorBoundary from '@/components/AppErrorBoundary';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider
      locale={zhCN}
      theme={{
        token: {
          colorPrimary: '#17875f',
          colorInfo: '#2f6f9f',
          colorSuccess: '#17875f',
          colorWarning: '#c88722',
          colorError: '#c94f4f',
          colorText: '#1d2924',
          colorTextSecondary: '#68756f',
          colorBorder: '#dce4df',
          borderRadius: 10,
          fontFamily: '"PingFang SC", "Microsoft YaHei", -apple-system, BlinkMacSystemFont, sans-serif',
        },
        components: {
          Button: { controlHeight: 36, primaryShadow: 'none' },
          Card: { headerBg: 'transparent' },
          Table: { headerBg: '#f4f7f5', rowHoverBg: '#f5faf7' },
        },
      }}
    >
      <AppErrorBoundary>
        <RouterProvider router={router} />
      </AppErrorBoundary>
    </ConfigProvider>
  </React.StrictMode>
);
