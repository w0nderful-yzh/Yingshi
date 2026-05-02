import { useEffect, useRef } from 'react';
import flvjs from 'flv.js';

interface Props {
  url: string;
  autoPlay?: boolean;
  controls?: boolean;
  className?: string;
}

export default function FlvPlayer({ url, autoPlay = true, controls = true, className }: Props) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const playerRef = useRef<flvjs.Player | null>(null);

  useEffect(() => {
    const video = videoRef.current;
    if (!video || !url) return;

    if (flvjs.isSupported()) {
      const player = flvjs.createPlayer({
        type: 'flv',
        url,
        isLive: false,
      });
      playerRef.current = player;
      player.attachMediaElement(video);
      player.load();
      if (autoPlay) {
        video.play().catch(() => {});
      }
      return () => {
        player.destroy();
        playerRef.current = null;
      };
    }
  }, [url, autoPlay]);

  return <video ref={videoRef} controls={controls} className={className} style={{ width: '100%', maxHeight: 500 }} />;
}
