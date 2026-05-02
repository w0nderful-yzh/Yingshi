import { Badge, Button } from 'antd';
import { BellOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useAlarmStore } from '@/store/alarmStore';

export default function AlarmBadge() {
  const navigate = useNavigate();
  const unreadCount = useAlarmStore((s) => s.unreadCount);

  return (
    <Badge count={unreadCount} size="small">
      <Button type="text" icon={<BellOutlined />} onClick={() => navigate('/alarms')} />
    </Badge>
  );
}
