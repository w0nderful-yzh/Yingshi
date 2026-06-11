import { useLocation, useNavigate } from 'react-router-dom';
import { Tooltip } from 'antd';
import {
  BellOutlined,
  CameraOutlined,
  DashboardOutlined,
  FileSearchOutlined,
  HeartOutlined,
  LinkOutlined,
  PlayCircleOutlined,
  RobotOutlined,
  SettingOutlined,
  UserOutlined,
  VideoCameraOutlined,
} from '@ant-design/icons';
import { useAppStore } from '@/store/appStore';
import { useAuthStore } from '@/store/authStore';
import { canWriteRole } from '@/utils/permission';

const menuGroups = [
  {
    label: '运行中心',
    items: [
      { key: '/dashboard', icon: <DashboardOutlined />, label: '运行总览' },
      { key: '/video/live', icon: <VideoCameraOutlined />, label: '实时监控' },
      { key: '/alarms', icon: <BellOutlined />, label: '告警中心' },
    ],
  },
  {
    label: '监测配置',
    items: [
      { key: '/detection/configs', icon: <SettingOutlined />, label: '检测任务' },
      { key: '/detection/records', icon: <FileSearchOutlined />, label: '检测记录' },
      { key: '/video/playback', icon: <PlayCircleOutlined />, label: '视频回放' },
    ],
  },
  {
    label: '资产管理',
    items: [
      { key: '/devices', icon: <CameraOutlined />, label: '设备管理' },
      { key: '/devices/bind', icon: <LinkOutlined />, label: '绑定设备', requiresWrite: true },
      { key: '/pets', icon: <HeartOutlined />, label: '宠物档案' },
    ],
  },
  {
    label: '辅助工具',
    items: [
      { key: '/pet-ai', icon: <RobotOutlined />, label: 'AI 助手' },
      { key: '/settings', icon: <UserOutlined />, label: '系统设置' },
    ],
  },
];

function isItemActive(pathname: string, key: string) {
  if (key === '/dashboard') return pathname === '/dashboard';
  if (key === '/detection/configs') {
    return pathname.startsWith('/detection/configs');
  }
  return pathname === key || pathname.startsWith(`${key}/`);
}

export default function Sidebar() {
  const navigate = useNavigate();
  const location = useLocation();
  const collapsed = useAppStore((state) => state.sidebarCollapsed);
  const role = useAuthStore((state) => state.user?.role);

  return (
    <aside className={`ops-sidebar ${collapsed ? 'is-collapsed' : ''}`}>
      <button type="button" className="ops-sidebar__brand" onClick={() => navigate('/dashboard')}>
        <span className="ops-sidebar__brand-mark">P</span>
        {!collapsed && (
          <span>
            <strong>Pet Sentinel</strong>
            <small>智能监测平台</small>
          </span>
        )}
      </button>

      <nav className="ops-sidebar__nav" aria-label="主导航">
        {menuGroups.map((group) => {
          const items = group.items.filter((item) => !item.requiresWrite || canWriteRole(role));
          if (!items.length) return null;
          return (
            <div className="ops-sidebar__group" key={group.label}>
              {!collapsed && <div className="ops-sidebar__group-label">{group.label}</div>}
              {items.map((item) => {
                const active = isItemActive(location.pathname, item.key);
                const button = (
                  <button
                    type="button"
                    key={item.key}
                    className={`ops-sidebar__item ${active ? 'is-active' : ''}`}
                    onClick={() => navigate(item.key)}
                    aria-current={active ? 'page' : undefined}
                  >
                    <span className="ops-sidebar__item-icon">{item.icon}</span>
                    {!collapsed && <span>{item.label}</span>}
                  </button>
                );
                return collapsed ? (
                  <Tooltip key={item.key} title={item.label} placement="right">
                    {button}
                  </Tooltip>
                ) : button;
              })}
            </div>
          );
        })}
      </nav>

      {!collapsed && (
        <div className="ops-sidebar__status">
          <span className="ops-sidebar__status-dot" />
          <div>
            <strong>服务运行中</strong>
            <small>每 30 秒刷新状态</small>
          </div>
        </div>
      )}
    </aside>
  );
}
