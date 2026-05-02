import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Table, Button, Input, Select, Space, Popconfirm, message, Tag } from 'antd';
import { SyncOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { getDevices, syncDevices, deleteDevice, enableDevice, disableDevice } from '@/api/device';
import type { DeviceVO } from '@/types';
import StatusTag from '@/components/StatusTag';
import PageLoading from '@/components/PageLoading';

export default function DeviceListPage() {
  const navigate = useNavigate();
  const [devices, setDevices] = useState<DeviceVO[]>([]);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [sourceFilter, setSourceFilter] = useState<string>('');

  const fetchDevices = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getDevices({
        keyword: keyword || undefined,
        status: statusFilter || undefined,
        sourceType: sourceFilter || undefined,
      });
      setDevices(data);
    } catch (err: any) {
      message.error(err.message);
    } finally {
      setLoading(false);
    }
  }, [keyword, statusFilter, sourceFilter]);

  useEffect(() => {
    fetchDevices();
  }, [fetchDevices]);

  const handleSync = async () => {
    setSyncing(true);
    try {
      const res = await syncDevices();
      message.success(`同步完成: ${res.message}`);
      fetchDevices();
    } catch (err: any) {
      message.error(err.message);
    } finally {
      setSyncing(false);
    }
  };

  const handleToggleStatus = async (record: DeviceVO) => {
    try {
      if (record.status === 'DISABLED') {
        await enableDevice(record.id);
        message.success('设备已启用');
      } else {
        await disableDevice(record.id);
        message.success('设备已禁用');
      }
      fetchDevices();
    } catch (err: any) {
      message.error(err.message);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteDevice(id);
      message.success('设备已删除');
      fetchDevices();
    } catch (err: any) {
      message.error(err.message);
    }
  };

  const columns: ColumnsType<DeviceVO> = [
    { title: '设备序列号', dataIndex: 'deviceSerial', key: 'deviceSerial', width: 180 },
    { title: '设备名称', dataIndex: 'deviceName', key: 'deviceName', width: 150 },
    { title: '设备类型', dataIndex: 'deviceType', key: 'deviceType', width: 120 },
    {
      title: '来源',
      dataIndex: 'sourceType',
      key: 'sourceType',
      width: 80,
      render: (t: string) => <Tag>{t}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 90,
      render: (s: string) => <StatusTag status={s} />,
    },
    { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
    {
      title: '操作',
      key: 'action',
      width: 220,
      render: (_: unknown, record: DeviceVO) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => navigate(`/devices/${record.id}`)}>
            详情
          </Button>
          <Button type="link" size="small" onClick={() => handleToggleStatus(record)}>
            {record.status === 'DISABLED' ? '启用' : '禁用'}
          </Button>
          <Popconfirm title="确认删除此设备？" onConfirm={() => handleDelete(record.id)}>
            <Button type="link" size="small" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-semibold m-0">设备管理</h2>
        <Space>
          <Button icon={<SyncOutlined />} onClick={handleSync} loading={syncing}>
            从萤石同步
          </Button>
        </Space>
      </div>

      <div className="flex gap-3 mb-4">
        <Input
          placeholder="搜索设备名称/序列号"
          prefix={<SearchOutlined />}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onPressEnter={fetchDevices}
          style={{ width: 240 }}
          allowClear
        />
        <Select
          placeholder="状态筛选"
          value={statusFilter || undefined}
          onChange={(v) => setStatusFilter(v || '')}
          allowClear
          style={{ width: 120 }}
          options={[
            { label: '全部', value: '' },
            { label: '在线', value: 'ONLINE' },
            { label: '离线', value: 'OFFLINE' },
            { label: '已禁用', value: 'DISABLED' },
          ]}
        />
        <Select
          placeholder="来源筛选"
          value={sourceFilter || undefined}
          onChange={(v) => setSourceFilter(v || '')}
          allowClear
          style={{ width: 120 }}
          options={[
            { label: '全部', value: '' },
            { label: 'EZVIZ', value: 'EZVIZ' },
            { label: 'RTSP', value: 'RTSP' },
            { label: 'UPLOAD', value: 'UPLOAD' },
          ]}
        />
      </div>

      <Table columns={columns} dataSource={devices} rowKey="id" loading={loading} />
    </div>
  );
}
