import { useEffect, useRef } from 'react';

/* ------------------------------------------------------------------ */
/*  Ripple — 水波涟漪扩散动效                                            */
/*  从中心向外扩散多圈同心圆，营造柔和的水波纹背景效果                       */
/* ------------------------------------------------------------------ */

interface RippleProps {
  /** 涟漪圆圈数量 */
  count?: number;
  /** 主色调 */
  color?: string;
  className?: string;
}

export function Ripple({
  count = 8,
  color = 'rgba(167, 139, 250, 0.08)',
  className = '',
}: RippleProps) {
  return (
    <div className={`ripple-container ${className}`}>
      {Array.from({ length: count }, (_, i) => (
        <div
          key={i}
          className="ripple-circle"
          style={{
            width: `${(i + 1) * 14}%`,
            height: `${(i + 1) * 14}%`,
            borderColor: color,
            animationDelay: `${i * 0.8}s`,
            opacity: 1 - i * (0.6 / count),
          }}
        />
      ))}
    </div>
  );
}
