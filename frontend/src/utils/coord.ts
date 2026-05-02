export function pixelToPercent(
  px: number,
  py: number,
  width: number,
  height: number
): { x: number; y: number } {
  return {
    x: (px / width) * 100,
    y: (py / height) * 100,
  };
}

export function percentToPixel(
  pctX: number,
  pctY: number,
  width: number,
  height: number
): { x: number; y: number } {
  return {
    x: (pctX / 100) * width,
    y: (pctY / 100) * height,
  };
}
