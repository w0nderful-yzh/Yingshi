import { useEffect, useRef } from 'react';
import flvjs from 'flv.js';

interface Props {
  url: string;
  autoPlay?: boolean;
  controls?: boolean;
  className?: string;
  onError?: (message: string) => void;
  fill?: boolean;
}

export default function FlvPlayer({ url, autoPlay = true, controls = true, className, onError, fill = false }: Props) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const playerRef = useRef<flvjs.Player | null>(null);

  useEffect(() => {
    const video = videoRef.current;
    if (!video || !url) return;

    if (flvjs.isSupported()) {
      const handleVideoError = () => {
        onError?.('FLV 视频流播放失败，请刷新后重试');
      };
      const tryAutoPlay = () => {
        if (autoPlay) {
          video.play().catch(() => {});
        }
      };

      const player = flvjs.createPlayer({
        type: 'flv',
        url,
        isLive: true,
      }, {
        enableStashBuffer: false,
        lazyLoad: false,
        autoCleanupSourceBuffer: true,
      });
      playerRef.current = player;
      player.attachMediaElement(video);
      player.on(flvjs.Events.ERROR, (errorType, errorDetail) => {
        console.error('FLV playback error:', errorType, errorDetail);
      });
      video.addEventListener('error', handleVideoError);
      video.addEventListener('canplay', tryAutoPlay);
      player.load();
      tryAutoPlay();
      return () => {
        video.removeEventListener('error', handleVideoError);
        video.removeEventListener('canplay', tryAutoPlay);
        player.destroy();
        playerRef.current = null;
      };
    }
  }, [url, autoPlay, onError]);

  return (
    <video
      ref={videoRef}
      controls={controls}
      muted={autoPlay}
      playsInline
      className={className}
      style={fill
        ? { width: '100%', height: '100%', maxHeight: 'none', objectFit: 'contain', display: 'block' }
        : { width: '100%', maxHeight: 500 }}
    />
  );
}
