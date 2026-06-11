package com.yzh.yingshi.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzh.yingshi.constant.PetDetectionConstant;
import com.yzh.yingshi.entity.PetSafeZone;
import com.yzh.yingshi.service.PetAiDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Uses the bottom-center of the detection box as the pet's floor contact point.
 * This is more stable for floor regions than using the visual center of the pet.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PetSafeZoneEvaluator {

    private static final double EPSILON = 0.000001;

    private final ObjectMapper objectMapper;

    public boolean isInsideAnyZone(PetAiDetector.PetDetection detection, List<PetSafeZone> zones) {
        double anchorX = detection.getX() + detection.getWidth() / 2.0;
        double anchorY = detection.getY() + detection.getHeight();

        for (PetSafeZone zone : zones) {
            if (isInsideZone(anchorX, anchorY, zone)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInsideZone(double x, double y, PetSafeZone zone) {
        if (PetDetectionConstant.ZONE_TYPE_RECTANGLE.equals(zone.getZoneType())) {
            return isPointInRectangle(x, y, zone);
        }
        if (PetDetectionConstant.ZONE_TYPE_POLYGON.equals(zone.getZoneType())) {
            return isPointInPolygon(x, y, zone);
        }
        return false;
    }

    private boolean isPointInRectangle(double x, double y, PetSafeZone zone) {
        if (zone.getRectLeft() == null || zone.getRectTop() == null
                || zone.getRectRight() == null || zone.getRectBottom() == null) {
            return false;
        }
        return x >= zone.getRectLeft() && x <= zone.getRectRight()
                && y >= zone.getRectTop() && y <= zone.getRectBottom();
    }

    private boolean isPointInPolygon(double x, double y, PetSafeZone zone) {
        if (zone.getPolygonPoints() == null || zone.getPolygonPoints().isBlank()) {
            return false;
        }

        try {
            List<Map<String, Double>> points = objectMapper.readValue(
                    zone.getPolygonPoints(), new TypeReference<>() {});
            return isPointInPolygonList(x, y, points);
        } catch (Exception e) {
            log.warn("解析安全区域多边形失败 zoneId={}: {}", zone.getId(), e.getMessage());
            return false;
        }
    }

    private boolean isPointInPolygonList(double x, double y, List<Map<String, Double>> points) {
        if (points.size() < 3) {
            return false;
        }

        boolean inside = false;
        for (int i = 0, j = points.size() - 1; i < points.size(); j = i++) {
            double xi = points.get(i).get("x");
            double yi = points.get(i).get("y");
            double xj = points.get(j).get("x");
            double yj = points.get(j).get("y");

            if (isPointOnSegment(x, y, xi, yi, xj, yj)) {
                return true;
            }
            if ((yi > y) != (yj > y)
                    && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
                inside = !inside;
            }
        }
        return inside;
    }

    private boolean isPointOnSegment(double x, double y, double x1, double y1, double x2, double y2) {
        double cross = (x - x1) * (y2 - y1) - (y - y1) * (x2 - x1);
        if (Math.abs(cross) > EPSILON) {
            return false;
        }
        return x >= Math.min(x1, x2) - EPSILON && x <= Math.max(x1, x2) + EPSILON
                && y >= Math.min(y1, y2) - EPSILON && y <= Math.max(y1, y2) + EPSILON;
    }
}
