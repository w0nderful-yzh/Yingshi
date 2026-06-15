import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Dropdown, Empty, message, Modal, Switch, Tag } from 'antd';
import {
  DeleteOutlined,
  EditOutlined,
  ExperimentOutlined,
  MoreOutlined,
  PlusOutlined,
  RadarChartOutlined,
  SafetyCertificateOutlined,
  ScanOutlined,
  SettingOutlined,
  VideoCameraOutlined,
} from '@ant-design/icons';
import {
  deleteDetectionConfig,
  getDetectionConfigs,
  triggerAnalysis,
  triggerDetection,
  updateDetectionConfig,
} from '@/api/petDetection';
import { useAuthStore } from '@/store/authStore';
import type { PetDetectionConfigVO, PetDetectionResultVO } from '@/types';
import { canWriteRole } from '@/utils/permission';
import PetBoundingBox from '@/components/PetBoundingBox';

export default function DetectionConfigListPage() {
  const navigate = useNavigate();
  const role = useAuthStore((state) => state.user?.role);
  const [configs, setConfigs] = useState<PetDetectionConfigVO[]>([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<number>();
  const [detectResult, setDetectResult] = useState<PetDetectionResultVO | null>(null);
  const canWrite = canWriteRole(role);

  const fetchConfigs = async () => {
    setLoading(true);
    try {
      setConfigs(await getDetectionConfigs());
    } catch (error: any) {
      message.error(error.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchConfigs();
  }, []);

  const handleToggleEnabled = async (record: PetDetectionConfigVO) => {
    setBusyId(record.id);
    try {
      await updateDetectionConfig(record.id, {
        petId: record.petId,
        deviceId: record.deviceId,
        enabled: record.enabled === 0,
      });
      setConfigs((current) => current.map((config) =>
        config.id === record.id ? { ...config, enabled: record.enabled === 0 ? 1 : 0 } : config));
      message.success(record.enabled === 0 ? '检测任务已启用' : '检测任务已暂停');
    } catch (error: any) {
      message.error(error.message);
    } finally {
      setBusyId(undefined);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteDetectionConfig(id);
      setConfigs((current) => current.filter((config) => config.id !== id));
      message.success('检测任务已删除');
    } catch (error: any) {
      message.error(error.message);
    }
  };

  const confirmDelete = (id: number) => {
    Modal.confirm({
      title: '删除这个检测任务？',
      content: '相关安全区域也会一并删除，且无法恢复。',
      okText: '确认删除',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: () => handleDelete(id),
    });
  };

  const handleDetect = async (id: number) => {
    setBusyId(id);
    try {
      const result = await triggerDetection(id);
      setDetectResult(result);
    } catch (error: any) {
      message.error(error.message);
    } finally {
      setBusyId(undefined);
    }
  };

  const handleAnalyze = async (id: number) => {
    setBusyId(id);
    try {
      await triggerAnalysis(id);
      message.success('异常分析已完成，请到告警中心查看结果');
    } catch (error: any) {
      message.error(error.message);
    } finally {
      setBusyId(undefined);
    }
  };

  const activeCount = configs.filter((config) => config.enabled === 1).length;
  const zoneReadyCount = configs.filter((config) => config.safeZones?.length > 0).length;

  return (
    <div className="task-page">
      <section className="task-page__overview">
        <div>
          <span className="task-page__eyebrow">DETECTION TASKS</span>
          <h1>让每个监测任务都可理解、可操作</h1>
          <p>一个任务负责连接宠物、摄像头和异常规则。优先补全安全区域，再启用持续监测。</p>
        </div>
        {canWrite && (
          <Button type="primary" size="large" icon={<PlusOutlined />} onClick={() => navigate('/detection/configs/new')}>
            新建检测任务
          </Button>
        )}
      </section>

      <section className="task-page__stats">
        <div><span>任务总数</span><strong>{configs.length}</strong><small>全部监测关系</small></div>
        <div><span>正在运行</span><strong>{activeCount}</strong><small>{configs.length - activeCount} 个已暂停</small></div>
        <div><span>区域已配置</span><strong>{zoneReadyCount}</strong><small>{configs.length - zoneReadyCount} 个待完善</small></div>
      </section>

      {loading ? (
        <div className="task-page__loading">正在加载检测任务...</div>
      ) : configs.length ? (
        <section className="task-card-grid">
          {configs.map((config) => {
            const zoneReady = Boolean(config.safeZones?.length);
            const active = config.enabled === 1;
            return (
              <article className={`task-card ${active ? 'is-active' : 'is-paused'}`} key={config.id}>
                <header className="task-card__header">
                  <div className="task-card__identity">
                    <span className="task-card__pet-mark">
                      {config.petName?.trim()?.slice(0, 1) || '宠'}
                    </span>
                    <div>
                      <div>
                        <h2>{config.petName || '未命名宠物'}</h2>
                        <Tag color={active ? 'success' : 'default'} bordered={false}>
                          {active ? '运行中' : '已暂停'}
                        </Tag>
                      </div>
                      <p><VideoCameraOutlined /> {config.deviceName || config.deviceSerial}</p>
                    </div>
                  </div>
                  {canWrite && (
                    <Dropdown
                      placement="bottomRight"
                      menu={{
                        items: [
                          {
                            key: 'edit',
                            icon: <EditOutlined />,
                            label: '编辑阈值',
                            onClick: () => navigate(`/detection/configs/${config.id}/edit`),
                          },
                          {
                            key: 'analyze',
                            icon: <ExperimentOutlined />,
                            label: '执行异常分析',
                            onClick: () => handleAnalyze(config.id),
                          },
                          { type: 'divider' },
                          {
                            key: 'delete',
                            danger: true,
                            icon: <DeleteOutlined />,
                            label: '删除任务',
                            onClick: () => confirmDelete(config.id),
                          },
                        ],
                      }}
                    >
                      <Button type="text" icon={<MoreOutlined />} aria-label="更多任务操作" />
                    </Dropdown>
                  )}
                </header>

                <div className="task-card__status-row">
                  <div className={zoneReady ? 'is-ready' : 'is-warning'}>
                    <SafetyCertificateOutlined />
                    <span>
                      <small>安全区域</small>
                      <strong>{zoneReady ? `${config.safeZones.length} 个区域` : '尚未配置'}</strong>
                    </span>
                  </div>
                  <div>
                    <RadarChartOutlined />
                    <span>
                      <small>告警冷却</small>
                      <strong>{Math.max(1, Math.round(config.cooldownSeconds / 60))} 分钟</strong>
                    </span>
                  </div>
                </div>

                <div className="task-card__rules">
                  <span><small>消失</small><strong>{config.petAbsentMinutes} 分钟</strong></span>
                  <span><small>静止</small><strong>{config.stillnessMinutes} 分钟</strong></span>
                  <span><small>活动</small><strong>{config.activityCountThreshold} 次 / {config.activityWindowMinutes} 分</strong></span>
                </div>

                {!zoneReady && (
                  <div className="task-card__notice">
                    <SettingOutlined />
                    未配置安全区域时，系统无法判断宠物是否离区。
                  </div>
                )}

                <footer className="task-card__actions">
                  <Button
                    type={zoneReady ? 'default' : 'primary'}
                    icon={<SafetyCertificateOutlined />}
                    onClick={() => navigate(`/detection/configs/${config.id}/zones`)}
                  >
                    {zoneReady ? '调整安全区域' : '配置安全区域'}
                  </Button>
                  <Button
                    icon={<ScanOutlined />}
                    loading={busyId === config.id}
                    onClick={() => handleDetect(config.id)}
                  >
                    立即检测
                  </Button>
                  {canWrite && (
                    <label className="task-card__switch">
                      <span>{active ? '持续监测' : '已暂停'}</span>
                      <Switch
                        size="small"
                        checked={active}
                        loading={busyId === config.id}
                        onChange={() => handleToggleEnabled(config)}
                      />
                    </label>
                  )}
                </footer>
              </article>
            );
          })}
        </section>
      ) : (
        <section className="task-page__empty">
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description="还没有检测任务"
          >
            {canWrite && (
              <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/detection/configs/new')}>
                创建第一个任务
              </Button>
            )}
          </Empty>
        </section>
      )}

      <Modal
        open={Boolean(detectResult)}
        onCancel={() => setDetectResult(null)}
        footer={[
          <Button key="records" onClick={() => navigate('/detection/records')}>查看检测记录</Button>,
          <Button key="close" type="primary" onClick={() => setDetectResult(null)}>完成</Button>,
        ]}
        title="即时检测结果"
        width={680}
      >
        {detectResult && (
          <div className={`task-result ${detectResult.inSafeZone ? 'is-safe' : 'is-risk'}`}>
            <div className="task-result__summary">
              <span>{detectResult.inSafeZone ? <SafetyCertificateOutlined /> : <RadarChartOutlined />}</span>
              <div>
                <h3>{detectResult.inSafeZone ? '宠物处于安全区域' : '检测到区域外活动'}</h3>
                <p>{detectResult.message}</p>
              </div>
            </div>
            <div className="task-result__meta">
              <span><small>宠物</small><strong>{detectResult.petName}</strong></span>
              <span><small>设备</small><strong>{detectResult.deviceName}</strong></span>
              <span><small>告警</small><strong>{detectResult.alarmTriggered ? '已触发' : '未触发'}</strong></span>
            </div>
            {detectResult.snapshotUrl && (
              <div className="text-center">
                <PetBoundingBox
                  petCoordX={detectResult.petCoordX}
                  petCoordY={detectResult.petCoordY}
                  petWidth={detectResult.petWidth}
                  petHeight={detectResult.petHeight}
                  petName={detectResult.petName}
                  imageUrl={detectResult.snapshotUrl}
                  safeZones={detectResult.safeZones}
                />
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}
