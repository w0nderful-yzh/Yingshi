import { Card, Descriptions, Button, Avatar, message } from 'antd';
import { UserOutlined, LogoutOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';

export default function UserSettingsPage() {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">系统设置</h2>

      <Card>
        <div className="flex items-center gap-6 mb-6">
          <Avatar size={80} icon={<UserOutlined />} />
          <div>
            <h3 className="text-lg m-0">{user?.nickname || user?.username}</h3>
            <p className="text-gray-400 m-0">角色: {user?.role}</p>
          </div>
        </div>

        <Descriptions column={1} bordered>
          <Descriptions.Item label="用户ID">{user?.id}</Descriptions.Item>
          <Descriptions.Item label="用户名">{user?.username}</Descriptions.Item>
          <Descriptions.Item label="昵称">{user?.nickname || '-'}</Descriptions.Item>
          <Descriptions.Item label="角色">{user?.role}</Descriptions.Item>
        </Descriptions>

        <div className="mt-6">
          <Button danger icon={<LogoutOutlined />} onClick={handleLogout}>
            退出登录
          </Button>
        </div>
      </Card>
    </div>
  );
}
