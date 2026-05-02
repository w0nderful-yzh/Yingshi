import { useEffect, useState } from 'react';
import { Card, Table, Select, DatePicker, Button, Space, Tag, Modal, message } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { getDetectionRecords } from '@/api/petDetection';
import { getDevices } from '@/api/device';
import { getPets } from '@/api/pet';
import { getDetectionConfigs } from '@/api/petDetection';
import type { PetDetectionRecordVO, DeviceVO, PetVO, PetDetectionConfigVO } from '@/types';
import { formatDate } from '@/utils/format';
import PetBoundingBox from '@/components/PetBoundingBox';

const { RangePicker } = DatePicker;

export default function DetectionRecordsPage() {
  const [records, setRecords] = useState<PetDetectionRecordVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [devices, setDevices] = useState<DeviceVO[]>([]);
  const [pets, setPets] = useState<PetVO[]>([]);
  const [configs, setConfigs] = useState<PetDetectionConfigVO[]>([]);
  const [filters, setFilters] = useState({
    detectionConfigId: undefined as number | undefined,
    petId: undefined as number | undefined,
    deviceId: undefined as number | undefined,
    alarmTriggered: undefined as number | undefined,
  });
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);
  const [detailRecord, setDetailRecord] = useState<PetDetectionRecordVO | null>(null);

  useEffect(() => {
    Promise.all([
      getDevices().catch(() => []),
      getPets().catch(() => []),
      getDetectionConfigs().catch(() => []),
    ]).then(([d, p, c]) => {
      setDevices(d);
      setPets(p);
      setConfigs(c);
    });
  }, []);

  const fetchRecords = async () => {
    setLoading(true);
    try {
      const params: any = { ...filters };
      if (dateRange) {
        params.startTime = dateRange[0].format('YYYY-MM-DD HH:mm:ss');
        params.endTime = dateRange[1].format('YYYY-MM-DD HH:mm:ss');
      }
      const data = await getDetectionRecords(params);
      setRecords(data);
    } catch (err: any) {
      message.error(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRecords();
  }, [filters]);

  const columns: ColumnsType<PetDetectionRecordVO> = [
    { title: '宠物', dataIndex: 'petName', key: 'petName', width: 100 },
    { title: '设备', dataIndex: 'deviceName', key: 'deviceName', width: 150 },
    {
      title: '检测时间',
      dataIndex: 'detectTime',
      key: 'detectTime',
      width: 180,
      render: (t: string) => formatDate(t),
    },
    {
      title: '安全区域内',
      dataIndex: 'inSafeZone',
      key: 'inSafeZone',
      width: 100,
      render: (v: number) => (v === 1 ? <Tag color="green">是</Tag> : <Tag color="red">否</Tag>),
    },
    {
      title: '告警触发',
      dataIndex: 'alarmTriggered',
      key: 'alarmTriggered',
      width: 100,
      render: (v: number) => (v === 1 ? <Tag color="red">是</Tag> : <Tag>否</Tag>),
    },
    {
      title: '快照',
      dataIndex: 'snapshotUrl',
      key: 'snapshotUrl',
      width: 100,
      render: (url: string) =>
        url ? (
          <img src={url} alt="快照" style={{ width: 60, height: 40, objectFit: 'cover', cursor: 'pointer' }} />
        ) : (
          '-'
        ),
    },
    {
      title: '操作',
      key: 'action',
      width: 80,
      render: (_: unknown, record) => (
        <Button type="link" size="small" onClick={() => setDetailRecord(record)}>
          详情
        </Button>
      ),
    },
  ];

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">检测记录</h2>

      <Card className="mb-4">
        <Space wrap>
          <Select
            placeholder="检测配置"
            value={filters.detectionConfigId}
            onChange={(v) => setFilters((f) => ({ ...f, detectionConfigId: v }))}
            allowClear
            style={{ width: 180 }}
            options={configs.map((c) => ({ label: `${c.petName} - ${c.deviceName}`, value: c.id }))}
          />
          <Select
            placeholder="宠物"
            value={filters.petId}
            onChange={(v) => setFilters((f) => ({ ...f, petId: v }))}
            allowClear
            style={{ width: 120 }}
            options={pets.map((p) => ({ label: p.petName, value: p.id }))}
          />
          <Select
            placeholder="设备"
            value={filters.deviceId}
            onChange={(v) => setFilters((f) => ({ ...f, deviceId: v }))}
            allowClear
            style={{ width: 180 }}
            showSearch
            optionFilterProp="label"
            options={devices.map((d) => ({ label: d.deviceName, value: d.id }))}
          />
          <Select
            placeholder="告警触发"
            value={filters.alarmTriggered}
            onChange={(v) => setFilters((f) => ({ ...f, alarmTriggered: v }))}
            allowClear
            style={{ width: 120 }}
            options={[
              { label: '全部', value: undefined },
              { label: '是', value: 1 },
              { label: '否', value: 0 },
            ]}
          />
          <RangePicker
            showTime
            value={dateRange}
            onChange={(dates) => {
              setDateRange(dates as [dayjs.Dayjs, dayjs.Dayjs] | null);
            }}
          />
          <Button icon={<SearchOutlined />} onClick={fetchRecords}>
            查询
          </Button>
        </Space>
      </Card>

      <Table columns={columns} dataSource={records} rowKey="id" loading={loading} scroll={{ x: 900 }} />

      <Modal
        title="检测详情"
        open={!!detailRecord}
        onCancel={() => setDetailRecord(null)}
        footer={null}
        width={700}
      >
        {detailRecord && (
          <div>
            {detailRecord.snapshotUrl && (
              <div className="mb-4 text-center">
                <PetBoundingBox
                  petCoordX={detailRecord.petCoordX}
                  petCoordY={detailRecord.petCoordY}
                  petWidth={detailRecord.petWidth}
                  petHeight={detailRecord.petHeight}
                  petName={detailRecord.petName}
                  imageUrl={detailRecord.snapshotUrl}
                />
              </div>
            )}
            <div className="grid grid-cols-2 gap-2 text-sm">
              <div>宠物: {detailRecord.petName}</div>
              <div>设备: {detailRecord.deviceName}</div>
              <div>检测时间: {formatDate(detailRecord.detectTime)}</div>
              <div>
                安全区域内: {detailRecord.inSafeZone === 1 ? '是' : '否'}
              </div>
              <div>告警触发: {detailRecord.alarmTriggered === 1 ? '是' : '否'}</div>
              <div>
                宠物坐标: ({detailRecord.petCoordX.toFixed(1)}, {detailRecord.petCoordY.toFixed(1)})
              </div>
              <div>
                宠物尺寸: {detailRecord.petWidth.toFixed(1)} x {detailRecord.petHeight.toFixed(1)}
              </div>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
