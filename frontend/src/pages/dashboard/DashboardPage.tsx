import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Row, Col, Card, Statistic, Table, Button, Space, Tag, message } from 'antd';
import {
  CameraOutlined,
  CheckCircleOutlined,
  HeartOutlined,
  SettingOutlined,
  BellOutlined,
  SyncOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { getDevices, syncDevices } from '@/api/device';
import { getPets } from '@/api/pet';
import { getDetectionConfigs } from '@/api/petDetection';
import { getAlarms, syncAlarms } from '@/api/alarm';
import { useAlarmStore } from '@/store/alarmStore';
import type { DeviceVO, PetVO, PetDetectionConfigVO, AlarmMessageVO } from '@/types';
import { DeviceStatusMap, AlarmTypeMap } from '@/utils/constants';
import { formatDate } from '@/utils/format';
import PageLoading from '@/components/PageLoading';

export default function DashboardPage() {
  const navigate = useNavigate();
  const unreadCount = useAlarmStore((s) => s.unreadCount);
  const [devices, setDevices] = useState<DeviceVO[]>([]);
  const [pets, setPets] = useState<PetVO[]>([]);
  const [configs, setConfigs] = useState<PetDetectionConfigVO[]>([]);
  const [alarms, setAlarms] = useState<AlarmMessageVO[]>([]);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [devs, petList, cfgList, alarmList] = await Promise.all([
        getDevices().catch(() => []),
        getPets().catch(() => []),
        getDetectionConfigs().catch(() => []),
        getAlarms({ readStatus: 0 }).catch(() => []),
      ]);
      setDevices(devs);
      setPets(petList);
      setConfigs(cfgList);
      setAlarms(alarmList.slice(0, 10));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleSyncDevices = async () => {
    setSyncing(true);
    try {
      const res = await syncDevices();
      message.success(`设备同步完成: ${res.message}`);
      fetchData();
    } catch (err: any) {
      message.error(err.message);
    } finally {
      setSyncing(false);
    }
  };

  const handleSyncAlarms = async () => {
    try {
      const res = await syncAlarms();
      message.success(`告警同步完成: ${res.message}`);
      fetchData();
    } catch (err: any) {
      message.error(err.message);
    }
  };

  const onlineCount = devices.filter((d) => d.status === 'ONLINE').length;
  const activeConfigs = configs.filter((c) => c.enabled === 1).length;

  const alarmColumns: ColumnsType<AlarmMessageVO> = [
    { title: '设备', dataIndex: 'deviceName', key: 'deviceName', width: 120 },
    {
      title: '类型',
      dataIndex: 'alarmType',
      key: 'alarmType',
      width: 140,
      render: (t: string) => AlarmTypeMap[t] || t,
    },
    { title: '内容', dataIndex: 'alarmContent', key: 'alarmContent', ellipsis: true },
    {
      title: '时间',
      dataIndex: 'alarmTime',
      key: 'alarmTime',
      width: 160,
      render: (t: string) => formatDate(t),
    },
    {
      title: '状态',
      dataIndex: 'readStatus',
      key: 'readStatus',
      width: 80,
      render: (s: number) => (s === 0 ? <Tag color="red">未读</Tag> : <Tag>已读</Tag>),
    },
  ];

  if (loading) return <PageLoading />;

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-xl font-semibold m-0">总览</h2>
        <Space>
          <Button icon={<SyncOutlined />} onClick={handleSyncDevices} loading={syncing}>
            同步设备
          </Button>
          <Button icon={<SyncOutlined />} onClick={handleSyncAlarms}>
            同步告警
          </Button>
        </Space>
      </div>

      <Row gutter={16} className="mb-6">
        <Col span={6}>
          <Card hoverable onClick={() => navigate('/devices')}>
            <Statistic title="设备总数" value={devices.length} prefix={<CameraOutlined />} />
          </Card>
        </Col>
        <Col span={6}>
          <Card hoverable onClick={() => navigate('/devices')}>
            <Statistic
              title="在线设备"
              value={onlineCount}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#3f8600' }}
            />
          </Card>
        </Col>
        <Col span={4}>
          <Card hoverable onClick={() => navigate('/pets')}>
            <Statistic title="宠物数量" value={pets.length} prefix={<HeartOutlined />} />
          </Card>
        </Col>
        <Col span={4}>
          <Card hoverable onClick={() => navigate('/detection/configs')}>
            <Statistic title="检测配置" value={activeConfigs} prefix={<SettingOutlined />} />
          </Card>
        </Col>
        <Col span={4}>
          <Card hoverable onClick={() => navigate('/alarms')}>
            <Statistic
              title="未读告警"
              value={unreadCount}
              prefix={<BellOutlined />}
              valueStyle={{ color: '#cf1322' }}
            />
          </Card>
        </Col>
      </Row>

      <Card title="最近告警">
        <Table
          columns={alarmColumns}
          dataSource={alarms}
          rowKey="id"
          size="small"
          pagination={false}
          locale={{ emptyText: '暂无告警' }}
        />
      </Card>
    </div>
  );
}
