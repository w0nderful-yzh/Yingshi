import { useNavigate } from 'react-router-dom';
import { Layout, Badge, Dropdown, Avatar, Space, Button } from 'antd';
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

const { Header: AntHeader } = Layout;

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
    <AntHeader className="!bg-white !px-4 flex items-center justify-between shadow-sm">
      <Button
        type="text"
        icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
        onClick={toggleSidebar}
      />
      <Space size="middle">
        <Badge count={unreadCount} size="small">
          <Button type="text" icon={<BellOutlined />} onClick={() => navigate('/alarms')} />
        </Badge>
        <Dropdown menu={dropdownItems} placement="bottomRight">
          <Space className="cursor-pointer">
            <Avatar size="small" icon={<UserOutlined />} />
            <span>{user?.nickname || user?.username || '-'}</span>
          </Space>
        </Dropdown>
      </Space>
    </AntHeader>
  );
}
