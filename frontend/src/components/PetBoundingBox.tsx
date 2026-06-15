import type { PetSafeZoneVO } from '@/types';

interface Props {
  petCoordX?: number | null;
  petCoordY?: number | null;
  petWidth?: number | null;
  petHeight?: number | null;
  petName?: string;
  imageUrl: string;
  safeZones?: PetSafeZoneVO[];
}

const isFiniteNumber = (value: number | null | undefined): value is number =>
  typeof value === 'number' && Number.isFinite(value);

export default function PetBoundingBox({
  petCoordX,
  petCoordY,
  petWidth,
  petHeight,
  petName,
  imageUrl,
  safeZones = [],
}: Props) {
  const hasBoundingBox = [petCoordX, petCoordY, petWidth, petHeight].every(isFiniteNumber);

  return (
    <div className="relative inline-block">
      <img src={imageUrl} alt="检测快照" className="block max-w-full" style={{ maxHeight: 400 }} />
      {safeZones.length > 0 && (
        <svg
          className="absolute inset-0 h-full w-full pointer-events-none"
          viewBox="0 0 100 100"
          preserveAspectRatio="none"
          aria-label="安全区域"
        >
          {safeZones.map((zone) => {
            if (
              zone.zoneType === 'RECTANGLE'
              && isFiniteNumber(zone.rectLeft)
              && isFiniteNumber(zone.rectTop)
              && isFiniteNumber(zone.rectRight)
              && isFiniteNumber(zone.rectBottom)
            ) {
              return (
                <rect
                  key={zone.id}
                  x={zone.rectLeft}
                  y={zone.rectTop}
                  width={zone.rectRight - zone.rectLeft}
                  height={zone.rectBottom - zone.rectTop}
                  fill="rgba(34, 197, 94, 0.14)"
                  stroke="#22c55e"
                  strokeWidth="2"
                  vectorEffect="non-scaling-stroke"
                />
              );
            }
            if (zone.zoneType === 'POLYGON' && zone.polygonPoints && zone.polygonPoints.length >= 3) {
              return (
                <polygon
                  key={zone.id}
                  points={zone.polygonPoints.map((point) => `${point.x},${point.y}`).join(' ')}
                  fill="rgba(34, 197, 94, 0.14)"
                  stroke="#22c55e"
                  strokeWidth="2"
                  vectorEffect="non-scaling-stroke"
                />
              );
            }
            return null;
          })}
        </svg>
      )}
      {hasBoundingBox && (
        <div
          className="absolute border-2 border-red-500 bg-red-500/10"
          style={{
            left: `${petCoordX}%`,
            top: `${petCoordY}%`,
            width: `${petWidth}%`,
            height: `${petHeight}%`,
          }}
        >
          {petName && (
            <span className="absolute -top-6 left-0 bg-red-500 text-white text-xs px-1 py-0.5 rounded">
              {petName}
            </span>
          )}
        </div>
      )}
    </div>
  );
}
