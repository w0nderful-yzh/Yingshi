import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Card, Select, Space, Button, message, Descriptions, Alert } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { getDevices } from '@/api/device';
import { getLiveUrl } from '@/api/video';
import type { DeviceVO, LiveUrlVO } from '@/types';
import { VideoProtocol, VideoQuality } from '@/utils/constants';
import VideoPlayer from '@/components/VideoPlayer/VideoPlayer';

export default function LivePreviewPage() {
  const [searchParams] = useSearchParams();
  const initialDeviceId = searchParams.get('deviceId');
  const [devices, setDevices] = useState<DeviceVO[]>([]);
  const [deviceId, setDeviceId] = useState<number | undefined>(initialDeviceId ? Number(initialDeviceId) : undefined);
  const [protocol, setProtocol] = useState<number>(VideoProtocol.EZOPEN);
  const [quality, setQuality] = useState<number>(VideoQuality.HD);
  const [liveData, setLiveData] = useState<LiveUrlVO | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [playerError, setPlayerError] = useState<string | null>(null);

  useEffect(() => {
    getDevices({ status: 'ONLINE' })
      .then(setDevices)
      .catch((err) => message.error(err.message));
  }, []);

  const fetchLiveUrl = useCallback(async () => {
    if (!deviceId) {
      message.warning('请选择设备');
      return;
    }
    setLoading(true);
    setError(null);
    setPlayerError(null);
    try {
      const data = await getLiveUrl({ deviceId, protocol, quality });
      setLiveData(data);
    } catch (err: any) {
      const errMsg = err.message || '获取直播地址失败';
      setError(errMsg);
      setLiveData(null);
      console.error('获取直播地址失败:', errMsg, err);
    } finally {
      setLoading(false);
    }
  }, [deviceId, protocol, quality]);

  useEffect(() => {
    if (deviceId) fetchLiveUrl();
  }, [deviceId, fetchLiveUrl]);

  const selectedDevice = devices.find((d) => d.id === deviceId);

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">实时监控</h2>

      <Card className="mb-4">
        <Space wrap>
          <Select
            placeholder="选择设备"
            value={deviceId}
            onChange={setDeviceId}
            style={{ width: 240 }}
            showSearch
            optionFilterProp="label"
            options={devices.map((d) => ({ label: `${d.deviceName} (${d.deviceSerial})`, value: d.id }))}
          />
          <Select
            value={protocol}
            onChange={setProtocol}
            style={{ width: 120 }}
            options={[
              { label: 'EZOPEN（H.265）', value: VideoProtocol.EZOPEN },
              { label: 'FLV（兼容）', value: VideoProtocol.FLV },
              { label: 'HLS', value: VideoProtocol.HLS },
            ]}
          />
          <Select
            value={quality}
            onChange={setQuality}
            style={{ width: 100 }}
            options={[
              { label: '高清', value: VideoQuality.HD },
              { label: '流畅', value: VideoQuality.SMOOTH },
            ]}
          />
          <Button icon={<ReloadOutlined />} onClick={fetchLiveUrl} loading={loading}>
            刷新
          </Button>
        </Space>
      </Card>

      {error && (
        <Alert
          type="error"
          message="获取直播地址失败"
          description={error}
          showIcon
          closable
          className="mb-4"
          onClose={() => setError(null)}
        />
      )}

      {protocol !== VideoProtocol.EZOPEN && (
        <Alert
          type="warning"
          message="当前协议仅适用于设备输出 H.264 的场景"
          description="这台设备当前输出 H.265；若画面提示“视频编码类型非 H264”，请切换到 EZOPEN（H.265）。"
          showIcon
          className="mb-4"
        />
      )}

      {playerError && (
        <Alert
          type="error"
          message="视频播放失败"
          description={playerError}
          showIcon
          closable
          className="mb-4"
          onClose={() => setPlayerError(null)}
        />
      )}

      <div className="flex flex-col gap-4 xl:flex-row">
        <div className="min-w-0 flex-1">
          <Card className="overflow-hidden">
            <div className="min-w-0 max-w-full overflow-hidden">
              {liveData?.url ? (
                <VideoPlayer
                  url={liveData.url}
                  accessToken={liveData.accessToken}
                  autoPlay
                  controls
                  onError={setPlayerError}
                />
              ) : (
                <div className="flex items-center justify-center h-80 bg-gray-100 text-gray-400">
                  {loading ? '加载中...' : '请选择设备查看实时视频'}
                </div>
              )}
            </div>
          </Card>
        </div>

        {selectedDevice && (
          <Card title="设备信息" className="w-full xl:w-[300px] xl:shrink-0">
            <Descriptions column={1} size="small">
              <Descriptions.Item label="名称">{selectedDevice.deviceName}</Descriptions.Item>
              <Descriptions.Item label="序列号">{selectedDevice.deviceSerial}</Descriptions.Item>
              <Descriptions.Item label="状态">{selectedDevice.status}</Descriptions.Item>
              <Descriptions.Item label="类型">{selectedDevice.deviceType || '-'}</Descriptions.Item>
            </Descriptions>
          </Card>
        )}
      </div>
    </div>
  );
}
