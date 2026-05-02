import { useNavigate, useLocation } from 'react-router-dom';
import { Layout, Menu } from 'antd';
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

const { Sider } = Layout;

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

export default function Sidebar() {
  const navigate = useNavigate();
  const location = useLocation();
  const collapsed = useAppStore((s) => s.sidebarCollapsed);

  const selectedKey = '/' + location.pathname.split('/').filter(Boolean).slice(0, 2).join('/');

  return (
    <Sider trigger={null} collapsible collapsed={collapsed} width={220} theme="dark">
      <div className="h-16 flex items-center justify-center">
        <span className="text-white text-lg font-bold">{collapsed ? 'AIoT' : '宠物监测 AIoT'}</span>
      </div>
      <Menu
        theme="dark"
        mode="inline"
        selectedKeys={[selectedKey]}
        items={menuItems}
        onClick={({ key }) => navigate(key)}
      />
    </Sider>
  );
}
