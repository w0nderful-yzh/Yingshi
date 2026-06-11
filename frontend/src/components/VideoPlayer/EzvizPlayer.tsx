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

interface FullscreenChangeEvent {
  data?: {
    isCurrentFullscreen?: boolean;
    isCurrentBrowserFullscreen?: boolean;
  };
  isCurrentFullscreen?: boolean;
  isCurrentBrowserFullscreen?: boolean;
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

    let restoreFrame = 0;
    let restoreTimer = 0;

    const resizeToContainer = () => {
      if (!playerRef.current || !container.isConnected) return;
      const rect = container.getBoundingClientRect();
      playerRef.current.resize(
        Math.max(1, Math.round(rect.width)),
        Math.max(1, Math.round(rect.height)),
      );
    };

    const restoreEmbeddedLayout = () => {
      if (document.fullscreenElement) return;

      const playerElement = container.matches('.ezplayer')
        ? container
        : container.querySelector('.ezplayer');
      playerElement?.classList.remove(
        'ezplayer-fullscreen',
        'ezplayer-global-fullscreen',
      );
      document.body.classList.remove('ezplayer-body-mobile-noscroll');

      window.cancelAnimationFrame(restoreFrame);
      window.clearTimeout(restoreTimer);
      restoreFrame = window.requestAnimationFrame(() => {
        restoreFrame = window.requestAnimationFrame(resizeToContainer);
      });
      restoreTimer = window.setTimeout(resizeToContainer, 120);
    };

    const handleFullscreenChange = (event?: FullscreenChangeEvent) => {
      const fullscreenState = event?.data ?? event;
      const reportsFullscreen = fullscreenState?.isCurrentFullscreen === true
        || fullscreenState?.isCurrentBrowserFullscreen === true;
      const reportsExit = (
        fullscreenState?.isCurrentFullscreen !== undefined
        || fullscreenState?.isCurrentBrowserFullscreen !== undefined
      ) && !reportsFullscreen;

      if (reportsExit || (!reportsFullscreen && !document.fullscreenElement)) {
        restoreEmbeddedLayout();
      }
    };
    const handleNativeFullscreenChange = () => handleFullscreenChange();
    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        window.setTimeout(restoreEmbeddedLayout, 80);
      }
    };

    player.eventEmitter.on(EZUIKitPlayer.EVENTS.exitFullscreen, restoreEmbeddedLayout);
    player.eventEmitter.on(EZUIKitPlayer.EVENTS.fullscreenChange, handleFullscreenChange);
    document.addEventListener('fullscreenchange', handleNativeFullscreenChange);
    document.addEventListener('keydown', handleEscape);

    const resizeObserver = new ResizeObserver(resizeToContainer);
    resizeObserver.observe(container);

    return () => {
      resizeObserver.disconnect();
      window.cancelAnimationFrame(restoreFrame);
      window.clearTimeout(restoreTimer);
      document.removeEventListener('fullscreenchange', handleNativeFullscreenChange);
      document.removeEventListener('keydown', handleEscape);
      player.eventEmitter.off(EZUIKitPlayer.EVENTS.firstFrameDisplay, handleFirstFrame);
      player.eventEmitter.off(EZUIKitPlayer.EVENTS.videoInfo, handleVideoInfo);
      player.eventEmitter.off(EZUIKitPlayer.EVENTS.exitFullscreen, restoreEmbeddedLayout);
      player.eventEmitter.off(EZUIKitPlayer.EVENTS.fullscreenChange, handleFullscreenChange);
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
        display: 'block',
        maxWidth: '100%',
        minWidth: 0,
        overflow: 'hidden',
      }}
    />
  );
}
