package com.yzh.yingshi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzh.yingshi.common.api.BusinessCode;
import com.yzh.yingshi.common.exception.BusinessException;
import com.yzh.yingshi.config.EzvizProperties;
import com.yzh.yingshi.constant.VideoConstant;
import com.yzh.yingshi.vo.CloudPlaybackUrlVO;
import com.yzh.yingshi.vo.CloudRecordFileVO;
import com.yzh.yingshi.vo.LiveUrlVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EzvizVideoService {

    private final EzvizProperties ezvizProperties;
    private final EzvizTokenResolver ezvizTokenResolver;
    private final ObjectMapper objectMapper;

    public LiveUrlVO getLiveAddress(Long deviceId, String deviceSerial, Integer channelNo,
                                    Integer protocol, Integer quality, Integer expireTime) {
        String token = ezvizTokenResolver.resolve();
        JsonNode data = fetchLiveAddress(token, deviceSerial, channelNo, protocol, quality, expireTime, VideoConstant.ADDRESS_TYPE_LIVE, null, null);

        if (data == null) {
            log.warn("首次获取直播地址失败，尝试刷新token后重试");
            token = ezvizTokenResolver.resolveWithRefresh();
            data = fetchLiveAddress(token, deviceSerial, channelNo, protocol, quality, expireTime, VideoConstant.ADDRESS_TYPE_LIVE, null, null);
            if (data == null) {
                throw new BusinessException(BusinessCode.INTERNAL_ERROR, "获取萤石直播地址失败");
            }
        }

        String url = data.has("url") ? data.get("url").asText() : null;
        if (url == null || url.isBlank()) {
            throw new BusinessException(BusinessCode.INTERNAL_ERROR, "萤石未返回播放地址");
        }

        LiveUrlVO vo = new LiveUrlVO();
        vo.setDeviceId(deviceId);
        vo.setDeviceSerial(deviceSerial);
        vo.setChannelNo(channelNo);
        vo.setProtocol(protocol);
        vo.setQuality(quality);
        vo.setUrl(url);
        vo.setExpireTime(data.has("expireTime") ? data.get("expireTime").asText() : null);
        return vo;
    }



    public List<CloudRecordFileVO> listCloudRecordFiles(Long deviceId, String deviceSerial, Integer channelNo,
                                                         String startTime, String endTime) {
        String token = ezvizTokenResolver.resolve();
        JsonNode data = fetchCloudRecords(token, deviceSerial, channelNo, startTime, endTime);

        if (data == null) {
            log.warn("首次查询云录像失败，尝试刷新token后重试");
            token = ezvizTokenResolver.resolveWithRefresh();
            data = fetchCloudRecords(token, deviceSerial, channelNo, startTime, endTime);
        }

        List<CloudRecordFileVO> result = new ArrayList<>();
        if (data == null || data.isNull()) {
            return result;
        }

        List<JsonNode> records = extractRecordList(data);
        for (JsonNode record : records) {
            CloudRecordFileVO vo = new CloudRecordFileVO();
            vo.setDeviceId(deviceId);
            vo.setDeviceSerial(deviceSerial);
            vo.setChannelNo(channelNo);
            vo.setStartTime(formatEpochMillis(extractTextField(record, "startTime", "beginTime", "start_time")));
            vo.setEndTime(formatEpochMillis(extractTextField(record, "endTime", "end_time")));
            vo.setRecordType("CLOUD");
            vo.setFileType(extractTextField(record, "fileType", "type"));
            vo.setSource("EZVIZ_CLOUD");
            result.add(vo);
        }
        return result;
    }

    public CloudPlaybackUrlVO getCloudPlaybackAddress(Long deviceId, String deviceSerial, Integer channelNo,
                                                       String startTime, String endTime,
                                                       Integer protocol, Integer quality, Integer expireTime) {
        String token = ezvizTokenResolver.resolve();
        JsonNode data = fetchLiveAddress(token, deviceSerial, channelNo, protocol, quality, expireTime, VideoConstant.ADDRESS_TYPE_CLOUD_PLAYBACK, startTime, endTime);

        if (data == null) {
            log.warn("首次获取云回放地址失败，尝试刷新token后重试");
            token = ezvizTokenResolver.resolveWithRefresh();
            data = fetchLiveAddress(token, deviceSerial, channelNo, protocol, quality, expireTime, VideoConstant.ADDRESS_TYPE_CLOUD_PLAYBACK, startTime, endTime);
            if (data == null) {
                throw new BusinessException(BusinessCode.INTERNAL_ERROR, "获取萤石云回放地址失败");
            }
        }

        String url = data.has("url") ? data.get("url").asText() : null;
        if (url == null || url.isBlank()) {
            throw new BusinessException(BusinessCode.INTERNAL_ERROR, "萤石未返回回放地址");
        }

        CloudPlaybackUrlVO vo = new CloudPlaybackUrlVO();
        vo.setDeviceId(deviceId);
        vo.setDeviceSerial(deviceSerial);
        vo.setChannelNo(channelNo);
        vo.setProtocol(protocol);
        vo.setQuality(quality);
        vo.setStartTime(startTime);
        vo.setEndTime(endTime);
        vo.setUrl(url);
        vo.setExpireTime(data.has("expireTime") ? data.get("expireTime").asText() : null);
        return vo;
    }

    private JsonNode fetchLiveAddress(String accessToken, String deviceSerial, Integer channelNo,
                                      Integer protocol, Integer quality, Integer expireTime,
                                      Integer type, String startTime, String stopTime) {
        String url = ezvizProperties.getBaseUrl() + "/api/lapp/v2/live/address/get";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("accessToken", accessToken);
        params.add("deviceSerial", deviceSerial);
        params.add("channelNo", String.valueOf(channelNo));
        params.add("protocol", String.valueOf(protocol));
        params.add("quality", String.valueOf(quality));
        params.add("expireTime", String.valueOf(expireTime));
        params.add("type", String.valueOf(type));
        params.add("code", "NJIXMQ");

        if (startTime != null) {
            params.add("startTime", startTime);
        }
        if (stopTime != null) {
            params.add("stopTime", stopTime);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.postForObject(url, request, String.class);
            log.info("萤石直播地址API完整响应: {}", response);
            JsonNode root = objectMapper.readTree(response);

            String code = String.valueOf(root.get("code").asText());
            if ("10002".equals(code)) {
                log.warn("萤石token过期, code=10002");
                return null;
            }
            if (!"200".equals(code)) {
                String msg = root.has("msg") ? root.get("msg").asText() : "unknown error";
                log.error("萤石直播/回放地址接口失败, code={}, msg={}, 完整响应={}", code, msg, response);
                return null;
            }

            JsonNode data = root.get("data");
            return data;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用萤石直播/回放地址接口异常", e);
            return null;
        }
    }


    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern(VideoConstant.DATE_TIME_PATTERN);

    private String toEpochMilliString(String time) {
        return String.valueOf(
                LocalDateTime.parse(time, DATE_TIME_FORMATTER)
                        .atZone(ZoneId.of("Asia/Shanghai"))
                        .toInstant()
                        .toEpochMilli()
        );
}

    private JsonNode fetchCloudRecords(String accessToken, String deviceSerial, Integer channelNo,
                                        String startTime, String endTime) {
        String url = ezvizProperties.getBaseUrl() + "/api/lapp/video/by/time";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("accessToken", accessToken);
        params.add("deviceSerial", deviceSerial);
        params.add("channelNo", String.valueOf(channelNo));
        params.add("startTime", toEpochMilliString(startTime));
        params.add("endTime", toEpochMilliString(endTime));

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.postForObject(url, request, String.class);
            JsonNode root = objectMapper.readTree(response);

            String code = String.valueOf(root.get("code").asText());
            if ("10002".equals(code)) {
                log.warn("萤石token过期, code=10002");
                return null;
            }
            if (!"200".equals(code)) {
                String msg = root.has("msg") ? root.get("msg").asText() : "unknown error";
                log.error("萤石云录像查询接口失败, code={}, msg={}", code, msg);
                return null;
            }

            return root.get("data");
        } catch (Exception e) {
            log.error("调用萤石云录像查询接口异常", e);
            return null;
        }
    }

    private List<JsonNode> extractRecordList(JsonNode data) {
        List<JsonNode> records = new ArrayList<>();
        if (data.isArray()) {
            data.forEach(records::add);
        } else if (data.isObject()) {
            String[] possibleFields = {"files", "recordList", "records", "storageFiles"};
            for (String field : possibleFields) {
                if (data.has(field) && data.get(field).isArray()) {
                    data.get(field).forEach(records::add);
                    break;
                }
            }
            if (records.isEmpty() && data.has("startTime")) {
                records.add(data);
            }
        }
        return records;
    }

    private String formatEpochMillis(String millis) {
        if (millis == null || millis.isBlank()) return null;
        try {
            long ts = Long.parseLong(millis);
            return LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(ts), ZoneId.of("Asia/Shanghai"))
                    .format(DATE_TIME_FORMATTER);
        } catch (NumberFormatException e) {
            return millis;
        }
    }

    private String extractTextField(JsonNode node, String... fieldNames) {
        for (String name : fieldNames) {
            if (node.has(name) && !node.get(name).isNull()) {
                return node.get(name).asText();
            }
        }
        return null;
    }
}
