import { Outlet } from 'react-router-dom';
import { Layout } from 'antd';
import Sidebar from './components/Sidebar';
import Header from './components/Header';

const { Content } = Layout;

/* ------------------------------------------------------------------ */
/*  BasicLayout — 主框架布局                                            */
/*  深色渐变背景 + 玻璃拟态侧边栏 + 毛玻璃顶栏 + 半透明内容区               */
/* ------------------------------------------------------------------ */

export default function BasicLayout() {
  return (
    <div className="app-layout">
      <Sidebar />
      <div className="app-layout__main">
        <Header />
        <div className="app-layout__content">
          <Outlet />
        </div>
      </div>
    </div>
  );
}
