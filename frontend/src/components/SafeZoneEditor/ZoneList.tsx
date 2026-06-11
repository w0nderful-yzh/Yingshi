import { Button, Empty, Tag } from 'antd';
import { DeleteOutlined } from '@ant-design/icons';
import type { ZoneData } from './SafeZoneCanvas';

interface Props {
  zones: ZoneData[];
  selectedKey?: string;
  onDelete: (key: string) => void;
  onSelect: (key: string) => void;
}

export default function ZoneList({ zones, selectedKey, onDelete, onSelect }: Props) {
  if (!zones.length) {
    return (
      <Empty
        image={Empty.PRESENTED_IMAGE_SIMPLE}
        description="还没有安全区域"
        className="safe-zone-empty"
      />
    );
  }

  return (
    <div className="safe-zone-list">
      {zones.map((zone, index) => (
        <div
          role="button"
          tabIndex={0}
          key={zone.key}
          className={`safe-zone-list__item ${selectedKey === zone.key ? 'is-selected' : ''}`}
          onClick={() => onSelect(zone.key)}
          onKeyDown={(event) => {
            if (event.key === 'Enter' || event.key === ' ') {
              event.preventDefault();
              onSelect(zone.key);
            }
          }}
        >
          <span className="safe-zone-list__index">{String(index + 1).padStart(2, '0')}</span>
          <span className="safe-zone-list__content">
            <strong>{zone.zoneName || `安全区域 ${index + 1}`}</strong>
            <span>
              <Tag color={zone.zoneType === 'RECTANGLE' ? 'cyan' : 'gold'} bordered={false}>
                {zone.zoneType === 'RECTANGLE' ? '矩形' : '多边形'}
              </Tag>
              {zone.id ? '已保存' : '待保存'}
            </span>
          </span>
          <Button
            type="text"
            danger
            icon={<DeleteOutlined />}
            aria-label={`删除${zone.zoneName}`}
            onClick={(event) => {
              event.stopPropagation();
              onDelete(zone.key);
            }}
          />
        </div>
      ))}
    </div>
  );
}
