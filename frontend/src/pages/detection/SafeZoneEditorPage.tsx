import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Alert, Button, Input, message, Popconfirm, Tag } from 'antd';
import {
  ArrowLeftOutlined,
  CheckCircleFilled,
  DeleteOutlined,
  InfoCircleOutlined,
  SaveOutlined,
  VideoCameraOutlined,
} from '@ant-design/icons';
import {
  createSafeZone,
  deleteSafeZone,
  getDetectionConfigById,
  getSafeZones,
  updateSafeZone,
} from '@/api/petDetection';
import { getLiveUrl } from '@/api/video';
import type { PetDetectionConfigVO, PetSafeZoneVO } from '@/types';
import VideoPlayer from '@/components/VideoPlayer/VideoPlayer';
import SafeZoneCanvas, {
  type EditorMode,
  type ZoneData,
} from '@/components/SafeZoneEditor/SafeZoneCanvas';
import ZoneToolbar from '@/components/SafeZoneEditor/ZoneToolbar';
import ZoneList from '@/components/SafeZoneEditor/ZoneList';
import PageLoading from '@/components/PageLoading';

function toZoneData(zone: PetSafeZoneVO): ZoneData {
  return {
    key: `saved-${zone.id}`,
    id: zone.id,
    zoneName: zone.zoneName,
    zoneType: zone.zoneType,
    rectLeft: zone.rectLeft ?? undefined,
    rectTop: zone.rectTop ?? undefined,
    rectRight: zone.rectRight ?? undefined,
    rectBottom: zone.rectBottom ?? undefined,
    polygonPoints: zone.polygonPoints ?? undefined,
  };
}

function comparableZones(zones: ZoneData[]) {
  return zones.map(({ key: _key, ...zone }) => zone);
}

