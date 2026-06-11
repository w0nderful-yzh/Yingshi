import { useCallback, useEffect, useRef, useState } from 'react';
import { percentToPixel, pixelToPercent } from '@/utils/coord';

export type EditorMode = 'SELECT' | 'RECTANGLE' | 'POLYGON';

export interface ZoneData {
  key: string;
  id?: number;
  zoneName: string;
  zoneType: 'RECTANGLE' | 'POLYGON';
  rectLeft?: number;
  rectTop?: number;
  rectRight?: number;
  rectBottom?: number;
  polygonPoints?: Array<{ x: number; y: number }>;
}

interface Props {
  zones: ZoneData[];
  mode: EditorMode;
  selectedZoneKey?: string;
  hasVideo: boolean;
  onZoneComplete: (zone: ZoneData) => void;
  onZoneChange: (key: string, zone: ZoneData) => void;
  onSelectZone: (key: string | undefined) => void;
}

type Point = { x: number; y: number };
type DragState =
  | { kind: 'move'; zone: ZoneData; start: Point }
  | { kind: 'rect-handle'; zone: ZoneData; handle: number }
  | { kind: 'polygon-handle'; zone: ZoneData; handle: number }
  | null;

const FILL = 'rgba(16, 185, 129, 0.22)';
const STROKE = '#34d399';
const SELECTED_STROKE = '#fbbf24';
const HANDLE_SIZE = 8;

function clamp(value: number) {
  return Math.max(0, Math.min(100, value));
}

function getRectanglePoints(zone: ZoneData, width: number, height: number) {
  return [
    percentToPixel(zone.rectLeft ?? 0, zone.rectTop ?? 0, width, height),
    percentToPixel(zone.rectRight ?? 0, zone.rectTop ?? 0, width, height),
    percentToPixel(zone.rectRight ?? 0, zone.rectBottom ?? 0, width, height),
    percentToPixel(zone.rectLeft ?? 0, zone.rectBottom ?? 0, width, height),
  ];
}

function isPointInPolygon(point: Point, polygon: Point[]) {
  let inside = false;
  for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
    const a = polygon[i];
    const b = polygon[j];
    if ((a.y > point.y) !== (b.y > point.y)
      && point.x < ((b.x - a.x) * (point.y - a.y)) / (b.y - a.y) + a.x) {
      inside = !inside;
    }
  }
  return inside;
}

function hitHandle(point: Point, handles: Point[]) {
  return handles.findIndex((handle) =>
    Math.abs(point.x - handle.x) <= HANDLE_SIZE + 3
    && Math.abs(point.y - handle.y) <= HANDLE_SIZE + 3);
}

