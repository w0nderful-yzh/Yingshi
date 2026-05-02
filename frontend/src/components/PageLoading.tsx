import { Spin } from 'antd';

export default function PageLoading() {
  return (
    <div className="flex items-center justify-center h-64">
      <Spin size="large" tip="加载中..." />
    </div>
  );
}
