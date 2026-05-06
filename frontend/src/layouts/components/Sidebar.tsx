import { useRef, useState, useCallback, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  DashboardOutlined,
  CameraOutlined,
  VideoCameraOutlined,
  PlayCircleOutlined,
  HeartOutlined,
  SettingOutlined,
  FileSearchOutlined,
  BellOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { useAppStore } from '@/store/appStore';

/* ------------------------------------------------------------------ */
/*  菜单项配置 (保留原有路由 key 与图标不变)                              */
/* ------------------------------------------------------------------ */
const menuItems = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: '总览' },
  { key: '/devices', icon: <CameraOutlined />, label: '设备管理' },
  { key: '/video/live', icon: <VideoCameraOutlined />, label: '实时监控' },
  { key: '/video/playback', icon: <PlayCircleOutlined />, label: '视频回放' },
  { key: '/pets', icon: <HeartOutlined />, label: '宠物管理' },
  { key: '/detection/configs', icon: <SettingOutlined />, label: '检测配置' },
  { key: '/detection/records', icon: <FileSearchOutlined />, label: '检测记录' },
  { key: '/alarms', icon: <BellOutlined />, label: '告警消息' },
  { key: '/settings', icon: <UserOutlined />, label: '系统设置' },
];

/* ------------------------------------------------------------------ */
/*  Sidebar 组件                                                       */
/* ------------------------------------------------------------------ */
export default function Sidebar() {
  const navigate = useNavigate();
  const location = useLocation();
  const collapsed = useAppStore((s) => s.sidebarCollapsed);

  /* 当前选中项 — 保留原有逻辑 */
  const selectedKey =
    '/' + location.pathname.split('/').filter(Boolean).slice(0, 2).join('/');

  /* ---------- 鼠标悬浮跟踪 ---------- */
  const menuListRef = useRef<HTMLDivElement>(null);
  const hoverRef = useRef<HTMLDivElement>(null);
  const [hoverVisible, setHoverVisible] = useState(false);

  /** 鼠标在菜单区域内移动 — 计算悬浮框位置 */
  const handleMouseMove = useCallback(
    (e: React.MouseEvent<HTMLDivElement>) => {
      const container = menuListRef.current;
      const hoverEl = hoverRef.current;
      if (!container || !hoverEl) return;

      /* 找到鼠标当前所在的菜单项 */
      const items = container.querySelectorAll<HTMLElement>('[data-menu-item]');
      let target: HTMLElement | null = null;
      for (const item of items) {
        const rect = item.getBoundingClientRect();
        if (e.clientY >= rect.top && e.clientY <= rect.bottom) {
          target = item;
          break;
        }
      }

      if (target) {
        const containerRect = container.getBoundingClientRect();
        const targetRect = target.getBoundingClientRect();
        /* 悬浮框定位：与菜单项对齐，带 4px 内边距 */
        hoverEl.style.top = `${targetRect.top - containerRect.top}px`;
        hoverEl.style.height = `${targetRect.height}px`;
        setHoverVisible(true);
      }
    },
    [],
  );

  /** 鼠标离开菜单区域 — 隐藏悬浮框 */
  const handleMouseLeave = useCallback(() => {
    setHoverVisible(false);
  }, []);

  /* ---------- 侧边栏折叠时同步隐藏悬浮框 ---------- */
  useEffect(() => {
    setHoverVisible(false);
  }, [collapsed]);

  /* ---------------------------------------------------------------- */
  /*  渲染                                                             */
  /* ---------------------------------------------------------------- */
  const siderWidth = collapsed ? 80 : 220;

  return (
    <aside
      className="glass-sidebar"
      style={{ width: siderWidth }}
    >
      {/* ---- Logo 区域 ---- */}
      <div className="glass-sidebar__logo">
        <span className="glass-sidebar__logo-text">
          {collapsed ? '🐾' : '🐾 宠物监测 AIoT'}
        </span>
      </div>

      {/* ---- 菜单列表 ---- */}
      <div
        ref={menuListRef}
        className="glass-sidebar__menu"
        onMouseMove={handleMouseMove}
        onMouseLeave={handleMouseLeave}
      >
        {/* 悬浮跟踪框 */}
        <div
          ref={hoverRef}
          className="glass-sidebar__hover-indicator"
          style={{ opacity: hoverVisible ? 1 : 0 }}
        />

        {menuItems.map((item) => {
          const isActive = selectedKey === item.key;
          return (
            <div
              key={item.key}
              data-menu-item
              className={`glass-sidebar__item ${isActive ? 'glass-sidebar__item--active' : ''}`}
              onClick={() => navigate(item.key)}
            >
              <span className="glass-sidebar__item-icon">{item.icon}</span>
              {!collapsed && (
                <span className="glass-sidebar__item-label">{item.label}</span>
              )}
            </div>
          );
        })}
      </div>
    </aside>
  );
}
