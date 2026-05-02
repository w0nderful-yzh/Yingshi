import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, Form, Input, InputNumber, Switch, Select, Button, Space, message } from 'antd';
import {
  getDetectionConfigById,
  createDetectionConfig,
  updateDetectionConfig,
} from '@/api/petDetection';
import { getPets } from '@/api/pet';
import { getDevices } from '@/api/device';
import type { PetVO, DeviceVO, PetDetectionConfigRequest } from '@/types';

export default function DetectionConfigFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [pets, setPets] = useState<PetVO[]>([]);
  const [devices, setDevices] = useState<DeviceVO[]>([]);
  const isEdit = !!id;

  useEffect(() => {
    Promise.all([getPets().catch(() => []), getDevices().catch(() => [])]).then(([p, d]) => {
      setPets(p);
      setDevices(d);
    });
  }, []);

  useEffect(() => {
    if (isEdit) {
      setLoading(true);
      getDetectionConfigById(Number(id))
        .then((data) => {
          form.setFieldsValue({
            petId: data.petId,
            deviceId: data.deviceId,
            enabled: data.enabled === 1,
            cooldownSeconds: data.cooldownSeconds,
            petAbsentMinutes: data.petAbsentMinutes,
            activityWindowMinutes: data.activityWindowMinutes,
            activityCountThreshold: data.activityCountThreshold,
            stillnessMinutes: data.stillnessMinutes,
            remark: data.remark,
          });
        })
        .catch((err) => message.error(err.message))
        .finally(() => setLoading(false));
    }
  }, [id, isEdit, form]);

  const handleSubmit = async () => {
    setSubmitting(true);
    try {
      const values = await form.validateFields();
      const dto: PetDetectionConfigRequest = {
        petId: values.petId,
        deviceId: values.deviceId,
        enabled: values.enabled ?? true,
        cooldownSeconds: values.cooldownSeconds,
        petAbsentMinutes: values.petAbsentMinutes,
        activityWindowMinutes: values.activityWindowMinutes,
        activityCountThreshold: values.activityCountThreshold,
        stillnessMinutes: values.stillnessMinutes,
        remark: values.remark,
      };
      if (isEdit) {
        await updateDetectionConfig(Number(id), dto);
        message.success('更新成功');
      } else {
        await createDetectionConfig(dto);
        message.success('创建成功');
      }
      navigate('/detection/configs');
    } catch (err: any) {
      message.error(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">{isEdit ? '编辑检测配置' : '新建检测配置'}</h2>

      <Card loading={loading} style={{ maxWidth: 640 }}>
        <Form form={form} layout="vertical" initialValues={{ enabled: true, cooldownSeconds: 300 }}>
          <Form.Item name="petId" label="宠物" rules={[{ required: true, message: '请选择宠物' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              options={pets.map((p) => ({ label: p.petName, value: p.id }))}
            />
          </Form.Item>
          <Form.Item name="deviceId" label="设备" rules={[{ required: true, message: '请选择设备' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              options={devices.map((d) => ({ label: `${d.deviceName} (${d.deviceSerial})`, value: d.id }))}
            />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="cooldownSeconds" label="告警冷却时间（秒）">
            <InputNumber min={30} max={3600} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="petAbsentMinutes" label="宠物消失阈值（分钟）">
            <InputNumber min={5} max={1440} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="activityWindowMinutes" label="活动检测窗口（分钟）">
            <InputNumber min={1} max={60} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="activityCountThreshold" label="活动次数阈值">
            <InputNumber min={1} max={100} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="stillnessMinutes" label="静止阈值（分钟）">
            <InputNumber min={5} max={1440} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" onClick={handleSubmit} loading={submitting}>
                {isEdit ? '更新' : '创建'}
              </Button>
              <Button onClick={() => navigate('/detection/configs')}>取消</Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}
