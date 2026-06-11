import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, message, Progress, Tag } from 'antd';
import {
  ArrowRightOutlined,
  BellOutlined,
  CameraOutlined,
  CheckCircleFilled,
  ClockCircleOutlined,
  ExclamationCircleFilled,
  HeartOutlined,
  PlusOutlined,
  SafetyCertificateOutlined,
  ScanOutlined,
  SettingOutlined,
  SyncOutlined,
  VideoCameraOutlined,
} from '@ant-design/icons';
import { getDevices, syncDevices } from '@/api/device';
import { getPets } from '@/api/pet';
import { getDetectionConfigs } from '@/api/petDetection';
import { getAlarms, syncAlarms } from '@/api/alarm';
import { useAlarmStore } from '@/store/alarmStore';
import { useAuthStore } from '@/store/authStore';
import type { AlarmMessageVO, DeviceVO, PetDetectionConfigVO, PetVO } from '@/types';
import { AlarmTypeMap } from '@/utils/constants';
import { formatDateShort } from '@/utils/format';
import { canWriteRole } from '@/utils/permission';
import PageLoading from '@/components/PageLoading';

function alarmTone(type: string) {
  if (type === 'PET_OUT_OF_ZONE') return 'critical';
  if (type === 'PET_ABSENT') return 'warning';
  return 'notice';
}

