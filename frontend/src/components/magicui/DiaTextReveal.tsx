import { useEffect, useRef, useState } from 'react';

/* ------------------------------------------------------------------ */
/*  DiaTextReveal — 钻石般文字逐字渐变揭示动效                           */
/*  每个字符依次从透明淡入并带色彩渐变，形成流光揭示效果                     */
/* ------------------------------------------------------------------ */

interface DiaTextRevealProps {
  text: string;
  className?: string;
  /** 渐变色数组，至少两个颜色 */
  colors?: string[];
  /** 每个字符的动画延迟间隔 (ms) */
  staggerMs?: number;
}

export function DiaTextReveal({
  text,
  className = '',
  colors = ['#A97CF8', '#F38CB8', '#FDCC92'],
  staggerMs = 80,
}: DiaTextRevealProps) {
  const [visible, setVisible] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  /* 组件挂载后触发揭示动画 */
  useEffect(() => {
    const timer = setTimeout(() => setVisible(true), 200);
    return () => clearTimeout(timer);
  }, []);

  /* 构建渐变色 CSS */
  const gradient = `linear-gradient(135deg, ${colors.join(', ')})`;

  return (
    <div ref={containerRef} className={`dia-text-reveal ${className}`}>
      {text.split('').map((char, i) => (
        <span
          key={i}
          className="dia-text-reveal__char"
          style={{
            transitionDelay: `${i * staggerMs}ms`,
            opacity: visible ? 1 : 0,
            transform: visible ? 'translateY(0)' : 'translateY(20px)',
            backgroundImage: gradient,
          }}
        >
          {char === ' ' ? '\u00A0' : char}
        </span>
      ))}
    </div>
  );
}
