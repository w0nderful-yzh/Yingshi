import { createBrowserRouter, Navigate } from 'react-router-dom';
import AuthGuard from './AuthGuard';
import BasicLayout from '@/layouts/BasicLayout';
import AuthLayout from '@/layouts/AuthLayout';
import LoginPage from '@/pages/login/LoginPage';
import RegisterPage from '@/pages/login/RegisterPage';
import DashboardPage from '@/pages/dashboard/DashboardPage';
import DeviceListPage from '@/pages/device/DeviceListPage';
import DeviceDetailPage from '@/pages/device/DeviceDetailPage';
import LivePreviewPage from '@/pages/video/LivePreviewPage';
import PlaybackPage from '@/pages/video/PlaybackPage';
import PetListPage from '@/pages/pet/PetListPage';
import DetectionConfigListPage from '@/pages/detection/DetectionConfigListPage';
import DetectionConfigFormPage from '@/pages/detection/DetectionConfigFormPage';
import SafeZoneEditorPage from '@/pages/detection/SafeZoneEditorPage';
import DetectionRecordsPage from '@/pages/detection/DetectionRecordsPage';
import AlarmListPage from '@/pages/alarm/AlarmListPage';
import UserSettingsPage from '@/pages/settings/UserSettingsPage';

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <AuthLayout />,
    children: [{ index: true, element: <LoginPage /> }],
  },
  {
    path: '/register',
    element: <AuthLayout />,
    children: [{ index: true, element: <RegisterPage /> }],
  },
  {
    element: <AuthGuard />,
    children: [
      {
        element: <BasicLayout />,
        children: [
          { index: true, element: <Navigate to="/dashboard" replace /> },
          { path: 'dashboard', element: <DashboardPage /> },
          { path: 'devices', element: <DeviceListPage /> },
          { path: 'devices/:id', element: <DeviceDetailPage /> },
          { path: 'video/live', element: <LivePreviewPage /> },
          { path: 'video/playback', element: <PlaybackPage /> },
          { path: 'pets', element: <PetListPage /> },
          { path: 'detection/configs', element: <DetectionConfigListPage /> },
          { path: 'detection/configs/new', element: <DetectionConfigFormPage /> },
          { path: 'detection/configs/:id/edit', element: <DetectionConfigFormPage /> },
          { path: 'detection/configs/:id/zones', element: <SafeZoneEditorPage /> },
          { path: 'detection/records', element: <DetectionRecordsPage /> },
          { path: 'alarms', element: <AlarmListPage /> },
          { path: 'settings', element: <UserSettingsPage /> },
        ],
      },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
]);
