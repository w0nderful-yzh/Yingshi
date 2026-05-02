import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Card, Select, DatePicker, Button, Table, Space, message } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { getDevices } from '@/api/device';
import { getCloudRecords, getCloudPlaybackUrl } from '@/api/video';
import type { DeviceVO, CloudRecordFileVO, CloudPlaybackUrlVO } from '@/types';
import { VideoProtocol, VideoQuality } from '@/utils/constants';
import VideoPlayer from '@/components/VideoPlayer/VideoPlayer';

const { RangePicker } = DatePicker;

export default function PlaybackPage() {
  const [searchParams] = useSearchParams();
  const initialDeviceId = searchParams.get('deviceId');
  const [devices, setDevices] = useState<DeviceVO[]>([]);
  const [deviceId, setDeviceId] = useState<number | undefined>(initialDeviceId ? Number(initialDeviceId) : undefined);
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);
  const [records, setRecords] = useState<CloudRecordFileVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [playbackData, setPlaybackData] = useState<CloudPlaybackUrlVO | null>(null);
  const [playbackLoading, setPlaybackLoading] = useState(false);

  useEffect(() => {
    getDevices().then(setDevices).catch((err) => message.error(err.message));
  }, []);

  const handleSearch = async () => {
    if (!deviceId) {
      message.warning('请选择设备');
      return;
    }
    if (!dateRange) {
      message.warning('请选择时间范围');
      return;
    }
    setLoading(true);
    try {
      const startTime = dateRange[0].format('YYYY-MM-DD HH:mm:ss');
      const endTime = dateRange[1].format('YYYY-MM-DD HH:mm:ss');
      const data = await getCloudRecords({ deviceId, startTime, endTime });
      setRecords(data);
    } catch (err: any) {
      message.error(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handlePlay = async (record: CloudRecordFileVO) => {
    if (!deviceId) return;
    setPlaybackLoading(true);
    try {
      const data = await getCloudPlaybackUrl({
        deviceId,
        startTime: record.startTime,
        endTime: record.endTime,
        protocol: VideoProtocol.FLV,
        quality: VideoQuality.SMOOTH,
      });
      setPlaybackData(data);
    } catch (err: any) {
      message.error(err.message);
    } finally {
      setPlaybackLoading(false);
    }
  };

  const columns: ColumnsType<CloudRecordFileVO> = [
    { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 180 },
    { title: '结束时间', dataIndex: 'endTime', key: 'endTime', width: 180 },
    { title: '录制类型', dataIndex: 'recordType', key: 'recordType', width: 100 },
    { title: '文件类型', dataIndex: 'fileType', key: 'fileType', width: 100 },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: unknown, record: CloudRecordFileVO) => (
        <Button type="link" size="small" onClick={() => handlePlay(record)} loading={playbackLoading}>
          播放
        </Button>
      ),
    },
  ];

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">视频回放</h2>

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
          <RangePicker
            showTime
            value={dateRange}
            onChange={(dates) => setDateRange(dates as [dayjs.Dayjs, dayjs.Dayjs] | null)}
          />
          <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch} loading={loading}>
            查询
          </Button>
        </Space>
      </Card>

      <div className="flex gap-4">
        <div className="flex-1">
          <Card title="录像列表">
            <Table
              columns={columns}
              dataSource={records}
              rowKey={(_, i) => String(i)}
              size="small"
              loading={loading}
              locale={{ emptyText: '请选择设备和时间范围查询录像' }}
            />
          </Card>
        </div>

        <Card title="播放器" style={{ width: 520 }}>
          {playbackData?.url ? (
            <VideoPlayer url={playbackData.url} autoPlay controls />
          ) : (
            <div className="flex items-center justify-center h-64 bg-gray-100 text-gray-400">
              选择录像开始播放
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
