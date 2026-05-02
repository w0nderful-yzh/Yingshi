import { useEffect } from 'react';
import { Modal, Form, Input, InputNumber, Select } from 'antd';
import type { PetVO } from '@/types';

interface Props {
  open: boolean;
  pet?: PetVO | null;
  onOk: (values: any) => void;
  onCancel: () => void;
  loading?: boolean;
}

export default function PetFormModal({ open, pet, onOk, onCancel, loading }: Props) {
  const [form] = Form.useForm();

  useEffect(() => {
    if (open) {
      if (pet) {
        form.setFieldsValue(pet);
      } else {
        form.resetFields();
      }
    }
  }, [open, pet, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    onOk(values);
  };

  return (
    <Modal
      title={pet ? '编辑宠物' : '添加宠物'}
      open={open}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={loading}
      destroyOnClose
    >
      <Form form={form} layout="vertical" preserve={false}>
        <Form.Item name="petName" label="宠物名称" rules={[{ required: true, message: '请输入宠物名称' }]}>
          <Input />
        </Form.Item>
        <Form.Item name="petType" label="宠物类型" rules={[{ required: true, message: '请选择宠物类型' }]}>
          <Select
            options={[
              { label: '狗', value: 'DOG' },
              { label: '猫', value: 'CAT' },
              { label: '其他', value: 'OTHER' },
            ]}
          />
        </Form.Item>
        <Form.Item name="age" label="年龄（月）">
          <InputNumber min={0} max={300} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="gender" label="性别">
          <Select
            allowClear
            options={[
              { label: '公', value: 'MALE' },
              { label: '母', value: 'FEMALE' },
              { label: '未知', value: 'UNKNOWN' },
            ]}
          />
        </Form.Item>
        <Form.Item name="remark" label="备注">
          <Input.TextArea rows={3} />
        </Form.Item>
        <Form.Item name="avatarUrl" label="头像URL">
          <Input placeholder="输入图片URL" />
        </Form.Item>
      </Form>
    </Modal>
  );
}
