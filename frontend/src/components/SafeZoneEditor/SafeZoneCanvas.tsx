import { useRef, useEffect, useCallback, useState } from 'react';
import { pixelToPercent, percentToPixel } from '@/utils/coord';

export interface ZoneData {
  id?: number;
  zoneName: string;
  zoneType: 'RECTANGLE' | 'POLYGON';
  rectLeft?: number;
  rectTop?: number;
  rectRight?: number;
  rectBottom?: number;
  polygonPoints?: Array<{ x: number; y: number }>;
  _color?: string;
}

interface Props {
  zones: ZoneData[];
  drawingMode: 'RECTANGLE' | 'POLYGON';
  onZoneComplete: (zone: ZoneData) => void;
  selectedZoneId?: number;
  onSelectZone?: (id: number | undefined) => void;
}

const COLORS = [
  'rgba(76,175,80,0.3)',
  'rgba(33,150,243,0.3)',
  'rgba(255,152,0,0.3)',
  'rgba(156,39,176,0.3)',
  'rgba(244,67,54,0.3)',
  'rgba(0,150,136,0.3)',
];

const BORDER_COLORS = [
  'rgba(76,175,80,0.8)',
  'rgba(33,150,243,0.8)',
  'rgba(255,152,0,0.8)',
  'rgba(156,39,176,0.8)',
  'rgba(244,67,54,0.8)',
  'rgba(0,150,136,0.8)',
];

