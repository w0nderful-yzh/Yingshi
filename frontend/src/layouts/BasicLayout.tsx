import { Outlet } from 'react-router-dom';
import { Layout } from 'antd';
import Sidebar from './components/Sidebar';
import Header from './components/Header';

const { Content } = Layout;

export default function BasicLayout() {
  return (
    <div className="app-layout">
      <Sidebar />
      <div className="app-layout__main">
        <Header />
        <main className="app-layout__content"><Outlet /></main>
      </div>
    </div>
  );
}
