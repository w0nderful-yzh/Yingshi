package com.yzh.yingshi.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 兼容萤石设备列表接口在不同授权模式下的响应结构。
 */
public final class EzvizDeviceResponseParser {

    private EzvizDeviceResponseParser() {
    }

    public static List<JsonNode> parseDevices(JsonNode root) {
        if (root == null) {
            return List.of();
        }

        JsonNode data = root.path("data");
        JsonNode deviceArray = findDeviceArray(data);
        if (!deviceArray.isArray()) {
            return List.of();
        }

        List<JsonNode> devices = new ArrayList<>();
        deviceArray.forEach(devices::add);
        return devices;
    }

    private static JsonNode findDeviceArray(JsonNode data) {
        if (data.isArray()) {
            return data;
        }
        if (!data.isObject()) {
            return data;
        }

        for (String field : List.of("list", "devices", "items")) {
            JsonNode candidate = data.path(field);
            if (candidate.isArray()) {
                return candidate;
            }
        }
        return data;
    }
}