export default function DashboardPage() {
  const navigate = useNavigate();
  const unreadCount = useAlarmStore((state) => state.unreadCount);
  const fetchUnreadCount = useAlarmStore((state) => state.fetchUnreadCount);
  const role = useAuthStore((state) => state.user?.role);
  const user = useAuthStore((state) => state.user);
  const [devices, setDevices] = useState<DeviceVO[]>([]);
  const [pets, setPets] = useState<PetVO[]>([]);
  const [configs, setConfigs] = useState<PetDetectionConfigVO[]>([]);
  const [alarms, setAlarms] = useState<AlarmMessageVO[]>([]);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState<'devices' | 'alarms' | null>(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [deviceList, petList, configList, alarmList] = await Promise.all([
        getDevices().catch(() => []),
        getPets().catch(() => []),
        getDetectionConfigs().catch(() => []),
        getAlarms({ readStatus: 0 }).catch(() => []),
      ]);
      setDevices(deviceList);
      setPets(petList);
      setConfigs(configList);
      setAlarms(alarmList.slice(0, 6));
      fetchUnreadCount();
    } finally {
      setLoading(false);
    }
  }, [fetchUnreadCount]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleSync = async (type: 'devices' | 'alarms') => {
    setSyncing(type);
    try {
      const result = type === 'devices' ? await syncDevices() : await syncAlarms();
      message.success(result.message || '同步完成');
      await fetchData();
    } catch (error: any) {
      message.error(error.message);
    } finally {
      setSyncing(null);
    }
  };

  const onlineDevices = devices.filter((device) => device.status === 'ONLINE');
  const offlineDevices = devices.filter((device) => device.status !== 'ONLINE');
  const activeConfigs = configs.filter((config) => config.enabled === 1);
  const configuredZones = configs.filter((config) => config.safeZones?.length > 0);
  const coverage = pets.length ? Math.round((activeConfigs.length / pets.length) * 100) : 0;
  const canWrite = canWriteRole(role);

  const health = useMemo(() => {
    if (unreadCount > 0) {
      return {
        tone: 'attention',
        title: `${unreadCount} 条告警待处理`,
        description: '建议先确认异常事件，再检查设备和检测任务。',
        icon: <ExclamationCircleFilled />,
      };
    }
    if (offlineDevices.length > 0) {
      return {
        tone: 'attention',
        title: `${offlineDevices.length} 台设备不可用`,
        description: '部分监控画面可能缺失，请检查设备连接状态。',
        icon: <ExclamationCircleFilled />,
      };
    }
    if (!activeConfigs.length) {
      return {
        tone: 'setup',
        title: '检测任务尚未启用',
        description: '完成宠物、摄像头与安全区域配置后即可开始监测。',
        icon: <SettingOutlined />,
      };
    }
    return {
      tone: 'healthy',
      title: '监测系统运行正常',
      description: `${activeConfigs.length} 个任务正在持续监测，当前没有未读告警。`,
      icon: <CheckCircleFilled />,
    };
  }, [activeConfigs.length, offlineDevices.length, unreadCount]);

  const nextSteps = [
    devices.length === 0 && {
      title: '接入第一台摄像头',
      description: '绑定萤石设备，建立实时画面来源。',
      action: '绑定设备',
      path: '/devices/bind',
    },
    pets.length === 0 && {
      title: '创建宠物档案',
      description: '检测任务需要关联一个明确的监测对象。',
      action: '添加宠物',
      path: '/pets',
    },
    configs.length === 0 && {
      title: '创建检测任务',
      description: '选择宠物与摄像头，设置异常判断阈值。',
      action: '新建任务',
      path: '/detection/configs/new',
    },
    configs.length > 0 && configuredZones.length < configs.length && {
      title: '补全安全区域',
      description: `${configs.length - configuredZones.length} 个任务尚未配置可活动范围。`,
      action: '去配置',
      path: `/detection/configs/${configs.find((config) => !config.safeZones?.length)?.id}/zones`,
    },
  ].filter(Boolean) as Array<{ title: string; description: string; action: string; path: string }>;

  if (loading) return <PageLoading />;

  return (
    <div className="ops-dashboard">
      <section className={`ops-health-hero is-${health.tone}`}>
        <div className="ops-health-hero__main">
          <div className="ops-health-hero__eyebrow">今日运行状态</div>
          <div className="ops-health-hero__title">
            <span>{health.icon}</span>
            <div>
              <h1>{health.title}</h1>
              <p>{health.description}</p>
            </div>
          </div>
          <div className="ops-health-hero__actions">
            {unreadCount > 0 ? (
              <Button type="primary" icon={<BellOutlined />} onClick={() => navigate('/alarms')}>
                处理告警
              </Button>
            ) : (
              <Button type="primary" icon={<VideoCameraOutlined />} onClick={() => navigate('/video/live')}>
                查看实时画面
              </Button>
            )}
            <Button icon={<ScanOutlined />} onClick={() => navigate('/detection/records')}>
              查看检测记录
            </Button>
          </div>
        </div>
        <div className="ops-health-hero__aside">
          <span>你好，{user?.nickname || user?.username || '管理员'}</span>
          <strong>{onlineDevices.length}/{devices.length || 0}</strong>
          <small>设备在线</small>
          <div className="ops-health-hero__pulse">
            <i />
            系统状态每 30 秒更新
          </div>
        </div>
      </section>

      <section className="ops-metric-grid">
        <button type="button" className="ops-metric-card" onClick={() => navigate('/devices')}>
          <span className="ops-metric-card__icon is-green"><CameraOutlined /></span>
          <span><small>在线设备</small><strong>{onlineDevices.length}</strong></span>
          <em>{offlineDevices.length ? `${offlineDevices.length} 台需检查` : '全部在线'}</em>
        </button>
        <button type="button" className="ops-metric-card" onClick={() => navigate('/detection/configs')}>
          <span className="ops-metric-card__icon is-blue"><SafetyCertificateOutlined /></span>
          <span><small>运行任务</small><strong>{activeConfigs.length}</strong></span>
          <em>{configs.length - activeConfigs.length} 个未启用</em>
        </button>
        <button type="button" className="ops-metric-card" onClick={() => navigate('/pets')}>
          <span className="ops-metric-card__icon is-amber"><HeartOutlined /></span>
          <span><small>监测对象</small><strong>{pets.length}</strong></span>
          <em>覆盖率 {Math.min(coverage, 100)}%</em>
        </button>
        <button type="button" className="ops-metric-card" onClick={() => navigate('/alarms')}>
          <span className="ops-metric-card__icon is-red"><BellOutlined /></span>
          <span><small>未读告警</small><strong>{unreadCount}</strong></span>
          <em>{unreadCount ? '需要处理' : '当前清空'}</em>
        </button>
      </section>

      <div className="ops-dashboard__grid">
        <section className="ops-panel ops-panel--alarms">
          <div className="ops-panel__header">
            <div>
              <span className="ops-panel__eyebrow">ATTENTION</span>
              <h2>待处理事件</h2>
            </div>
            <Button type="link" onClick={() => navigate('/alarms')}>
              查看全部 <ArrowRightOutlined />
            </Button>
          </div>
          {alarms.length ? (
            <div className="ops-event-list">
              {alarms.map((alarm) => (
                <button
                  type="button"
                  key={alarm.id}
                  className={`ops-event-item is-${alarmTone(alarm.alarmType)}`}
                  onClick={() => navigate('/alarms')}
                >
                  <span className="ops-event-item__marker" />
                  <span className="ops-event-item__body">
                    <span>
                      <strong>{AlarmTypeMap[alarm.alarmType] || alarm.alarmName || '异常事件'}</strong>
                      <Tag bordered={false}>{alarm.deviceName || alarm.deviceSerial}</Tag>
                    </span>
                    <p>{alarm.alarmContent || '检测到异常事件，请查看详情。'}</p>
                  </span>
                  <time><ClockCircleOutlined /> {formatDateShort(alarm.alarmTime)}</time>
                </button>
              ))}
            </div>
          ) : (
            <div className="ops-empty-state">
              <CheckCircleFilled />
              <strong>没有待处理告警</strong>
              <span>新的异常事件会在这里集中展示。</span>
            </div>
          )}
        </section>

        <aside className="ops-dashboard__side">
          <section className="ops-panel">
            <div className="ops-panel__header">
              <div>
                <span className="ops-panel__eyebrow">COVERAGE</span>
                <h2>监测覆盖</h2>
              </div>
              <strong className="ops-panel__score">{Math.min(coverage, 100)}%</strong>
            </div>
            <Progress
              percent={Math.min(coverage, 100)}
              showInfo={false}
              strokeColor="#17875f"
              trailColor="#e7ece9"
            />
            <div className="ops-coverage-list">
              <span><i className="is-green" /> 已启用任务 <strong>{activeConfigs.length}</strong></span>
              <span><i className="is-blue" /> 已配置区域 <strong>{configuredZones.length}</strong></span>
              <span><i className="is-gray" /> 宠物档案 <strong>{pets.length}</strong></span>
            </div>
          </section>

          <section className="ops-panel">
            <div className="ops-panel__header">
              <div>
                <span className="ops-panel__eyebrow">NEXT STEP</span>
                <h2>{nextSteps.length ? '建议操作' : '快捷操作'}</h2>
              </div>
            </div>
            {nextSteps.length ? (
              <div className="ops-next-steps">
                {nextSteps.slice(0, 3).map((step, index) => (
                  <div className="ops-next-step" key={step.title}>
                    <span>{index + 1}</span>
                    <div><strong>{step.title}</strong><p>{step.description}</p></div>
                    <Button size="small" onClick={() => navigate(step.path)}>{step.action}</Button>
                  </div>
                ))}
              </div>
            ) : (
              <div className="ops-quick-actions">
                <button type="button" onClick={() => navigate('/detection/configs/new')}>
                  <PlusOutlined /><span>新建检测任务</span>
                </button>
                <button type="button" onClick={() => navigate('/video/playback')}>
                  <VideoCameraOutlined /><span>查看历史回放</span>
                </button>
              </div>
            )}
          </section>

          {canWrite && (
            <section className="ops-sync-strip">
              <div><SyncOutlined /><span><strong>数据同步</strong><small>手动刷新云端设备与告警</small></span></div>
              <div>
                <Button size="small" loading={syncing === 'devices'} onClick={() => handleSync('devices')}>设备</Button>
                <Button size="small" loading={syncing === 'alarms'} onClick={() => handleSync('alarms')}>告警</Button>
              </div>
            </section>
          )}
        </aside>
      </div>
    </div>
  );
}
