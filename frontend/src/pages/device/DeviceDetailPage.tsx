import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, Descriptions, Button, Space, Modal, Form, Input, Select, message, Popconfirm } from 'antd';
import { VideoCameraOutlined, PlayCircleOutlined } from '@ant-design/icons';
import { getDeviceById, updateDevice, deleteDevice, enableDevice, disableDevice } from '@/api/device';
import type { DeviceVO, DeviceUpdateDTO } from '@/types';
import StatusTag from '@/components/StatusTag';
import PageLoading from '@/components/PageLoading';
import { formatDate } from '@/utils/format';

export default function DeviceDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [device, setDevice] = useState<DeviceVO | null>(null);
  const [loading, setLoading] = useState(true);
  const [editOpen, setEditOpen] = useState(false);
  const [editLoading, setEditLoading] = useState(false);
  const [form] = Form.useForm();

  const fetchDevice = async () => {
    if (!id) return;
    setLoading(true);
    try {
      const data = await getDeviceById(Number(id));
      setDevice(data);
    } catch (err: any) {
      message.error(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDevice();
  }, [id]);

  const handleEdit = () => {
    if (!device) return;
    form.setFieldsValue({ deviceName: device.deviceName, remark: device.remark });
    setEditOpen(true);
  };

  const handleEditSubmit = async () => {
    if (!id) return;
    setEditLoading(true);
    try {
      const values = await form.validateFields();
      const dto: DeviceUpdateDTO = { deviceName: values.deviceName, remark: values.remark };
      await updateDevice(Number(id), dto);
      message.success('更新成功');
      setEditOpen(false);
      fetchDevice();
    } catch (err: any) {
      message.error(err.message);
    } finally {
      setEditLoading(false);
    }
  };

  const handleToggleStatus = async () => {
    if (!device || !id) return;
    try {
      if (device.status === 'DISABLED') {
        await enableDevice(Number(id));
        message.success('设备已启用');
      } else {
        await disableDevice(Number(id));
        message.success('设备已禁用');
      }
      fetchDevice();
    } catch (err: any) {
      message.error(err.message);
    }
  };

  const handleDelete = async () => {
    if (!id) return;
    try {
      await deleteDevice(Number(id));
      message.success('设备已删除');
      navigate('/devices');
    } catch (err: any) {
      message.error(err.message);
    }
  };

  if (loading) return <PageLoading />;
  if (!device) return <div>设备不存在</div>;

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-semibold m-0">设备详情</h2>
        <Space>
          <Button icon={<VideoCameraOutlined />} onClick={() => navigate(`/video/live?deviceId=${device.id}`)}>
            实时预览
          </Button>
          <Button icon={<PlayCircleOutlined />} onClick={() => navigate(`/video/playback?deviceId=${device.id}`)}>
            视频回放
          </Button>
          <Button onClick={handleEdit}>编辑</Button>
          <Button onClick={handleToggleStatus}>{device.status === 'DISABLED' ? '启用' : '禁用'}</Button>
          <Popconfirm title="确认删除此设备？" onConfirm={handleDelete}>
            <Button danger>删除</Button>
          </Popconfirm>
        </Space>
      </div>

      <Card>
        <Descriptions column={2} bordered>
          <Descriptions.Item label="设备ID">{device.id}</Descriptions.Item>
          <Descriptions.Item label="设备名称">{device.deviceName}</Descriptions.Item>
          <Descriptions.Item label="设备序列号">{device.deviceSerial}</Descriptions.Item>
          <Descriptions.Item label="通道号">{device.channelNo}</Descriptions.Item>
          <Descriptions.Item label="设备类型">{device.deviceType || '-'}</Descriptions.Item>
          <Descriptions.Item label="来源">{device.sourceType}</Descriptions.Item>
          <Descriptions.Item label="状态">
            <StatusTag status={device.status} />
          </Descriptions.Item>
          <Descriptions.Item label="备注">{device.remark || '-'}</Descriptions.Item>
          <Descriptions.Item label="创建时间">{formatDate(device.createdAt)}</Descriptions.Item>
          <Descriptions.Item label="更新时间">{formatDate(device.updatedAt)}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Modal
        title="编辑设备"
        open={editOpen}
        onOk={handleEditSubmit}
        onCancel={() => setEditOpen(false)}
        confirmLoading={editLoading}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="deviceName" label="设备名称" rules={[{ required: true, message: '请输入设备名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
