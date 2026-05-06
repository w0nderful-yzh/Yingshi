import { useNavigate } from 'react-router-dom';
import { Badge, Dropdown, Avatar, Space, Button } from 'antd';
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  BellOutlined,
  UserOutlined,
  LogoutOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import { useAppStore } from '@/store/appStore';
import { useAuthStore } from '@/store/authStore';
import { useAlarmStore } from '@/store/alarmStore';

/* ------------------------------------------------------------------ */
/*  Header — 玻璃拟态顶栏                                               */
/* ------------------------------------------------------------------ */

export default function Header() {
  const navigate = useNavigate();
  const collapsed = useAppStore((s) => s.sidebarCollapsed);
  const toggleSidebar = useAppStore((s) => s.toggleSidebar);
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const unreadCount = useAlarmStore((s) => s.unreadCount);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const dropdownItems = {
    items: [
      { key: 'settings', icon: <SettingOutlined />, label: '系统设置', onClick: () => navigate('/settings') },
      { type: 'divider' as const },
      { key: 'logout', icon: <LogoutOutlined />, label: '退出登录', danger: true, onClick: handleLogout },
    ],
  };

  return (
    <header className="glass-header">
      <Button
        type="text"
        icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
        onClick={toggleSidebar}
        className="glass-header__toggle"
      />
      <Space size="middle">
        <Badge count={unreadCount} size="small">
          <Button
            type="text"
            icon={<BellOutlined />}
            onClick={() => navigate('/alarms')}
            className="glass-header__icon-btn"
          />
        </Badge>
        <Dropdown menu={dropdownItems} placement="bottomRight">
          <Space className="cursor-pointer glass-header__user">
            <Avatar size="small" icon={<UserOutlined />} className="glass-header__avatar" />
            <span className="glass-header__username">{user?.nickname || user?.username || '-'}</span>
          </Space>
        </Dropdown>
      </Space>
    </header>
  );
}
