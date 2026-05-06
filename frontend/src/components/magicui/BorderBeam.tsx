import { useEffect, useRef } from 'react';

/* ------------------------------------------------------------------ */
/*  BorderBeam — 卡片边框上环绕流转的光束动效                             */
/*  一个发光光点沿着卡片四边持续循环移动，营造科技质感                       */
/* ------------------------------------------------------------------ */

interface BorderBeamProps {
  /** 光束绕一圈的时长 (秒) */
  duration?: number;
  /** 光束宽度 (px) */
  size?: number;
  /** 光束颜色 */
  color?: string;
  /** 容器额外 className */
  className?: string;
}

export function BorderBeam({
  duration = 8,
  size = 100,
  color = 'rgba(99, 102, 241, 0.6)',
  className = '',
}: BorderBeamProps) {
  const beamRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = beamRef.current;
    if (!el) return;
    /* 设置 CSS 自定义属性驱动动画 */
    el.style.setProperty('--border-beam-duration', `${duration}s`);
    el.style.setProperty('--border-beam-size', `${size}px`);
    el.style.setProperty('--border-beam-color', color);
  }, [duration, size, color]);

  return (
    <div
      ref={beamRef}
      className={`border-beam ${className}`}
      aria-hidden="true"
    />
  );
}
