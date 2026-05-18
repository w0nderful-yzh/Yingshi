import { lazy, Suspense, type ReactNode } from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';
import AuthGuard from './AuthGuard';
import BasicLayout from '@/layouts/BasicLayout';
import AuthLayout from '@/layouts/AuthLayout';
import PageLoading from '@/components/PageLoading';

const LoginPage = lazy(() => import('@/pages/login/LoginPage'));
const RegisterPage = lazy(() => import('@/pages/login/RegisterPage'));
const DashboardPage = lazy(() => import('@/pages/dashboard/DashboardPage'));
const DeviceListPage = lazy(() => import('@/pages/device/DeviceListPage'));
const DeviceDetailPage = lazy(() => import('@/pages/device/DeviceDetailPage'));
const LivePreviewPage = lazy(() => import('@/pages/video/LivePreviewPage'));
const PlaybackPage = lazy(() => import('@/pages/video/PlaybackPage'));
const PetListPage = lazy(() => import('@/pages/pet/PetListPage'));
const DetectionConfigListPage = lazy(() => import('@/pages/detection/DetectionConfigListPage'));
const DetectionConfigFormPage = lazy(() => import('@/pages/detection/DetectionConfigFormPage'));
const SafeZoneEditorPage = lazy(() => import('@/pages/detection/SafeZoneEditorPage'));
const DetectionRecordsPage = lazy(() => import('@/pages/detection/DetectionRecordsPage'));
const AlarmListPage = lazy(() => import('@/pages/alarm/AlarmListPage'));
const UserSettingsPage = lazy(() => import('@/pages/settings/UserSettingsPage'));
const PetAiPage = lazy(() => import('@/pages/pet-ai/PetAiPage'));
const DeviceBindPage = lazy(() => import('@/pages/device/DeviceBindPage'));
const OAuthCallbackPage = lazy(() => import('@/pages/device/OAuthCallbackPage'));

function withSuspense(element: ReactNode) {
  return <Suspense fallback={<PageLoading />}>{element}</Suspense>;
}

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <AuthLayout />,
    children: [{ index: true, element: withSuspense(<LoginPage />) }],
  },
  {
    path: '/register',
    element: <AuthLayout />,
    children: [{ index: true, element: withSuspense(<RegisterPage />) }],
  },
  {
    element: <AuthGuard />,
    children: [
      {
        element: <BasicLayout />,
        children: [
          { index: true, element: <Navigate to="/dashboard" replace /> },
          { path: 'dashboard', element: withSuspense(<DashboardPage />) },
          { path: 'devices', element: withSuspense(<DeviceListPage />) },
          { path: 'devices/bind', element: withSuspense(<DeviceBindPage />) },
          { path: 'devices/:id', element: withSuspense(<DeviceDetailPage />) },
          { path: 'oauth/ezviz/callback', element: withSuspense(<OAuthCallbackPage />) },
          { path: 'video/live', element: withSuspense(<LivePreviewPage />) },
          { path: 'video/playback', element: withSuspense(<PlaybackPage />) },
          { path: 'pets', element: withSuspense(<PetListPage />) },
          { path: 'pet-ai', element: withSuspense(<PetAiPage />) },
          { path: 'detection/configs', element: withSuspense(<DetectionConfigListPage />) },
          { path: 'detection/configs/new', element: withSuspense(<DetectionConfigFormPage />) },
          { path: 'detection/configs/:id/edit', element: withSuspense(<DetectionConfigFormPage />) },
          { path: 'detection/configs/:id/zones', element: withSuspense(<SafeZoneEditorPage />) },
          { path: 'detection/records', element: withSuspense(<DetectionRecordsPage />) },
          { path: 'alarms', element: withSuspense(<AlarmListPage />) },
          { path: 'settings', element: withSuspense(<UserSettingsPage />) },
        ],
      },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
]);
