import { Spin } from 'antd';

export default function PageLoading() {
  return (
    <div className="flex flex-col gap-3 items-center justify-center h-64 text-slate-400 text-sm">
      <Spin size="large" />
      <span>加载中...</span>
    </div>
  );
}
