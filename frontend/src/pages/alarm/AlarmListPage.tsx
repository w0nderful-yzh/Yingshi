import { useEffect, useState, useCallback } from 'react';
import { Table, Button, Select, Input, Space, Tag, Popconfirm, Modal, Image, message, Tabs } from 'antd';
import { SyncOutlined, CheckOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import {
  getAlarms,
  markAlarmRead,
  markAllRead,
  deleteAlarm,
  syncAlarms,
} from '@/api/alarm';
import {
  getPetAlarms,
  getAbsentAlarms,
  getAbnormalActivityAlarms,
  getStillnessAlarms,
} from '@/api/petDetection';
import { getDevices } from '@/api/device';
import type { AlarmMessageVO, DeviceVO } from '@/types';
import { AlarmTypeMap, AlarmSource } from '@/utils/constants';
import { formatDate } from '@/utils/format';
import { useAlarmStore } from '@/store/alarmStore';

export default function AlarmListPage() {
  const [alarms, setAlarms] = useState<AlarmMessageVO[]>([]);
  const [loading, setLoading] = useState(true);
  const [devices, setDevices] = useState<DeviceVO[]>([]);
  const [filters, setFilters] = useState({ deviceId: undefined as number | undefined, readStatus: undefined as number | undefined, keyword: '' });
  const [activeTab, setActiveTab] = useState('all');
  const [detailAlarm, setDetailAlarm] = useState<AlarmMessageVO | null>(null);
  const [syncing, setSyncing] = useState(false);
  const { decrementCount, resetCount, fetchUnreadCount } = useAlarmStore();

  useEffect(() => {
    getDevices().then(setDevices).catch(() => {});
  }, []);

  const fetchAlarms = useCallback(async () => {
    setLoading(true);
    try {
      let data: AlarmMessageVO[];
      switch (activeTab) {
        case 'pet':
          data = await getPetAlarms();
          break;
        case 'absent':
          data = await getAbsentAlarms();
          break;
        case 'activity':
          data = await getAbnormalActivityAlarms();
          break;
        case 'stillness':
          data = await getStillnessAlarms();
          break;
        default:
          data = await getAlarms({
            deviceId: filters.deviceId,
            readStatus: filters.readStatus,
            keyword: filters.keyword || undefined,
          });
      }
      setAlarms(data);
    } catch (err: any) {
      message.error(err.message);
    } finally {
      setLoading(false);
    }
  }, [activeTab, filters]);

  useEffect(() => {
    fetchAlarms();
  }, [fetchAlarms]);

  const handleMarkRead = async (id: number) => {
    try {
      await markAlarmRead(id);
      setAlarms((prev) => prev.map((a) => (a.id === id ? { ...a, readStatus: 1 } : a)));
      decrementCount();
    } catch (err: any) {
      message.error(err.message);
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await markAllRead(filters.deviceId);
      setAlarms((prev) => prev.map((a) => ({ ...a, readStatus: 1 })));
      resetCount();
      message.success('全部已读');
    } catch (err: any) {
      message.error(err.message);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteAlarm(id);
      setAlarms((prev) => prev.filter((a) => a.id !== id));
      fetchUnreadCount();
      message.success('删除成功');
    } catch (err: any) {
      message.error(err.message);
    }
  };

  const handleSync = async () => {
    setSyncing(true);
    try {
      const res = await syncAlarms();
      message.success(`同步完成: ${res.message}`);
      fetchAlarms();
      fetchUnreadCount();
    } catch (err: any) {
      message.error(err.message);
    } finally {
      setSyncing(false);
    }
  };

  const handleViewDetail = async (record: AlarmMessageVO) => {
    if (record.readStatus === 0) {
      await handleMarkRead(record.id);
    }
    setDetailAlarm(record);
  };

  const columns: ColumnsType<AlarmMessageVO> = [
    {
      title: '设备',
      dataIndex: 'deviceName',
      key: 'deviceName',
      width: 120,
      ellipsis: true,
    },
    {
      title: '类型',
      dataIndex: 'alarmType',
      key: 'alarmType',
      width: 140,
      render: (t: string) => AlarmTypeMap[t] || t || '-',
    },
    {
      title: '内容',
      dataIndex: 'alarmContent',
      key: 'alarmContent',
      ellipsis: true,
    },
    {
      title: '时间',
      dataIndex: 'alarmTime',
      key: 'alarmTime',
      width: 160,
      render: (t: string) => formatDate(t),
    },
    {
      title: '来源',
      dataIndex: 'source',
      key: 'source',
      width: 90,
      render: (s: string) => <Tag color={s === AlarmSource.PET_DETECT ? 'orange' : 'blue'}>{s}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'readStatus',
      key: 'readStatus',
      width: 80,
      render: (s: number) => (s === 0 ? <Tag color="red">未读</Tag> : <Tag>已读</Tag>),
    },
    {
      title: '操作',
      key: 'action',
      width: 160,
      render: (_: unknown, record) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => handleViewDetail(record)}>
            查看
          </Button>
          {record.readStatus === 0 && (
            <Button type="link" size="small" icon={<CheckOutlined />} onClick={() => handleMarkRead(record.id)}>
              已读
            </Button>
          )}
          <Popconfirm title="确认删除？" onConfirm={() => handleDelete(record.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const tabItems = [
    { key: 'all', label: '全部告警' },
    { key: 'pet', label: '宠物告警' },
    { key: 'absent', label: '宠物消失' },
    { key: 'activity', label: '异常活动' },
    { key: 'stillness', label: '长时间静止' },
  ];

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-semibold m-0">告警消息</h2>
        <Space>
          <Button icon={<SyncOutlined />} onClick={handleSync} loading={syncing}>
            同步告警
          </Button>
          <Button icon={<CheckOutlined />} onClick={handleMarkAllRead}>
            全部已读
          </Button>
        </Space>
      </div>

      <Tabs activeKey={activeTab} onChange={setActiveTab} items={tabItems} className="mb-4" />

      {activeTab === 'all' && (
        <div className="flex gap-3 mb-4">
          <Select
            placeholder="设备筛选"
            value={filters.deviceId}
            onChange={(v) => setFilters((f) => ({ ...f, deviceId: v }))}
            allowClear
            style={{ width: 180 }}
            showSearch
            optionFilterProp="label"
            options={devices.map((d) => ({ label: d.deviceName, value: d.id }))}
          />
          <Select
            placeholder="状态筛选"
            value={filters.readStatus}
            onChange={(v) => setFilters((f) => ({ ...f, readStatus: v }))}
            allowClear
            style={{ width: 120 }}
            options={[
              { label: '全部', value: undefined },
              { label: '未读', value: 0 },
              { label: '已读', value: 1 },
            ]}
          />
          <Input
            placeholder="关键词搜索"
            value={filters.keyword}
            onChange={(e) => setFilters((f) => ({ ...f, keyword: e.target.value }))}
            onPressEnter={fetchAlarms}
            prefix={<SearchOutlined />}
            style={{ width: 200 }}
            allowClear
          />
        </div>
      )}

      <Table columns={columns} dataSource={alarms} rowKey="id" loading={loading} />

      <Modal
        title="告警详情"
        open={!!detailAlarm}
        onCancel={() => setDetailAlarm(null)}
        footer={null}
        width={600}
      >
        {detailAlarm && (
          <div>
            {detailAlarm.alarmPicUrl && (
              <div className="mb-4 text-center">
                <Image src={detailAlarm.alarmPicUrl} alt="告警图片" style={{ maxHeight: 300 }} />
              </div>
            )}
            <div className="grid grid-cols-2 gap-2 text-sm">
              <div>设备: {detailAlarm.deviceName}</div>
              <div>序列号: {detailAlarm.deviceSerial}</div>
              <div>类型: {AlarmTypeMap[detailAlarm.alarmType] || detailAlarm.alarmType}</div>
              <div>来源: {detailAlarm.source}</div>
              <div>时间: {formatDate(detailAlarm.alarmTime)}</div>
              <div>状态: {detailAlarm.readStatus === 0 ? '未读' : '已读'}</div>
            </div>
            {detailAlarm.alarmContent && (
              <div className="mt-3 p-3 bg-gray-50 rounded text-sm">{detailAlarm.alarmContent}</div>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}