export default function SafeZoneCanvas({
  zones,
  mode,
  selectedZoneKey,
  hasVideo,
  onZoneComplete,
  onZoneChange,
  onSelectZone,
}: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const [canvasSize, setCanvasSize] = useState({ width: 960, height: 540 });
  const [previewZone, setPreviewZone] = useState<ZoneData | null>(null);
  const dragRef = useRef<DragState>(null);
  const drawingRef = useRef<{
    active: boolean;
    start: Point;
    current: Point;
    polygonPoints: Point[];
  }>({
    active: false,
    start: { x: 0, y: 0 },
    current: { x: 0, y: 0 },
    polygonPoints: [],
  });

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    const observer = new ResizeObserver(([entry]) => {
      const width = Math.round(entry.contentRect.width);
      setCanvasSize({ width, height: Math.round(width * 9 / 16) });
    });
    observer.observe(container);
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ratio = window.devicePixelRatio || 1;
    canvas.width = Math.round(canvasSize.width * ratio);
    canvas.height = Math.round(canvasSize.height * ratio);
    const ctx = canvas.getContext('2d');
    ctx?.setTransform(ratio, 0, 0, ratio, 0, 0);
  }, [canvasSize]);

  useEffect(() => {
    drawingRef.current.active = false;
    drawingRef.current.polygonPoints = [];
    dragRef.current = null;
    setPreviewZone(null);
  }, [mode]);

  const drawZone = useCallback((
    ctx: CanvasRenderingContext2D,
    zone: ZoneData,
    index: number,
    selected: boolean,
  ) => {
    const { width, height } = canvasSize;
    ctx.fillStyle = FILL;
    ctx.strokeStyle = selected ? SELECTED_STROKE : STROKE;
    ctx.lineWidth = selected ? 2.5 : 1.5;
    ctx.setLineDash(selected ? [] : [7, 5]);

    let labelPoint: Point | undefined;
    let handles: Point[] = [];

    if (zone.zoneType === 'RECTANGLE' && zone.rectLeft != null) {
      handles = getRectanglePoints(zone, width, height);
      const [topLeft, , bottomRight] = handles;
      ctx.fillRect(topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y - topLeft.y);
      ctx.strokeRect(topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y - topLeft.y);
      labelPoint = topLeft;
    } else if (zone.zoneType === 'POLYGON' && zone.polygonPoints?.length) {
      handles = zone.polygonPoints.map((point) => percentToPixel(point.x, point.y, width, height));
      ctx.beginPath();
      handles.forEach((point, pointIndex) => {
        if (pointIndex === 0) ctx.moveTo(point.x, point.y);
        else ctx.lineTo(point.x, point.y);
      });
      ctx.closePath();
      ctx.fill();
      ctx.stroke();
      labelPoint = handles[0];
    }

    ctx.setLineDash([]);
    if (labelPoint) {
      const label = zone.zoneName || `安全区域 ${index + 1}`;
      ctx.font = '600 12px "PingFang SC", sans-serif';
      const labelWidth = ctx.measureText(label).width + 18;
      const labelY = Math.max(8, labelPoint.y - 27);
      ctx.fillStyle = selected ? '#fbbf24' : '#10b981';
      ctx.fillRect(labelPoint.x, labelY, labelWidth, 22);
      ctx.fillStyle = '#07120f';
      ctx.fillText(label, labelPoint.x + 9, labelY + 15);
    }

    if (selected && mode === 'SELECT') {
      handles.forEach((point) => {
        ctx.fillStyle = '#f8fafc';
        ctx.strokeStyle = '#f59e0b';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.rect(point.x - HANDLE_SIZE / 2, point.y - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);
        ctx.fill();
        ctx.stroke();
      });
    }
  }, [canvasSize, mode]);

  const render = useCallback(() => {
    const canvas = canvasRef.current;
    const ctx = canvas?.getContext('2d');
    if (!canvas || !ctx) return;
    const { width, height } = canvasSize;
    ctx.clearRect(0, 0, width, height);

    if (!hasVideo) {
      ctx.fillStyle = '#101820';
      ctx.fillRect(0, 0, width, height);
      ctx.strokeStyle = 'rgba(148, 163, 184, 0.12)';
      ctx.lineWidth = 1;
      for (let x = 0; x <= width; x += width / 12) {
        ctx.beginPath();
        ctx.moveTo(x, 0);
        ctx.lineTo(x, height);
        ctx.stroke();
      }
      for (let y = 0; y <= height; y += height / 7) {
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(width, y);
        ctx.stroke();
      }
      ctx.fillStyle = '#94a3b8';
      ctx.font = '500 14px "PingFang SC", sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('视频暂不可用，区域仍会按画面百分比保存', width / 2, height / 2);
      ctx.textAlign = 'start';
    }

    zones.forEach((zone, index) => {
      const zoneToDraw = previewZone?.key === zone.key ? previewZone : zone;
      drawZone(ctx, zoneToDraw, index, zone.key === selectedZoneKey);
    });

    const drawing = drawingRef.current;
    if (!drawing.active) return;

    ctx.strokeStyle = '#fbbf24';
    ctx.fillStyle = 'rgba(251, 191, 36, 0.16)';
    ctx.lineWidth = 2;
    ctx.setLineDash([8, 5]);
    if (mode === 'RECTANGLE') {
      const x = Math.min(drawing.start.x, drawing.current.x);
      const y = Math.min(drawing.start.y, drawing.current.y);
      ctx.fillRect(x, y, Math.abs(drawing.current.x - drawing.start.x), Math.abs(drawing.current.y - drawing.start.y));
      ctx.strokeRect(x, y, Math.abs(drawing.current.x - drawing.start.x), Math.abs(drawing.current.y - drawing.start.y));
    } else if (mode === 'POLYGON' && drawing.polygonPoints.length) {
      ctx.beginPath();
      drawing.polygonPoints.forEach((point, index) => {
        if (index === 0) ctx.moveTo(point.x, point.y);
        else ctx.lineTo(point.x, point.y);
      });
      ctx.lineTo(drawing.current.x, drawing.current.y);
      ctx.stroke();
      ctx.setLineDash([]);
      drawing.polygonPoints.forEach((point) => {
        ctx.beginPath();
        ctx.arc(point.x, point.y, 4, 0, Math.PI * 2);
        ctx.fillStyle = '#f8fafc';
        ctx.fill();
      });
    }
    ctx.setLineDash([]);
  }, [canvasSize, drawZone, hasVideo, mode, previewZone, selectedZoneKey, zones]);

  useEffect(() => {
    render();
  }, [render]);

  const getPointer = (event: React.PointerEvent): Point => {
    const rect = canvasRef.current!.getBoundingClientRect();
    return { x: event.clientX - rect.left, y: event.clientY - rect.top };
  };

  const findZoneAt = (point: Point) => {
    const { width, height } = canvasSize;
    for (let index = zones.length - 1; index >= 0; index -= 1) {
      const zone = zones[index];
      if (zone.zoneType === 'RECTANGLE' && zone.rectLeft != null) {
        const [topLeft, , bottomRight] = getRectanglePoints(zone, width, height);
        if (point.x >= topLeft.x && point.x <= bottomRight.x
          && point.y >= topLeft.y && point.y <= bottomRight.y) return zone;
      }
      if (zone.zoneType === 'POLYGON' && zone.polygonPoints?.length) {
        const polygon = zone.polygonPoints.map((p) => percentToPixel(p.x, p.y, width, height));
        if (isPointInPolygon(point, polygon)) return zone;
      }
    }
    return undefined;
  };

  const handlePointerDown = (event: React.PointerEvent<HTMLCanvasElement>) => {
    const point = getPointer(event);
    const drawing = drawingRef.current;
    event.currentTarget.setPointerCapture(event.pointerId);

    if (mode === 'RECTANGLE') {
      drawing.active = true;
      drawing.start = point;
      drawing.current = point;
      return;
    }
    if (mode === 'POLYGON') {
      if (!drawing.active) {
        drawing.active = true;
        drawing.polygonPoints = [point];
      } else {
        drawing.polygonPoints.push(point);
      }
      drawing.current = point;
      render();
      return;
    }

    const selected = zones.find((zone) => zone.key === selectedZoneKey);
    if (selected) {
      const handles = selected.zoneType === 'RECTANGLE'
        ? getRectanglePoints(selected, canvasSize.width, canvasSize.height)
        : (selected.polygonPoints ?? []).map((p) => percentToPixel(p.x, p.y, canvasSize.width, canvasSize.height));
      const handle = hitHandle(point, handles);
      if (handle >= 0) {
        dragRef.current = selected.zoneType === 'RECTANGLE'
          ? { kind: 'rect-handle', zone: selected, handle }
          : { kind: 'polygon-handle', zone: selected, handle };
        return;
      }
    }

    const hit = findZoneAt(point);
    onSelectZone(hit?.key);
    if (hit) dragRef.current = { kind: 'move', zone: hit, start: point };
  };

  const handlePointerMove = (event: React.PointerEvent<HTMLCanvasElement>) => {
    const point = getPointer(event);
    const drawing = drawingRef.current;
    drawing.current = point;

    if (mode !== 'SELECT' || !dragRef.current) {
      if (drawing.active) render();
      return;
    }

    const drag = dragRef.current;
    const pct = pixelToPercent(point.x, point.y, canvasSize.width, canvasSize.height);
    let next = { ...drag.zone };

    if (drag.kind === 'rect-handle') {
      const opposite = (drag.handle + 2) % 4;
      const handles = [
        { x: drag.zone.rectLeft!, y: drag.zone.rectTop! },
        { x: drag.zone.rectRight!, y: drag.zone.rectTop! },
        { x: drag.zone.rectRight!, y: drag.zone.rectBottom! },
        { x: drag.zone.rectLeft!, y: drag.zone.rectBottom! },
      ];
      const fixed = handles[opposite];
      next = {
        ...next,
        rectLeft: Math.min(clamp(pct.x), fixed.x),
        rectTop: Math.min(clamp(pct.y), fixed.y),
        rectRight: Math.max(clamp(pct.x), fixed.x),
        rectBottom: Math.max(clamp(pct.y), fixed.y),
      };
    } else if (drag.kind === 'polygon-handle') {
      const polygonPoints = [...(drag.zone.polygonPoints ?? [])];
      polygonPoints[drag.handle] = { x: clamp(pct.x), y: clamp(pct.y) };
      next = { ...next, polygonPoints };
    } else {
      const delta = pixelToPercent(
        point.x - drag.start.x,
        point.y - drag.start.y,
        canvasSize.width,
        canvasSize.height,
      );
      if (drag.zone.zoneType === 'RECTANGLE') {
        const width = drag.zone.rectRight! - drag.zone.rectLeft!;
        const height = drag.zone.rectBottom! - drag.zone.rectTop!;
        const left = Math.max(0, Math.min(100 - width, drag.zone.rectLeft! + delta.x));
        const top = Math.max(0, Math.min(100 - height, drag.zone.rectTop! + delta.y));
        next = { ...next, rectLeft: left, rectTop: top, rectRight: left + width, rectBottom: top + height };
      } else {
        const points = drag.zone.polygonPoints ?? [];
        const minX = Math.min(...points.map((p) => p.x));
        const maxX = Math.max(...points.map((p) => p.x));
        const minY = Math.min(...points.map((p) => p.y));
        const maxY = Math.max(...points.map((p) => p.y));
        const dx = Math.max(-minX, Math.min(100 - maxX, delta.x));
        const dy = Math.max(-minY, Math.min(100 - maxY, delta.y));
        next = { ...next, polygonPoints: points.map((p) => ({ x: p.x + dx, y: p.y + dy })) };
      }
    }

    setPreviewZone(next);
  };

  const handlePointerUp = (event: React.PointerEvent<HTMLCanvasElement>) => {
    const drawing = drawingRef.current;
    if (mode === 'RECTANGLE' && drawing.active) {
      drawing.active = false;
      const start = pixelToPercent(drawing.start.x, drawing.start.y, canvasSize.width, canvasSize.height);
      const end = pixelToPercent(drawing.current.x, drawing.current.y, canvasSize.width, canvasSize.height);
      if (Math.abs(end.x - start.x) >= 2 && Math.abs(end.y - start.y) >= 2) {
        onZoneComplete({
          key: crypto.randomUUID(),
          zoneName: '',
          zoneType: 'RECTANGLE',
          rectLeft: clamp(Math.min(start.x, end.x)),
          rectTop: clamp(Math.min(start.y, end.y)),
          rectRight: clamp(Math.max(start.x, end.x)),
          rectBottom: clamp(Math.max(start.y, end.y)),
        });
      }
    }
    if (mode === 'SELECT' && previewZone) {
      onZoneChange(previewZone.key, previewZone);
      setPreviewZone(null);
    }
    dragRef.current = null;
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
    render();
  };

  const handleDoubleClick = () => {
    const drawing = drawingRef.current;
    if (mode !== 'POLYGON' || !drawing.active || drawing.polygonPoints.length < 3) return;
    drawing.active = false;
    const points = drawing.polygonPoints.slice(0, -1).map((point) =>
      pixelToPercent(point.x, point.y, canvasSize.width, canvasSize.height));
    if (points.length >= 3) {
      onZoneComplete({
        key: crypto.randomUUID(),
        zoneName: '',
        zoneType: 'POLYGON',
        polygonPoints: points.map((point) => ({ x: clamp(point.x), y: clamp(point.y) })),
      });
    }
    drawing.polygonPoints = [];
    render();
  };

  const handleKeyDown = (event: React.KeyboardEvent) => {
    if (event.key !== 'Escape') return;
    drawingRef.current.active = false;
    drawingRef.current.polygonPoints = [];
    dragRef.current = null;
    setPreviewZone(null);
    render();
  };

  return (
    <div ref={containerRef} className="safe-zone-canvas-shell">
      <canvas
        ref={canvasRef}
        className={`safe-zone-canvas safe-zone-canvas--${mode.toLowerCase()}`}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onPointerCancel={handlePointerUp}
        onDoubleClick={handleDoubleClick}
        onKeyDown={handleKeyDown}
        tabIndex={0}
        aria-label="安全区域编辑画布"
      />
    </div>
  );
}
