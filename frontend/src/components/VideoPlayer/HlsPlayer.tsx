import { useEffect, useRef } from 'react';
import Hls from 'hls.js';

interface Props {
  url: string;
  autoPlay?: boolean;
  controls?: boolean;
  className?: string;
}

export default function HlsPlayer({ url, autoPlay = true, controls = true, className }: Props) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const hlsRef = useRef<Hls | null>(null);

  useEffect(() => {
    const video = videoRef.current;
    if (!video || !url) return;

    if (Hls.isSupported()) {
      const hls = new Hls();
      hlsRef.current = hls;
      hls.loadSource(url);
      hls.attachMedia(video);
      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        if (autoPlay) video.play().catch(() => {});
      });
      hls.on(Hls.Events.ERROR, (_, data) => {
        if (data.fatal) {
          console.error('HLS fatal error:', data);
        }
      });
      return () => {
        hls.destroy();
        hlsRef.current = null;
      };
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
      video.src = url;
      if (autoPlay) video.play().catch(() => {});
    }
  }, [url, autoPlay]);

  return <video ref={videoRef} controls={controls} className={className} style={{ width: '100%', maxHeight: 500 }} />;
}
