import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Card, Table, Tag, Space, Popconfirm, message, Empty, Spin } from 'antd';
import { LinkOutlined, DisconnectOutlined, StopOutlined } from '@ant-design/icons';
import {
  getEzvizOAuthStatus,
  getEzvizAuthUrl,
  getUserDevices,
  unbindUserDevice,
  revokeEzvizOAuth,
} from '@/api/ezvizOAuth';
import { useAuthStore } from '@/store/authStore';
import type { UserDeviceVO } from '@/types';
import { canWriteRole } from '@/utils/permission';

export default function DeviceBindPage() {
  const navigate = useNavigate();
  const role = useAuthStore((s) => s.user?.role);
  const [authorized, setAuthorized] = useState<boolean | null>(null);
  const [loading, setLoading] = useState(true);
  const [bindingLoading, setBindingLoading] = useState(false);
  const [devices, setDevices] = useState<UserDeviceVO[]>([]);
  const canWrite = canWriteRole(role);

  const loadDevices = useCallback(async () => {
    try {
      const list = await getUserDevices();
      setDevices(list);
    } catch {
      setDevices([]);
    }
  }, []);

  const checkStatus = useCallback(async () => {
    setLoading(true);
    try {
      const status = await getEzvizOAuthStatus();
      setAuthorized(status.authorized);
      if (status.authorized) {
        await loadDevices();
      }
    } catch {
      setAuthorized(false);
    } finally {
      setLoading(false);
    }
  }, [loadDevices]);

  useEffect(() => {
    checkStatus();
  }, [checkStatus]);

  async function handleBind() {
    setBindingLoading(true);
    try {
      const { authUrl } = await getEzvizAuthUrl();
      window.location.href = authUrl;
    } catch (err: any) {
      message.error('获取授权链接失败: ' + (err?.message || '未知错误'));
      setBindingLoading(false);
    }
  }

  async function handleUnbind(id: number) {
    try {
      await unbindUserDevice(id);
      message.success('解绑成功');
      loadDevices();
    } catch (err: any) {
      message.error('解绑失败: ' + (err?.message || '未知错误'));
    }
  }

  async function handleRevoke() {
    try {
      await revokeEzvizOAuth();
      message.success('已撤销授权');
      setAuthorized(false);
      setDevices([]);
    } catch (err: any) {
      message.error('撤销失败: ' + (err?.message || '未知错误'));
    }
  }

  const columns = [
    { title: '设备序列号', dataIndex: 'deviceSerial', key: 'deviceSerial' },
    { title: '设备名称', dataIndex: 'deviceName', key: 'deviceName' },
    { title: '设备型号', dataIndex: 'deviceType', key: 'deviceType' },
    { title: '通道号', dataIndex: 'channelNo', key: 'channelNo' },
    {
      title: '绑定时间',
      dataIndex: 'boundAt',
      key: 'boundAt',
      render: (t: string) => (t ? new Date(t).toLocaleString() : '-'),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (s: number) => <Tag color={s === 1 ? 'green' : 'default'}>{s === 1 ? '已绑定' : '已解绑'}</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: UserDeviceVO) => (
        canWrite ? (
          <Popconfirm title="确认解绑该设备？" onConfirm={() => handleUnbind(record.id)}>
            <Button type="link" danger size="small">解绑</Button>
          </Popconfirm>
        ) : null
      ),
    },
  ];

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 120 }}>
        <Spin size="large" />
      </div>
    );
  }

  if (!authorized) {
    return (
      <Card>
        <Empty
          description={canWrite ? '尚未绑定萤石账号' : '当前角色无设备绑定权限'}
          style={{ padding: '60px 0' }}
        >
          {canWrite && (
            <Button
              type="primary"
              icon={<LinkOutlined />}
              size="large"
              loading={bindingLoading}
              onClick={handleBind}
            >
              绑定萤石设备
            </Button>
          )}
        </Empty>
      </Card>
    );
  }

  return (
    <Card
      title="我的萤石设备"
      extra={
        canWrite ? (
          <Space>
            <Button icon={<LinkOutlined />} onClick={handleBind}>
              重新授权
            </Button>
            <Popconfirm title="撤销授权将解绑所有设备，确认？" onConfirm={handleRevoke}>
              <Button icon={<StopOutlined />} danger>
                撤销授权
              </Button>
            </Popconfirm>
          </Space>
        ) : null
      }
    >
      <Table
        dataSource={devices}
        columns={columns}
        rowKey="id"
        pagination={false}
        locale={{ emptyText: <Empty description="暂无绑定设备" /> }}
      />
    </Card>
  );
}
