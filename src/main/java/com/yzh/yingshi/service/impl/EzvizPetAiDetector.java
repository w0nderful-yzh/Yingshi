package com.yzh.yingshi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzh.yingshi.config.EzvizProperties;
import com.yzh.yingshi.service.EzvizTokenService;
import com.yzh.yingshi.service.PetAiDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.*;

/**
 * 萤石AI宠物检测器
 * 调用萤石开放平台 pet_detection 算法接口, 返回宠物坐标位置
 *
 * 接口文档: https://open.ys7.com/help/4449
 * 前置条件: 需开通AI算法服务(reasoning), 并联系客服开通接口调用权限
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EzvizPetAiDetector implements PetAiDetector {

    private final EzvizProperties ezvizProperties;
    private final EzvizTokenService ezvizTokenService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String PET_DETECTION_PATH = "/api/service/intelligence/algo/analysis/pet_detection";
    private static final String RAT_DETECTION_PATH = "/api/service/intelligence/algo/analysis/rat_detection";

    @Override
    public List<PetDetection> detect(String imageUrl) {
        List<PetDetection> results = new ArrayList<>();
        if (imageUrl == null || imageUrl.isBlank()) {
            return results;
        }

        // 先尝试宠物检测
        String token = ezvizTokenService.getAccessToken();
        results = callDetectionApi(token, imageUrl, PET_DETECTION_PATH);

        // token过期重试
        if (results == null) {
            token = ezvizTokenService.refreshToken();
            results = callDetectionApi(token, imageUrl, PET_DETECTION_PATH);
        }

        if (results == null) {
            results = new ArrayList<>();
        }

        log.info("萤石AI宠物检测完成 imageUrl={}, 检测到{}个目标", imageUrl, results.size());
        return results;
    }

    /**
     * 调用萤石AI检测接口
     * @return 检测结果列表, token过期返回null
     */
    private List<PetDetection> callDetectionApi(String accessToken, String imageUrl, String apiPath) {
        String url = ezvizProperties.getBaseUrl() + apiPath;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("accessToken", accessToken);

        // 获取图片尺寸 (可选, 提高检测精度)
        int[] imgSize = getImageSize(imageUrl);

        Map<String, Object> body = buildRequestBody(imageUrl, imgSize);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            // 检查响应状态
            JsonNode meta = root.get("meta");
            if (meta != null) {
                int code = meta.has("code") ? meta.get("code").asInt() : -1;
                if (code == 10002) {
                    log.warn("萤石AI接口token过期");
                    ezvizTokenService.clearToken();
                    return null;
                }
                if (code != 200) {
                    String msg = meta.has("message") ? meta.get("message").asText() : "unknown";
                    log.warn("萤石AI检测接口返回错误, code={}, message={}", code, msg);
                    return new ArrayList<>();
                }
            }

            // 解析检测结果
            return parseDetections(root);

        } catch (Exception e) {
            log.error("调用萤石AI检测接口异常: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 解析检测结果
     * points格式: [{"x": 0.36, "y": 0.38}, {"x": 0.59, "y": 0.69}]
     * x,y 为归一化坐标 (0~1), 需要乘以100转为百分比
     */
    private List<PetDetection> parseDetections(JsonNode root) {
        List<PetDetection> results = new ArrayList<>();

        JsonNode data = root.get("data");
        if (data == null) return results;

        JsonNode images = data.get("images");
        if (images == null || !images.isArray() || images.isEmpty()) return results;

        JsonNode image = images.get(0);
        JsonNode contentAnn = image.get("contentAnn");
        if (contentAnn == null) return results;

        JsonNode bboxes = contentAnn.get("bboxes");
        if (bboxes == null || !bboxes.isArray()) return results;

        for (JsonNode bbox : bboxes) {
            try {
                JsonNode points = bbox.get("points");
                if (points == null || points.size() < 2) continue;

                // 左上角和右下角 (归一化坐标 0~1)
                double x1 = points.get(0).get("x").asDouble();
                double y1 = points.get(0).get("y").asDouble();
                double x2 = points.get(1).get("x").asDouble();
                double y2 = points.get(1).get("y").asDouble();

                // 转为百分比 (0~100)
                double pctX = x1 * 100.0;
                double pctY = y1 * 100.0;
                double pctW = (x2 - x1) * 100.0;
                double pctH = (y2 - y1) * 100.0;

                double confidence = bbox.has("weight") ? bbox.get("weight").asDouble() : 0.0;

                // 解析标签
                String tag = "pet";
                if (bbox.has("tagInfo") && bbox.get("tagInfo").has("tag")) {
                    tag = bbox.get("tagInfo").get("tag").asText();
                }

                PetDetection detection = new PetDetection(tag, confidence, pctX, pctY, pctW, pctH);
                results.add(detection);

                log.debug("检测到{}: 位置({}%,{}%), 大小({}%x{}%), 置信度={}",
                        tag,
                        String.format("%.1f", pctX), String.format("%.1f", pctY),
                        String.format("%.1f", pctW), String.format("%.1f", pctH),
                        String.format("%.2f", confidence));

            } catch (Exception e) {
                log.warn("解析检测框失败: {}", e.getMessage());
            }
        }

        return results;
    }

    /**
     * 构建请求体
     */
    private Map<String, Object> buildRequestBody(String imageUrl, int[] imgSize) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("requestId", UUID.randomUUID().toString());
        body.put("stream", false);

        // dataInfo
        Map<String, Object> dataItem = new LinkedHashMap<>();
        dataItem.put("modal", "image");
        dataItem.put("type", "url");
        dataItem.put("data", imageUrl);
        body.put("dataInfo", List.of(dataItem));

        // dataParams (可选, 提高检测精度)
        if (imgSize != null) {
            Map<String, Object> paramItem = new LinkedHashMap<>();
            paramItem.put("modal", "image");
            paramItem.put("img_width", imgSize[0]);
            paramItem.put("img_height", imgSize[1]);
            body.put("dataParams", List.of(paramItem));
        }

        return body;
    }

    /**
     * 获取图片尺寸
     */
    private int[] getImageSize(String imageUrl) {
        try {
            BufferedImage img = ImageIO.read(new URL(imageUrl));
            if (img != null) {
                return new int[]{img.getWidth(), img.getHeight()};
            }
        } catch (Exception e) {
            log.debug("获取图片尺寸失败, 将不传dataParams: {}", e.getMessage());
        }
        return null;
    }
}
