import { Space, Button, Radio, Input } from 'antd';
import { UndoOutlined, ClearOutlined } from '@ant-design/icons';

interface Props {
  drawingMode: 'RECTANGLE' | 'POLYGON';
  onModeChange: (mode: 'RECTANGLE' | 'POLYGON') => void;
  onUndo: () => void;
  onClearAll: () => void;
  zoneName: string;
  onZoneNameChange: (name: string) => void;
}

export default function ZoneToolbar({ drawingMode, onModeChange, onUndo, onClearAll, zoneName, onZoneNameChange }: Props) {
  return (
    <div className="flex flex-wrap items-center gap-3 mb-3">
      <Radio.Group value={drawingMode} onChange={(e) => onModeChange(e.target.value)}>
        <Radio.Button value="RECTANGLE">矩形</Radio.Button>
        <Radio.Button value="POLYGON">多边形</Radio.Button>
      </Radio.Group>
      <Input
        placeholder="区域名称"
        value={zoneName}
        onChange={(e) => onZoneNameChange(e.target.value)}
        style={{ width: 140 }}
        size="small"
      />
      <Space size="small">
        <Button size="small" icon={<UndoOutlined />} onClick={onUndo}>
          撤销
        </Button>
        <Button size="small" icon={<ClearOutlined />} danger onClick={onClearAll}>
          清空
        </Button>
      </Space>
      <span className="text-xs text-gray-400">
        {drawingMode === 'RECTANGLE' ? '拖拽绘制矩形区域' : '点击添加顶点，双击闭合'}
      </span>
    </div>
  );
}