export default function SafeZoneCanvas({ zones, drawingMode, onZoneComplete, selectedZoneId, onSelectZone }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const [canvasSize, setCanvasSize] = useState({ width: 800, height: 450 });

  // Drawing state
  const drawingRef = useRef<{
    active: boolean;
    startX: number;
    startY: number;
    currentX: number;
    currentY: number;
    polygonPoints: Array<{ x: number; y: number }>;
  }>({
    active: false,
    startX: 0,
    startY: 0,
    currentX: 0,
    currentY: 0,
    polygonPoints: [],
  });

  // ResizeObserver
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    const observer = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const { width } = entry.contentRect;
        const height = Math.round(width * 9 / 16);
        setCanvasSize({ width: Math.round(width), height });
      }
    });
    observer.observe(container);
    return () => observer.disconnect();
  }, []);

  // Update canvas resolution
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    canvas.width = canvasSize.width;
    canvas.height = canvasSize.height;
  }, [canvasSize]);

  // Render
  const render = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    const { width, height } = canvasSize;

    ctx.clearRect(0, 0, width, height);

    // Draw existing zones
    zones.forEach((zone, i) => {
      const colorIdx = i % COLORS.length;
      const fill = zone._color || COLORS[colorIdx];
      const stroke = BORDER_COLORS[colorIdx];
      const isSelected = zone.id === selectedZoneId;

      ctx.fillStyle = fill;
      ctx.strokeStyle = isSelected ? '#fff' : stroke;
      ctx.lineWidth = isSelected ? 3 : 2;

      if (zone.zoneType === 'RECTANGLE' && zone.rectLeft != null) {
        const tl = percentToPixel(zone.rectLeft, zone.rectTop!, width, height);
        const br = percentToPixel(zone.rectRight!, zone.rectBottom!, width, height);
        ctx.fillRect(tl.x, tl.y, br.x - tl.x, br.y - tl.y);
        ctx.strokeRect(tl.x, tl.y, br.x - tl.x, br.y - tl.y);

        // Label
        ctx.fillStyle = '#fff';
        ctx.font = '12px sans-serif';
        ctx.fillText(zone.zoneName || `区域${i + 1}`, tl.x + 4, tl.y + 16);
      } else if (zone.zoneType === 'POLYGON' && zone.polygonPoints?.length) {
        ctx.beginPath();
        const first = percentToPixel(zone.polygonPoints[0].x, zone.polygonPoints[0].y, width, height);
        ctx.moveTo(first.x, first.y);
        zone.polygonPoints.forEach((p, j) => {
          if (j === 0) return;
          const pt = percentToPixel(p.x, p.y, width, height);
          ctx.lineTo(pt.x, pt.y);
        });
        ctx.closePath();
        ctx.fill();
        ctx.stroke();

        // Label
        ctx.fillStyle = '#fff';
        ctx.font = '12px sans-serif';
        ctx.fillText(zone.zoneName || `区域${i + 1}`, first.x + 4, first.y + 16);
      }
    });

    // Draw in-progress shape
    const dr = drawingRef.current;
    if (dr.active) {
      ctx.setLineDash([5, 5]);
      ctx.strokeStyle = '#fff';
      ctx.fillStyle = 'rgba(255,255,255,0.2)';
      ctx.lineWidth = 2;

      if (drawingMode === 'RECTANGLE') {
        const x = Math.min(dr.startX, dr.currentX);
        const y = Math.min(dr.startY, dr.currentY);
        const w = Math.abs(dr.currentX - dr.startX);
        const h = Math.abs(dr.currentY - dr.startY);
        ctx.fillRect(x, y, w, h);
        ctx.strokeRect(x, y, w, h);
      } else if (dr.polygonPoints.length > 0) {
        ctx.beginPath();
        ctx.moveTo(dr.polygonPoints[0].x, dr.polygonPoints[0].y);
        dr.polygonPoints.forEach((p, i) => {
          if (i > 0) ctx.lineTo(p.x, p.y);
        });
        ctx.lineTo(dr.currentX, dr.currentY);
        ctx.stroke();

        // Draw vertex handles
        dr.polygonPoints.forEach((p) => {
          ctx.beginPath();
          ctx.arc(p.x, p.y, 4, 0, Math.PI * 2);
          ctx.fillStyle = '#fff';
          ctx.fill();
          ctx.stroke();
        });
      }
      ctx.setLineDash([]);
    }
  }, [zones, canvasSize, drawingMode, selectedZoneId]);

  useEffect(() => {
    render();
  }, [render]);

  const getMousePos = (e: React.MouseEvent): { x: number; y: number } => {
    const canvas = canvasRef.current!;
    const rect = canvas.getBoundingClientRect();
    return {
      x: e.clientX - rect.left,
      y: e.clientY - rect.top,
    };
  };

  const handleMouseDown = (e: React.MouseEvent) => {
    const pos = getMousePos(e);
    const dr = drawingRef.current;

    if (drawingMode === 'RECTANGLE') {
      dr.active = true;
      dr.startX = pos.x;
      dr.startY = pos.y;
      dr.currentX = pos.x;
      dr.currentY = pos.y;
    } else {
      // POLYGON
      if (!dr.active) {
        dr.active = true;
        dr.polygonPoints = [pos];
      } else {
        dr.polygonPoints.push(pos);
      }
      dr.currentX = pos.x;
      dr.currentY = pos.y;
    }
    render();
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    const pos = getMousePos(e);
    const dr = drawingRef.current;
    dr.currentX = pos.x;
    dr.currentY = pos.y;
    render();
  };

  const handleMouseUp = (e: React.MouseEvent) => {
    const dr = drawingRef.current;
    if (drawingMode === 'RECTANGLE' && dr.active) {
      dr.active = false;
      const { width, height } = canvasSize;
      const tl = pixelToPercent(Math.min(dr.startX, dr.currentX), Math.min(dr.startY, dr.currentY), width, height);
      const br = pixelToPercent(Math.max(dr.startX, dr.currentX), Math.max(dr.startY, dr.currentY), width, height);

      if (Math.abs(br.x - tl.x) > 1 && Math.abs(br.y - tl.y) > 1) {
        onZoneComplete({
          zoneName: '',
          zoneType: 'RECTANGLE',
          rectLeft: tl.x,
          rectTop: tl.y,
          rectRight: br.x,
          rectBottom: br.y,
        });
      }
      render();
    }
  };

  const handleDoubleClick = (e: React.MouseEvent) => {
    const dr = drawingRef.current;
    if (drawingMode === 'POLYGON' && dr.active && dr.polygonPoints.length >= 3) {
      dr.active = false;
      const { width, height } = canvasSize;
      const points = dr.polygonPoints.map((p) => pixelToPercent(p.x, p.y, width, height));
      onZoneComplete({
        zoneName: '',
        zoneType: 'POLYGON',
        polygonPoints: points,
      });
      dr.polygonPoints = [];
      render();
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') {
      const dr = drawingRef.current;
      dr.active = false;
      dr.polygonPoints = [];
      render();
    }
  };

  const handleClick = (e: React.MouseEvent) => {
    // Check if clicking on an existing zone
    const pos = getMousePos(e);
    const { width, height } = canvasSize;
    let found: number | undefined;

    for (let i = zones.length - 1; i >= 0; i--) {
      const zone = zones[i];
      if (zone.zoneType === 'RECTANGLE' && zone.rectLeft != null) {
        const tl = percentToPixel(zone.rectLeft, zone.rectTop!, width, height);
        const br = percentToPixel(zone.rectRight!, zone.rectBottom!, width, height);
        if (pos.x >= tl.x && pos.x <= br.x && pos.y >= tl.y && pos.y <= br.y) {
          found = zone.id;
          break;
        }
      }
    }
    onSelectZone?.(found);
  };

  return (
    <div ref={containerRef} className="relative w-full" style={{ aspectRatio: '16/9' }}>
      <canvas
        ref={canvasRef}
        className="absolute inset-0 w-full h-full cursor-crosshair bg-black/20"
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onDoubleClick={handleDoubleClick}
        onClick={handleClick}
        onKeyDown={handleKeyDown}
        tabIndex={0}
      />
    </div>
  );
}
