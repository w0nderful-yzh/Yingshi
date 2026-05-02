package com.yzh.yingshi.service.impl;

import com.yzh.yingshi.config.PetDetectionProperties;
import com.yzh.yingshi.service.EzvizSnapshotService;
import com.yzh.yingshi.service.PetAiDetector;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于帧差法的宠物运动检测器
 *
 * 工作原理:
 * 1. 保存上一帧截图的灰度像素数组
 * 2. 获取当前帧截图, 转灰度
 * 3. 逐像素对比, 差值超过阈值的标记为运动像素
 * 4. 将相邻运动像素聚合为运动区域(连通分量)
 * 5. 返回最大运动区域作为宠物位置
 *
 * 适用场景: 仓鼠等小型宠物在固定摄像头下的运动检测
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FrameDiffPetDetector implements PetAiDetector {

    private final PetDetectionProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    /** 上一帧灰度像素数据 (按设备序列号缓存) */
    private final java.util.concurrent.ConcurrentHashMap<String, int[]> prevFrameCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** 上一帧尺寸 (按设备序列号缓存) */
    private final java.util.concurrent.ConcurrentHashMap<String, int[]> prevSizeCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("帧差宠物检测器已初始化, 检测模式: FRAME_DIFF, 像素阈值={}, 最小区域面积={}",
                properties.getPixelThreshold(), properties.getMinAreaPixels());
    }

    @Override
    public List<PetDetection> detect(String imageUrl) {
        return detect(imageUrl, null);
    }

    /**
     * 带设备序列号的检测 (推荐使用, 支持帧缓存)
     */
    public List<PetDetection> detect(String imageUrl, String deviceSerial) {
        List<PetDetection> results = new ArrayList<>();

        if (imageUrl == null || imageUrl.isBlank()) {
            log.warn("截图URL为空, 跳过检测");
            return results;
        }

        try {
            // 1. 下载当前帧
            byte[] currentBytes = downloadImage(imageUrl);
            if (currentBytes == null) {
                log.warn("下载截图失败: {}", imageUrl);
                return results;
            }

            BufferedImage currentImage = ImageIO.read(new ByteArrayInputStream(currentBytes));
            if (currentImage == null) {
                log.warn("无法解析截图");
                return results;
            }

            int width = currentImage.getWidth();
            int height = currentImage.getHeight();

            // 2. 转灰度像素
            int[] currentGray = toGrayscale(currentImage);

            // 3. 获取上一帧
            String cacheKey = deviceSerial != null ? deviceSerial : "default";
            int[] prevGray = prevFrameCache.get(cacheKey);

            // 4. 保存当前帧为下一次的"上一帧"
            prevFrameCache.put(cacheKey, currentGray);
            prevSizeCache.put(cacheKey, new int[]{width, height});

            if (prevGray == null) {
                log.info("首帧已缓存, 等待下一帧进行比对 deviceSerial={}", cacheKey);
                return results;
            }

            // 5. 帧差检测
            List<Region> regions = detectMotionRegions(prevGray, currentGray, width, height);

            if (regions.isEmpty()) {
                log.debug("未检测到运动 deviceSerial={}", cacheKey);
                return results;
            }

            // 6. 取最大运动区域作为宠物位置
            Region largest = regions.get(0);
            for (Region r : regions) {
                if (r.area > largest.area) {
                    largest = r;
                }
            }

            // 7. 转换为百分比坐标
            double pctX = (double) largest.minX / width * 100.0;
            double pctY = (double) largest.minY / height * 100.0;
            double pctW = (double) (largest.maxX - largest.minX) / width * 100.0;
            double pctH = (double) (largest.maxY - largest.minY) / height * 100.0;

            // 确保最小尺寸 (避免检测框过小)
            pctW = Math.max(pctW, 2.0);
            pctH = Math.max(pctH, 2.0);

            PetDetection detection = new PetDetection("pet", 0.8, pctX, pctY, pctW, pctH);
            results.add(detection);

            log.info("帧差检测到运动: 位置({}%,{}%), 大小({}%x{}%), 运动区域数={}",
                    String.format("%.1f", pctX), String.format("%.1f", pctY),
                    String.format("%.1f", pctW), String.format("%.1f", pctH),
                    regions.size());

        } catch (Exception e) {
            log.error("帧差检测异常: {}", e.getMessage(), e);
        }

        return results;
    }

    /**
     * 清除指定设备的帧缓存
     */
    public void clearCache(String deviceSerial) {
        prevFrameCache.remove(deviceSerial);
        prevSizeCache.remove(deviceSerial);
    }

    /**
     * 清除所有帧缓存
     */
    public void clearAllCache() {
        prevFrameCache.clear();
        prevSizeCache.clear();
    }

    // ==================== 核心算法 ====================

    /**
     * 检测运动区域 (帧差法)
     */
    private List<Region> detectMotionRegions(int[] prevGray, int[] currGray, int width, int height) {
        int pixelThreshold = properties.getPixelThreshold();
        int gridSize = properties.getGridSize();
        int gridCols = (width + gridSize - 1) / gridSize;
        int gridRows = (height + gridSize - 1) / gridSize;

        // 标记每个网格是否有运动
        boolean[][] motionGrid = new boolean[gridRows][gridCols];

        for (int gy = 0; gy < gridRows; gy++) {
            for (int gx = 0; gx < gridCols; gx++) {
                int startX = gx * gridSize;
                int startY = gy * gridSize;
                int endX = Math.min(startX + gridSize, width);
                int endY = Math.min(startY + gridSize, height);

                int motionCount = 0;
                int totalCount = 0;

                for (int y = startY; y < endY; y++) {
                    for (int x = startX; x < endX; x++) {
                        int idx = y * width + x;
                        int diff = Math.abs(currGray[idx] - prevGray[idx]);
                        if (diff > pixelThreshold) {
                            motionCount++;
                        }
                        totalCount++;
                    }
                }

                // 如果运动像素占比超过30%, 标记该网格为运动
                double ratio = (double) motionCount / totalCount;
                motionGrid[gy][gx] = ratio > 0.3;
            }
        }

        // 连通分量分析, 聚合运动区域
        return findMotionClusters(motionGrid, gridRows, gridCols, gridSize, width, height);
    }

    /**
     * 连通分量分析 (Flood Fill)
     */
    private List<Region> findMotionClusters(boolean[][] grid, int rows, int cols,
                                             int gridSize, int imgWidth, int imgHeight) {
        boolean[][] visited = new boolean[rows][cols];
        List<Region> regions = new ArrayList<>();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] && !visited[r][c]) {
                    Region region = floodFill(grid, visited, r, c, rows, cols, gridSize, imgWidth, imgHeight);
                    if (region.area >= properties.getMinAreaPixels()) {
                        regions.add(region);
                    }
                }
            }
        }

        // 按面积降序排列
        regions.sort((a, b) -> Long.compare(b.area, a.area));
        return regions;
    }

    private Region floodFill(boolean[][] grid, boolean[][] visited,
                              int startR, int startC, int rows, int cols,
                              int gridSize, int imgWidth, int imgHeight) {
        Region region = new Region();
        region.minX = Integer.MAX_VALUE;
        region.minY = Integer.MAX_VALUE;
        region.maxX = Integer.MIN_VALUE;
        region.maxY = Integer.MIN_VALUE;

        java.util.Queue<int[]> queue = new java.util.LinkedList<>();
        queue.offer(new int[]{startR, startC});
        visited[startR][startC] = true;

        while (!queue.isEmpty()) {
            int[] pos = queue.poll();
            int r = pos[0], c = pos[1];

            // 更新区域边界 (像素坐标)
            int pxMinX = c * gridSize;
            int pxMinY = r * gridSize;
            int pxMaxX = Math.min((c + 1) * gridSize, imgWidth);
            int pxMaxY = Math.min((r + 1) * gridSize, imgHeight);

            region.minX = Math.min(region.minX, pxMinX);
            region.minY = Math.min(region.minY, pxMinY);
            region.maxX = Math.max(region.maxX, pxMaxX);
            region.maxY = Math.max(region.maxY, pxMaxY);

            // 4-连通扩展
            int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] d : dirs) {
                int nr = r + d[0], nc = c + d[1];
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols
                        && grid[nr][nc] && !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    queue.offer(new int[]{nr, nc});
                }
            }
        }

        region.area = (long) (region.maxX - region.minX) * (region.maxY - region.minY);
        return region;
    }

    // ==================== 工具方法 ====================

    /**
     * 将图片转为灰度像素数组
     */
    private int[] toGrayscale(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] gray = new int[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                // 标准灰度公式
                gray[y * width + x] = (int) (0.299 * r + 0.587 * g + 0.114 * b);
            }
        }
        return gray;
    }

    /**
     * 下载图片
     */
    private byte[] downloadImage(String imageUrl) {
        try {
            return restTemplate.getForObject(imageUrl, byte[].class);
        } catch (Exception e) {
            log.error("下载图片失败: {}", e.getMessage());
            return null;
        }
    }

    /** 运动区域 (像素坐标) */
    private static class Region {
        int minX, minY, maxX, maxY;
        long area;
    }
}
