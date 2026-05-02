import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Table, Button, Switch, Space, Popconfirm, message, Tag } from 'antd';
import { PlusOutlined, ExperimentOutlined, ScanOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import {
  getDetectionConfigs,
  deleteDetectionConfig,
  updateDetectionConfig,
  triggerDetection,
  triggerAnalysis,
} from '@/api/petDetection';
import type { PetDetectionConfigVO, PetDetectionResultVO } from '@/types';

export default function DetectionConfigListPage() {
  const navigate = useNavigate();
  const [configs, setConfigs] = useState<PetDetectionConfigVO[]>([]);
  const [loading, setLoading] = useState(true);
  const [detectResult, setDetectResult] = useState<PetDetectionResultVO | null>(null);

  const fetchConfigs = async () => {
    setLoading(true);
    try {
      const data = await getDetectionConfigs();
      setConfigs(data);
    } catch (err: any) {
      message.error(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchConfigs();
  }, []);

  const handleToggleEnabled = async (record: PetDetectionConfigVO) => {
    try {
      await updateDetectionConfig(record.id, {
        petId: record.petId,
        deviceId: record.deviceId,
        enabled: record.enabled === 0,
      });
      message.success(record.enabled === 0 ? '已启用' : '已禁用');
      fetchConfigs();
    } catch (err: any) {
      message.error(err.message);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteDetectionConfig(id);
      message.success('删除成功');
      fetchConfigs();
    } catch (err: any) {
      message.error(err.message);
    }
  };

  const handleDetect = async (id: number) => {
    try {
      const result = await triggerDetection(id);
      setDetectResult(result);
      message.success(result.message || '检测完成');
    } catch (err: any) {
      message.error(err.message);
    }
  };

  const handleAnalyze = async (id: number) => {
    try {
      await triggerAnalysis(id);
      message.success('异常分析已触发');
    } catch (err: any) {
      message.error(err.message);
    }
  };

  const columns: ColumnsType<PetDetectionConfigVO> = [
    { title: '宠物', dataIndex: 'petName', key: 'petName', width: 100 },
    { title: '设备', dataIndex: 'deviceName', key: 'deviceName', width: 150 },
    {
      title: '启用',
      dataIndex: 'enabled',
      key: 'enabled',
      width: 80,
      render: (v: number, record) => (
        <Switch checked={v === 1} size="small" onChange={() => handleToggleEnabled(record)} />
      ),
    },
    { title: '冷却(秒)', dataIndex: 'cooldownSeconds', key: 'cooldownSeconds', width: 90 },
    { title: '消失阈值(分)', dataIndex: 'petAbsentMinutes', key: 'petAbsentMinutes', width: 110 },
    { title: '活动窗口(分)', dataIndex: 'activityWindowMinutes', key: 'activityWindowMinutes', width: 110 },
    { title: '活动阈值', dataIndex: 'activityCountThreshold', key: 'activityCountThreshold', width: 90 },
    { title: '静止阈值(分)', dataIndex: 'stillnessMinutes', key: 'stillnessMinutes', width: 110 },
    {
      title: '安全区域',
      key: 'safeZones',
      width: 100,
      render: (_: unknown, record) => (
        <Tag color="blue">{record.safeZones?.length || 0} 个</Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 280,
      render: (_: unknown, record) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => navigate(`/detection/configs/${record.id}/edit`)}>
            编辑
          </Button>
          <Button type="link" size="small" onClick={() => navigate(`/detection/configs/${record.id}/zones`)}>
            区域配置
          </Button>
          <Button type="link" size="small" icon={<ScanOutlined />} onClick={() => handleDetect(record.id)}>
            检测
          </Button>
          <Button type="link" size="small" icon={<ExperimentOutlined />} onClick={() => handleAnalyze(record.id)}>
            分析
          </Button>
          <Popconfirm title="确认删除此配置？" onConfirm={() => handleDelete(record.id)}>
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
        <h2 className="text-xl font-semibold m-0">检测配置</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/detection/configs/new')}>
          新建配置
        </Button>
      </div>

      <Table columns={columns} dataSource={configs} rowKey="id" loading={loading} scroll={{ x: 1200 }} />

      {detectResult && (
        <div className="mt-4 p-4 bg-green-50 border border-green-200 rounded">
          <h4>检测结果</h4>
          <p>宠物: {detectResult.petName} | 设备: {detectResult.deviceName}</p>
          <p>时间: {detectResult.detectTime}</p>
          <p>
            安全区域内: {detectResult.inSafeZone ? '是' : '否'} | 告警触发: {detectResult.alarmTriggered ? '是' : '否'}
          </p>
          <p>{detectResult.message}</p>
          {detectResult.snapshotUrl && (
            <img src={detectResult.snapshotUrl} alt="检测快照" style={{ maxWidth: 400, marginTop: 8 }} />
          )}
        </div>
      )}
    </div>
  );
}
