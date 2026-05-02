import { Tag } from 'antd';
import { DeviceStatusMap } from '@/utils/constants';

interface Props {
  status: string;
}

export default function StatusTag({ status }: Props) {
  const config = DeviceStatusMap[status] || { label: status, color: 'default' };
  return <Tag color={config.color}>{config.label}</Tag>;
}
