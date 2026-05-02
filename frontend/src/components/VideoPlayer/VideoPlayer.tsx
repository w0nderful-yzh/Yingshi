import { Alert } from 'antd';
import HlsPlayer from './HlsPlayer';
import FlvPlayer from './FlvPlayer';

interface Props {
  url: string;
  autoPlay?: boolean;
  controls?: boolean;
  className?: string;
}

export default function VideoPlayer({ url, autoPlay, controls, className }: Props) {
  if (!url) {
    return <Alert type="info" message="请选择设备获取视频流" showIcon />;
  }

  const lowerUrl = url.toLowerCase();

  if (lowerUrl.includes('.m3u8') || lowerUrl.includes('hls')) {
    return <HlsPlayer url={url} autoPlay={autoPlay} controls={controls} className={className} />;
  }

  if (lowerUrl.includes('.flv') || lowerUrl.includes('flv')) {
    return <FlvPlayer url={url} autoPlay={autoPlay} controls={controls} className={className} />;
  }

  // Default: try HLS
  return <HlsPlayer url={url} autoPlay={autoPlay} controls={controls} className={className} />;
}
