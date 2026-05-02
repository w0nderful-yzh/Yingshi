import { List, Tag, Button, Popconfirm, Space } from 'antd';
import { DeleteOutlined, EditOutlined } from '@ant-design/icons';
import type { ZoneData } from './SafeZoneCanvas';

interface Props {
  zones: ZoneData[];
  onDelete: (index: number) => void;
  onSelect: (index: number) => void;
  selectedIndex?: number;
}

export default function ZoneList({ zones, onDelete, onSelect, selectedIndex }: Props) {
  if (zones.length === 0) {
    return <div className="text-gray-400 text-sm py-4 text-center">暂无安全区域，请在画面上绘制</div>;
  }

  return (
    <List
      size="small"
      dataSource={zones}
      renderItem={(zone, index) => (
        <List.Item
          className={`cursor-pointer ${selectedIndex === index ? 'bg-blue-50' : ''}`}
          onClick={() => onSelect(index)}
          actions={[
            <Popconfirm key="del" title="确认删除此区域？" onConfirm={() => onDelete(index)}>
              <Button type="text" size="small" danger icon={<DeleteOutlined />} />
            </Popconfirm>,
          ]}
        >
          <Space>
            <Tag color={zone.zoneType === 'RECTANGLE' ? 'blue' : 'green'}>{zone.zoneType}</Tag>
            <span>{zone.zoneName || `区域${index + 1}`}</span>
          </Space>
        </List.Item>
      )}
    />
  );
}
