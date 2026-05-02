interface Props {
  petCoordX: number;
  petCoordY: number;
  petWidth: number;
  petHeight: number;
  petName?: string;
  imageUrl: string;
}

export default function PetBoundingBox({ petCoordX, petCoordY, petWidth, petHeight, petName, imageUrl }: Props) {
  return (
    <div className="relative inline-block">
      <img src={imageUrl} alt="检测快照" className="max-w-full" style={{ maxHeight: 400 }} />
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
    </div>
  );
}
