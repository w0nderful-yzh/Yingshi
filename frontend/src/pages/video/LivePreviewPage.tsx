import { useEffect, useState } from 'react';
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
  const [protocol, setProtocol] = useState(VideoProtocol.HLS);
  const [quality, setQuality] = useState(VideoQuality.SMOOTH);
  const [liveData, setLiveData] = useState<LiveUrlVO | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getDevices({ status: 'ONLINE' })
      .then(setDevices)
      .catch((err) => message.error(err.message));
  }, []);

  const fetchLiveUrl = async () => {
    if (!deviceId) {
      message.warning('请选择设备');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const data = await getLiveUrl({ deviceId, protocol, quality });
      setLiveData(data);
      console.log('获取直播地址成功:', data);
    } catch (err: any) {
      const errMsg = err.message || '获取直播地址失败';
      setError(errMsg);
      setLiveData(null);
      console.error('获取直播地址失败:', errMsg, err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (deviceId) fetchLiveUrl();
  }, [deviceId, protocol, quality]);

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
              { label: 'HLS', value: VideoProtocol.HLS },
              { label: 'FLV', value: VideoProtocol.FLV },
              { label: 'RTMP', value: VideoProtocol.RTMP },
            ]}
          />
          <Select
            value={quality}
            onChange={setQuality}
            style={{ width: 100 }}
            options={[
              { label: '流畅', value: VideoQuality.SMOOTH },
              { label: '高清', value: VideoQuality.HD },
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

      <div className="flex gap-4">
        <div className="flex-1">
          <Card>
            {liveData?.url ? (
              <VideoPlayer url={liveData.url} autoPlay controls />
            ) : (
              <div className="flex items-center justify-center h-80 bg-gray-100 text-gray-400">
                {loading ? '加载中...' : '请选择设备查看实时视频'}
              </div>
            )}
          </Card>
        </div>

        {selectedDevice && (
          <Card title="设备信息" style={{ width: 300 }}>
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
