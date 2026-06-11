import { Alert } from 'antd';
import HlsPlayer from './HlsPlayer';
import FlvPlayer from './FlvPlayer';
import EzvizPlayer from './EzvizPlayer';

interface Props {
  url: string;
  accessToken?: string;
  autoPlay?: boolean;
  controls?: boolean;
  className?: string;
  onError?: (message: string) => void;
  fill?: boolean;
}

export default function VideoPlayer({ url, accessToken, autoPlay, controls, className, onError, fill }: Props) {
  if (!url) {
    return <Alert type="info" message="请选择设备获取视频流" showIcon />;
  }

  const lowerUrl = url.toLowerCase();

  if (lowerUrl.startsWith('ezopen://')) {
    if (!accessToken) {
      return <Alert type="error" message="缺少萤石播放器访问令牌" showIcon />;
    }
    return (
      <EzvizPlayer
        url={url}
        accessToken={accessToken}
        autoPlay={autoPlay}
        controls={controls}
        className={className}
        onError={onError}
        fill={fill}
      />
    );
  }

  if (lowerUrl.includes('.m3u8') || lowerUrl.includes('hls')) {
    return <HlsPlayer url={url} autoPlay={autoPlay} controls={controls} className={className} onError={onError} fill={fill} />;
  }

  if (lowerUrl.includes('.flv') || lowerUrl.includes('flv')) {
    return <FlvPlayer url={url} autoPlay={autoPlay} controls={controls} className={className} onError={onError} fill={fill} />;
  }

  // Default: try HLS
  return <HlsPlayer url={url} autoPlay={autoPlay} controls={controls} className={className} onError={onError} fill={fill} />;
}
