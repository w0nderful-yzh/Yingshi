package com.yzh.yingshi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzh.yingshi.entity.PetSafeZone;
import com.yzh.yingshi.service.PetAiDetector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PetSafeZoneEvaluatorTest {

    private final PetSafeZoneEvaluator evaluator = new PetSafeZoneEvaluator(new ObjectMapper());

    @Test
    void usesBottomCenterAsFloorContactPoint() {
        PetSafeZone zone = rectangle(35, 65, 65, 80);
        PetAiDetector.PetDetection detection =
                new PetAiDetector.PetDetection("cat", 0.9, 40, 20, 20, 50);

        assertTrue(evaluator.isInsideAnyZone(detection, List.of(zone)));
    }

    @Test
    void rejectsDetectionWhoseFloorContactPointIsOutside() {
        PetSafeZone zone = rectangle(35, 65, 65, 80);
        PetAiDetector.PetDetection detection =
                new PetAiDetector.PetDetection("cat", 0.9, 40, 10, 20, 30);

        assertFalse(evaluator.isInsideAnyZone(detection, List.of(zone)));
    }

    @Test
    void includesPolygonBoundary() {
        PetSafeZone zone = new PetSafeZone();
        zone.setZoneType("POLYGON");
        zone.setPolygonPoints("""
                [{"x":20,"y":20},{"x":80,"y":20},{"x":80,"y":80},{"x":20,"y":80}]
                """);
        PetAiDetector.PetDetection detection =
                new PetAiDetector.PetDetection("dog", 0.9, 10, 50, 20, 30);

        assertTrue(evaluator.isInsideAnyZone(detection, List.of(zone)));
    }

    private PetSafeZone rectangle(double left, double top, double right, double bottom) {
        PetSafeZone zone = new PetSafeZone();
        zone.setZoneType("RECTANGLE");
        zone.setRectLeft(left);
        zone.setRectTop(top);
        zone.setRectRight(right);
        zone.setRectBottom(bottom);
        return zone;
    }
}