export default function SafeZoneEditorPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [config, setConfig] = useState<PetDetectionConfigVO | null>(null);
  const [originalZones, setOriginalZones] = useState<ZoneData[]>([]);
  const [zones, setZones] = useState<ZoneData[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [mode, setMode] = useState<EditorMode>('SELECT');
  const [selectedZoneKey, setSelectedZoneKey] = useState<string>();
  const [liveUrl, setLiveUrl] = useState('');

  const fetchData = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    try {
      const [nextConfig, savedZones] = await Promise.all([
        getDetectionConfigById(Number(id)),
        getSafeZones(Number(id)),
      ]);
      const editableZones = savedZones.map(toZoneData);
      setConfig(nextConfig);
      setOriginalZones(editableZones);
      setZones(editableZones);
      setSelectedZoneKey(editableZones[0]?.key);

      try {
        const liveData = await getLiveUrl({ deviceId: nextConfig.deviceId });
        setLiveUrl(liveData.url);
      } catch {
        setLiveUrl('');
      }
    } catch (error: any) {
      message.error(error.message);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const dirty = useMemo(
    () => JSON.stringify(comparableZones(zones)) !== JSON.stringify(comparableZones(originalZones)),
    [originalZones, zones],
  );
  const selectedZone = zones.find((zone) => zone.key === selectedZoneKey);

  const updateZone = (key: string, nextZone: ZoneData) => {
    setZones((current) => current.map((zone) => zone.key === key ? nextZone : zone));
  };

  const handleZoneComplete = (zone: ZoneData) => {
    const nextZone = {
      ...zone,
      zoneName: `安全区域 ${zones.length + 1}`,
    };
    setZones((current) => [...current, nextZone]);
    setSelectedZoneKey(nextZone.key);
    setMode('SELECT');
  };

  const handleDelete = (key: string) => {
    setZones((current) => current.filter((zone) => zone.key !== key));
    if (selectedZoneKey === key) {
      const remaining = zones.filter((zone) => zone.key !== key);
      setSelectedZoneKey(remaining[0]?.key);
    }
  };

  const handleReset = () => {
    setZones(originalZones);
    setSelectedZoneKey(originalZones[0]?.key);
    setMode('SELECT');
  };

  const handleSave = async () => {
    if (!id) return;
    setSaving(true);
    try {
      const deletedIds = originalZones
        .filter((original) => original.id && !zones.some((zone) => zone.id === original.id))
        .map((zone) => zone.id!);

      await Promise.all(deletedIds.map(deleteSafeZone));
      await Promise.all(zones.map((zone) => {
        const payload = {
          detectionConfigId: Number(id),
          zoneName: zone.zoneName.trim() || '未命名区域',
          zoneType: zone.zoneType,
          rectLeft: zone.rectLeft,
          rectTop: zone.rectTop,
          rectRight: zone.rectRight,
          rectBottom: zone.rectBottom,
          polygonPoints: zone.polygonPoints,
        };
        return zone.id ? updateSafeZone(zone.id, payload) : createSafeZone(payload);
      }));

      message.success('安全区域已生效');
      await fetchData();
    } catch (error: any) {
      message.error(error.message);
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <PageLoading />;
  if (!config) return <Alert type="error" showIcon message="检测配置不存在" />;

  return (
    <div className="safe-zone-page">
      <header className="safe-zone-page__header">
        <div>
          <Button
            type="text"
            icon={<ArrowLeftOutlined />}
            className="safe-zone-page__back"
            onClick={() => navigate('/detection/configs')}
          >
            返回检测配置
          </Button>
          <div className="safe-zone-page__title-row">
            <div>
              <div className="safe-zone-page__eyebrow">BOUNDARY STUDIO</div>
              <h1>安全区域</h1>
            </div>
            <Tag color={config.enabled === 1 ? 'success' : 'default'}>
              {config.enabled === 1 ? '检测已启用' : '检测未启用'}
            </Tag>
          </div>
          <p>在摄像头画面上标出宠物可以活动的范围，多个区域之间按“任一区域内即安全”处理。</p>
        </div>
        <Button
          type="primary"
          size="large"
          icon={<SaveOutlined />}
          loading={saving}
          disabled={!dirty}
          onClick={handleSave}
        >
          保存并应用{dirty ? ` (${zones.length})` : ''}
        </Button>
      </header>

      <section className="safe-zone-summary">
        <div>
          <span>监控对象</span>
          <strong>{config.petName}</strong>
        </div>
        <div>
          <span>参考设备</span>
          <strong><VideoCameraOutlined /> {config.deviceName}</strong>
        </div>
        <div>
          <span>有效区域</span>
          <strong>{zones.length} 个</strong>
        </div>
        <div className={dirty ? 'is-warning' : 'is-ready'}>
          <span>配置状态</span>
          <strong>{dirty ? '有未保存修改' : <><CheckCircleFilled /> 已同步</>}</strong>
        </div>
      </section>

      <div className="safe-zone-workspace">
        <main className="safe-zone-editor">
          <ZoneToolbar
            mode={mode}
            dirty={dirty}
            onModeChange={setMode}
            onReset={handleReset}
          />

          <div className="safe-zone-stage">
            {liveUrl && (
              <VideoPlayer
                url={liveUrl}
                autoPlay
                controls={false}
                className="safe-zone-video"
              />
            )}
            <SafeZoneCanvas
              zones={zones}
              mode={mode}
              hasVideo={Boolean(liveUrl)}
              selectedZoneKey={selectedZoneKey}
              onZoneComplete={handleZoneComplete}
              onZoneChange={updateZone}
              onSelectZone={setSelectedZoneKey}
            />
            <div className="safe-zone-stage__live">
              <span /> {liveUrl ? '实时参考画面' : '无视频参考'}
            </div>
            <div className="safe-zone-stage__anchor">
              判定点：宠物检测框底部中心
            </div>
          </div>
        </main>

        <aside className="safe-zone-inspector">
          <div className="safe-zone-inspector__section">
            <div className="safe-zone-inspector__heading">
              <div>
                <span>区域清单</span>
                <strong>{zones.length}</strong>
              </div>
              <small>点击条目定位画面区域</small>
            </div>
            <ZoneList
              zones={zones}
              selectedKey={selectedZoneKey}
              onSelect={(key) => {
                setSelectedZoneKey(key);
                setMode('SELECT');
              }}
              onDelete={handleDelete}
            />
          </div>

          {selectedZone && (
            <div className="safe-zone-inspector__section safe-zone-properties">
              <div className="safe-zone-inspector__heading">
                <div><span>所选区域</span></div>
                <Popconfirm
                  title="删除这个安全区域？"
                  description="保存前仍可通过“放弃修改”恢复。"
                  onConfirm={() => handleDelete(selectedZone.key)}
                >
                  <Button type="text" danger icon={<DeleteOutlined />} />
                </Popconfirm>
              </div>
              <label>
                区域名称
                <Input
                  value={selectedZone.zoneName}
                  maxLength={30}
                  onChange={(event) => updateZone(selectedZone.key, {
                    ...selectedZone,
                    zoneName: event.target.value,
                  })}
                />
              </label>
              <div className="safe-zone-properties__meta">
                <span>形状</span>
                <strong>{selectedZone.zoneType === 'RECTANGLE' ? '矩形区域' : '多边形区域'}</strong>
              </div>
              <div className="safe-zone-properties__meta">
                <span>状态</span>
                <strong>{selectedZone.id ? '已保存，修改后待应用' : '新建，尚未应用'}</strong>
              </div>
            </div>
          )}

          <div className="safe-zone-rule-card">
            <div className="safe-zone-rule-card__icon"><InfoCircleOutlined /></div>
            <div>
              <strong>离区报警如何判定</strong>
              <p>系统取宠物检测框的底部中心点作为落地点。落地点连续 2 次不在任何安全区域内，才确认离区。</p>
              <div className="safe-zone-rule-card__steps">
                <span><i>1</i> 首次越界：记录观察</span>
                <span><i>2</i> 再次越界：触发一次报警</span>
                <span><i>3</i> 回到区域：重新布防</span>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
}
