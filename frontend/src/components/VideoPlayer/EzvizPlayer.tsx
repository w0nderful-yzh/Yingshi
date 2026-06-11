import { useEffect, useId, useRef } from 'react';
import { EZUIKitPlayer } from 'ezuikit-js';

interface Props {
  url: string;
  accessToken: string;
  autoPlay?: boolean;
  controls?: boolean;
  className?: string;
  onError?: (message: string) => void;
  fill?: boolean;
}

interface EzvizPlayerError {
  message?: string;
  type?: string;
  data?: {
    nErrorCode?: number;
    szErrorInfo?: string;
  };
}

function getPlayerErrorMessage(error: unknown) {
  const playerError = error as EzvizPlayerError;
  if (playerError?.data?.nErrorCode === 5) {
    return '设备视频已加密，请配置正确的设备验证码后重试';
  }
  return playerError?.data?.szErrorInfo
    || playerError?.message
    || '萤石播放器加载失败，请稍后重试';
}

export default function EzvizPlayer({
  url,
  accessToken,
  autoPlay = true,
  controls = false,
  className,
  onError,
  fill = false,
}: Props) {
  const reactId = useId();
  const containerId = `ezviz-player-${reactId.replace(/:/g, '')}`;
  const containerRef = useRef<HTMLDivElement>(null);
  const playerRef = useRef<EZUIKitPlayer | null>(null);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const { width, height } = container.getBoundingClientRect();
    container.dataset.playerStatus = 'loading';

    let player: EZUIKitPlayer;
    try {
      player = new EZUIKitPlayer({
        id: containerId,
        accessToken,
        url,
        width: Math.max(1, Math.round(width)),
        height: Math.max(1, Math.round(height)),
        staticPath: '/ezuikit_static',
        template: controls ? 'pcLive' : 'simple',
        audio: false,
        autoPlay,
        scaleMode: 1,
        language: 'zh',
        streamInfoCBType: 1,
        handleSuccess: () => {
          container.dataset.playerStatus = 'playing';
        },
        handleError: (error) => {
          container.dataset.playerStatus = 'error';
          onError?.(getPlayerErrorMessage(error));
        },
      });
    } catch (error) {
      container.dataset.playerStatus = 'error';
      onError?.(getPlayerErrorMessage(error));
      return;
    }

    playerRef.current = player;

    const handleFirstFrame = () => {
      container.dataset.playerStatus = 'ready';
    };
    const handleVideoInfo = (info?: { videoFormatName?: string }) => {
      if (info?.videoFormatName) {
        container.dataset.videoFormat = info.videoFormatName;
      }
    };

    player.eventEmitter.on(EZUIKitPlayer.EVENTS.firstFrameDisplay, handleFirstFrame);
    player.eventEmitter.on(EZUIKitPlayer.EVENTS.videoInfo, handleVideoInfo);

    const resizeObserver = new ResizeObserver(([entry]) => {
      if (!entry || !playerRef.current) return;
      const nextWidth = Math.max(1, Math.round(entry.contentRect.width));
      const nextHeight = Math.max(1, Math.round(entry.contentRect.height));
      playerRef.current.resize(nextWidth, nextHeight);
    });
    resizeObserver.observe(container);

    return () => {
      resizeObserver.disconnect();
      player.eventEmitter.off(EZUIKitPlayer.EVENTS.firstFrameDisplay, handleFirstFrame);
      player.eventEmitter.off(EZUIKitPlayer.EVENTS.videoInfo, handleVideoInfo);
      playerRef.current = null;
      try {
        const result = player.destroy();
        if (result instanceof Promise) {
          result.catch(() => undefined);
        }
      } catch {
        // The SDK may already have released its decoder during route changes.
      }
    };
  }, [accessToken, autoPlay, containerId, controls, onError, url]);

  return (
    <div
      id={containerId}
      ref={containerRef}
      className={className}
      style={{
        width: '100%',
        height: fill ? '100%' : undefined,
        aspectRatio: fill ? undefined : '16 / 9',
        background: '#020617',
        overflow: 'hidden',
      }}
    />
  );
}
