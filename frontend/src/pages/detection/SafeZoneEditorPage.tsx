import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, Button, Space, message, Descriptions, Tag } from 'antd';
import { SaveOutlined, ArrowLeftOutlined } from '@ant-design/icons';
import { getDetectionConfigById, getSafeZones, createSafeZone, updateSafeZone, deleteSafeZone } from '@/api/petDetection';
import { getLiveUrl } from '@/api/video';
import type { PetDetectionConfigVO, PetSafeZoneVO } from '@/types';
import VideoPlayer from '@/components/VideoPlayer/VideoPlayer';
import SafeZoneCanvas, { type ZoneData } from '@/components/SafeZoneEditor/SafeZoneCanvas';
import ZoneToolbar from '@/components/SafeZoneEditor/ZoneToolbar';
import ZoneList from '@/components/SafeZoneEditor/ZoneList';
import PageLoading from '@/components/PageLoading';

export default function SafeZoneEditorPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [config, setConfig] = useState<PetDetectionConfigVO | null>(null);
  const [existingZones, setExistingZones] = useState<PetSafeZoneVO[]>([]);
  const [newZones, setNewZones] = useState<ZoneData[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [drawingMode, setDrawingMode] = useState<'RECTANGLE' | 'POLYGON'>('RECTANGLE');
  const [zoneName, setZoneName] = useState('');
  const [selectedZoneIdx, setSelectedZoneIdx] = useState<number | undefined>();
  const [liveUrl, setLiveUrl] = useState<string>('');

  const fetchData = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    try {
      const [cfg, zones] = await Promise.all([
        getDetectionConfigById(Number(id)),
        getSafeZones(Number(id)),
      ]);
      setConfig(cfg);
      setExistingZones(zones);

      // Fetch live URL for the device
      try {
        const liveData = await getLiveUrl({ deviceId: cfg.deviceId });
        setLiveUrl(liveData.url);
      } catch {
        // Live URL not available, canvas will show without video
      }
    } catch (err: any) {
      message.error(err.message);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // Convert existing zones to ZoneData for canvas rendering
  const allCanvasZones: ZoneData[] = [
    ...existingZones.map((z, i) => ({
      id: z.id,
      zoneName: z.zoneName,
      zoneType: z.zoneType,
      rectLeft: z.rectLeft,
      rectTop: z.rectTop,
      rectRight: z.rectRight,
      rectBottom: z.rectBottom,
      polygonPoints: z.polygonPoints,
    })),
    ...newZones,
  ];

  const handleZoneComplete = (zone: ZoneData) => {
    const name = zoneName || `区域${existingZones.length + newZones.length + 1}`;
    setNewZones((prev) => [...prev, { ...zone, zoneName: name }]);
    setZoneName('');
  };

  const handleDeleteExisting = async (index: number) => {
    const zone = existingZones[index];
    try {
      await deleteSafeZone(zone.id);
      message.success('删除成功');
      setExistingZones((prev) => prev.filter((_, i) => i !== index));
    } catch (err: any) {
      message.error(err.message);
    }
  };

  const handleDeleteNew = (index: number) => {
    setNewZones((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSave = async () => {
    if (!id) return;
    setSaving(true);
    try {
      for (const zone of newZones) {
        await createSafeZone({
          detectionConfigId: Number(id),
          zoneName: zone.zoneName,
          zoneType: zone.zoneType,
          rectLeft: zone.rectLeft,
          rectTop: zone.rectTop,
          rectRight: zone.rectRight,
          rectBottom: zone.rectBottom,
          polygonPoints: zone.polygonPoints,
        });
      }
      message.success('保存成功');
      setNewZones([]);
      fetchData();
    } catch (err: any) {
      message.error(err.message);
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <PageLoading />;
  if (!config) return <div>配置不存在</div>;

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <Space>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/detection/configs')}>
            返回
          </Button>
          <h2 className="text-xl font-semibold m-0">安全区域配置</h2>
        </Space>
        <Button type="primary" icon={<SaveOutlined />} onClick={handleSave} loading={saving} disabled={newZones.length === 0}>
          保存新区域 ({newZones.length})
        </Button>
      </div>

      {config && (
        <Card size="small" className="mb-4">
          <Descriptions size="small" column={4}>
            <Descriptions.Item label="宠物">{config.petName}</Descriptions.Item>
            <Descriptions.Item label="设备">{config.deviceName}</Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag color={config.enabled === 1 ? 'green' : 'default'}>
                {config.enabled === 1 ? '已启用' : '已禁用'}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="现有区域">{existingZones.length} 个</Descriptions.Item>
          </Descriptions>
        </Card>
      )}

      <div className="flex gap-4">
        <div className="flex-1">
          <Card size="small">
            {liveUrl ? (
              <div className="relative">
                <VideoPlayer url={liveUrl} autoPlay controls />
                <div className="mt-2 text-xs text-gray-400">
                  提示: 在下方画布上绘制安全区域。矩形模式：拖拽绘制；多边形模式：点击添加顶点，双击闭合。
                </div>
              </div>
            ) : (
              <div className="text-center text-gray-400 py-4">无法获取视频流，使用空白画布绘制</div>
            )}
          </Card>

          <Card size="small" className="mt-2">
            <ZoneToolbar
              drawingMode={drawingMode}
              onModeChange={setDrawingMode}
              onUndo={() => {
                if (newZones.length > 0) {
                  setNewZones((prev) => prev.slice(0, -1));
                }
              }}
              onClearAll={() => setNewZones([])}
              zoneName={zoneName}
              onZoneNameChange={setZoneName}
            />
            <SafeZoneCanvas
              zones={allCanvasZones}
              drawingMode={drawingMode}
              onZoneComplete={handleZoneComplete}
              selectedZoneId={selectedZoneIdx !== undefined ? allCanvasZones[selectedZoneIdx]?.id : undefined}
              onSelectZone={(zoneId) => {
                const idx = allCanvasZones.findIndex((z) => z.id === zoneId);
                setSelectedZoneIdx(idx >= 0 ? idx : undefined);
              }}
            />
          </Card>
        </div>

        <Card title="区域列表" size="small" style={{ width: 280 }}>
          {existingZones.length > 0 && (
            <>
              <div className="text-xs text-gray-500 mb-2">已保存的区域</div>
              <ZoneList
                zones={existingZones.map((z) => ({
                  id: z.id,
                  zoneName: z.zoneName,
                  zoneType: z.zoneType,
                  rectLeft: z.rectLeft,
                  rectTop: z.rectTop,
                  rectRight: z.rectRight,
                  rectBottom: z.rectBottom,
                  polygonPoints: z.polygonPoints,
                }))}
                onDelete={handleDeleteExisting}
                onSelect={setSelectedZoneIdx}
                selectedIndex={selectedZoneIdx}
              />
            </>
          )}
          {newZones.length > 0 && (
            <>
              <div className="text-xs text-gray-500 mt-3 mb-2">新增的区域（未保存）</div>
              <ZoneList
                zones={newZones}
                onDelete={handleDeleteNew}
                onSelect={(i) => setSelectedZoneIdx(existingZones.length + i)}
                selectedIndex={selectedZoneIdx !== undefined ? selectedZoneIdx - existingZones.length : undefined}
              />
            </>
          )}
          {existingZones.length === 0 && newZones.length === 0 && (
            <div className="text-gray-400 text-sm py-4 text-center">暂无安全区域</div>
          )}
        </Card>
      </div>
    </div>
  );
}
