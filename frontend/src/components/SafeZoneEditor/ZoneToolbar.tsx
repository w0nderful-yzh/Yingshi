import { Button, Segmented, Tooltip } from 'antd';
import { AimOutlined, BorderOutlined, NodeIndexOutlined, UndoOutlined } from '@ant-design/icons';
import type { EditorMode } from './SafeZoneCanvas';

interface Props {
  mode: EditorMode;
  dirty: boolean;
  onModeChange: (mode: EditorMode) => void;
  onReset: () => void;
}

export default function ZoneToolbar({ mode, dirty, onModeChange, onReset }: Props) {
  return (
    <div className="safe-zone-toolbar">
      <div>
        <div className="safe-zone-toolbar__label">编辑工具</div>
        <Segmented
          value={mode}
          onChange={(value) => onModeChange(value as EditorMode)}
          options={[
            { label: '选择调整', value: 'SELECT', icon: <AimOutlined /> },
            { label: '矩形区域', value: 'RECTANGLE', icon: <BorderOutlined /> },
            { label: '多边形', value: 'POLYGON', icon: <NodeIndexOutlined /> },
          ]}
        />
      </div>
      <div className="safe-zone-toolbar__guide">
        {mode === 'SELECT' && '点击区域后可拖动，拖拽控制点可调整边界'}
        {mode === 'RECTANGLE' && '在画面上按住并拖动，松开即可创建区域'}
        {mode === 'POLYGON' && '逐点点击勾勒边界，双击最后一点完成'}
      </div>
      <Tooltip title={dirty ? '恢复到上次保存状态' : '当前没有未保存修改'}>
        <Button icon={<UndoOutlined />} disabled={!dirty} onClick={onReset}>
          放弃修改
        </Button>
      </Tooltip>
    </div>
  );
}
