import { useLocation, useNavigate } from 'react-router-dom';
import { Avatar, Badge, Button, Dropdown, Space } from 'antd';
import {
  BellOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  SettingOutlined,
  UserOutlined,
  VideoCameraOutlined,
} from '@ant-design/icons';
import { useAppStore } from '@/store/appStore';
import { useAuthStore } from '@/store/authStore';
import { useAlarmStore } from '@/store/alarmStore';
import { roleLabel } from '@/utils/permission';

const pageMeta = [
  { match: '/dashboard', title: '运行总览', description: '查看设备、检测任务和告警的实时状态' },
  { match: '/video/live', title: '实时监控', description: '快速查看在线摄像头画面' },
  { match: '/video/playback', title: '视频回放', description: '按设备和时间检索历史录像' },
  { match: '/devices/bind', title: '绑定设备', description: '授权并接入新的萤石设备' },
  { match: '/devices', title: '设备管理', description: '管理摄像头状态和基础信息' },
  { match: '/pets', title: '宠物档案', description: '维护监测对象的基础资料' },
  { match: '/pet-ai', title: 'AI 助手', description: '结合宠物数据进行智能分析' },
  { match: '/detection/records', title: '检测记录', description: '回看每次识别结果和检测快照' },
  { match: '/detection/configs', title: '检测任务', description: '配置宠物、摄像头和安全区域规则' },
  { match: '/alarms', title: '告警中心', description: '集中处理需要关注的异常事件' },
  { match: '/settings', title: '系统设置', description: '管理账户和系统偏好' },
];

export default function Header() {
  const navigate = useNavigate();
  const location = useLocation();
  const collapsed = useAppStore((state) => state.sidebarCollapsed);
  const toggleSidebar = useAppStore((state) => state.toggleSidebar);
  const user = useAuthStore((state) => state.user);
  const logout = useAuthStore((state) => state.logout);
  const unreadCount = useAlarmStore((state) => state.unreadCount);
  const meta = pageMeta.find((item) => location.pathname.startsWith(item.match)) ?? pageMeta[0];

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <header className="ops-header">
      <div className="ops-header__context">
        <Button
          type="text"
          icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          onClick={toggleSidebar}
          className="ops-header__toggle"
          aria-label={collapsed ? '展开导航' : '收起导航'}
        />
        <div>
          <strong>{meta.title}</strong>
          <span>{meta.description}</span>
        </div>
      </div>

      <div className="ops-header__actions">
        <Button
          icon={<VideoCameraOutlined />}
          className="ops-header__live-button"
          onClick={() => navigate('/video/live')}
        >
          查看实时画面
        </Button>
        <Badge count={unreadCount} size="small" overflowCount={99}>
          <Button
            type="text"
            icon={<BellOutlined />}
            onClick={() => navigate('/alarms')}
            className="ops-header__icon-button"
            aria-label="打开告警中心"
          />
        </Badge>
        <Dropdown
          placement="bottomRight"
          menu={{
            items: [
              {
                key: 'settings',
                icon: <SettingOutlined />,
                label: '系统设置',
                onClick: () => navigate('/settings'),
              },
              { type: 'divider' },
              {
                key: 'logout',
                icon: <LogoutOutlined />,
                label: '退出登录',
                danger: true,
                onClick: handleLogout,
              },
            ],
          }}
        >
          <button type="button" className="ops-header__user">
            <Avatar size={34} icon={<UserOutlined />} />
            <span>
              <strong>{user?.nickname || user?.username || '-'}</strong>
              <small>{user?.role ? roleLabel(user.role) : '用户'}</small>
            </span>
          </button>
        </Dropdown>
      </div>
    </header>
  );
}
