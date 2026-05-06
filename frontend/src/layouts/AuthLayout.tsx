import { Outlet } from 'react-router-dom';
import { Ripple } from '@/components/magicui/Ripple';

/* ------------------------------------------------------------------ */
/*  AuthLayout — 登录 / 注册页面的外层布局                                */
/*  柔和浅紫渐变背景 + 涟漪水波纹 + 分散光斑装饰                           */
/* ------------------------------------------------------------------ */

export default function AuthLayout() {
  return (
    <div className="auth-layout">
      {/* 分散的柔和渐变光斑 */}
      <div className="auth-layout__spot auth-layout__spot--1" />
      <div className="auth-layout__spot auth-layout__spot--2" />
      <div className="auth-layout__spot auth-layout__spot--3" />

      {/* 涟漪水波纹背景 */}
      <Ripple count={8} color="rgba(196, 181, 253, 0.10)" />

      {/* 极淡的爪印水印 */}
      <svg className="paw-watermark paw-watermark--1" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12,8A3,3 0 0,0 15,5A3,3 0 0,0 12,2A3,3 0 0,0 9,5A3,3 0 0,0 12,8M6,11A3,3 0 0,0 9,8A3,3 0 0,0 6,5A3,3 0 0,0 3,8A3,3 0 0,0 6,11M18,11A3,3 0 0,0 21,8A3,3 0 0,0 18,5A3,3 0 0,0 15,8A3,3 0 0,0 18,11M12,10C9,10 6,12.5 6,15.5C6,18.5 9,22 12,22C15,22 18,18.5 18,15.5C18,12.5 15,10 12,10Z" />
      </svg>
      <svg className="paw-watermark paw-watermark--2" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12,8A3,3 0 0,0 15,5A3,3 0 0,0 12,2A3,3 0 0,0 9,5A3,3 0 0,0 12,8M6,11A3,3 0 0,0 9,8A3,3 0 0,0 6,5A3,3 0 0,0 3,8A3,3 0 0,0 6,11M18,11A3,3 0 0,0 21,8A3,3 0 0,0 18,5A3,3 0 0,0 15,8A3,3 0 0,0 18,11M12,10C9,10 6,12.5 6,15.5C6,18.5 9,22 12,22C15,22 18,18.5 18,15.5C18,12.5 15,10 12,10Z" />
      </svg>
      <svg className="paw-watermark paw-watermark--3" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12,8A3,3 0 0,0 15,5A3,3 0 0,0 12,2A3,3 0 0,0 9,5A3,3 0 0,0 12,8M6,11A3,3 0 0,0 9,8A3,3 0 0,0 6,5A3,3 0 0,0 3,8A3,3 0 0,0 6,11M18,11A3,3 0 0,0 21,8A3,3 0 0,0 18,5A3,3 0 0,0 15,8A3,3 0 0,0 18,11M12,10C9,10 6,12.5 6,15.5C6,18.5 9,22 12,22C15,22 18,18.5 18,15.5C18,12.5 15,10 12,10Z" />
      </svg>

      {/* 页面内容 */}
      <div className="auth-layout__content">
        <Outlet />
      </div>
    </div>
  );
}
