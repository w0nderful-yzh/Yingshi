package com.yzh.yingshi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzh.yingshi.common.api.BusinessCode;
import com.yzh.yingshi.common.exception.BusinessException;
import com.yzh.yingshi.config.EzvizProperties;
import com.yzh.yingshi.dto.EzvizOAuthCallbackDTO;
import com.yzh.yingshi.entity.Device;
import com.yzh.yingshi.entity.UserDevice;
import com.yzh.yingshi.entity.UserEzvizAccount;
import com.yzh.yingshi.mapper.DeviceMapper;
import com.yzh.yingshi.mapper.UserDeviceMapper;
import com.yzh.yingshi.mapper.UserEzvizAccountMapper;
import com.yzh.yingshi.vo.EzvizAuthUrlVO;
import com.yzh.yingshi.vo.UserDeviceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EzvizOAuthService {

    private final EzvizProperties ezvizProperties;
    private final UserEzvizAccountMapper accountMapper;
    private final UserDeviceMapper userDeviceMapper;
    private final DeviceMapper deviceMapper;
    private final ObjectMapper objectMapper;

    /**
     * 生成萤石 OAuth 授权页 URL
     */
    public EzvizAuthUrlVO generateAuthUrl(Long userId) {
        String state = UUID.randomUUID().toString().replace("-", "") + ":" + userId;
        String redirectUri = resolveRedirectUri();
        String authUrl = ezvizProperties.getBaseUrl() + "/oauth2/authorize"
                + "?appKey=" + ezvizProperties.getAppKey()
                + "&responseType=code"
                + "&redirectUri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);

        log.info("生成萤石OAuth授权URL, userId={}, redirectUri={}, state={}", userId, redirectUri, state);
        return new EzvizAuthUrlVO(authUrl, state);
    }

    private String resolveRedirectUri() {
        String frontendUrl = ezvizProperties.getOauth().getFrontendUrl();
        if (StringUtils.hasText(frontendUrl)) {
            return frontendUrl.replaceAll("/+$", "") + "/oauth/ezviz/callback";
        }
        return ezvizProperties.getOauth().getRedirectUri();
    }

    /**
     * 从 state 参数中解析 userId（格式: {uuid}:{userId}）
     */
    public Long parseUserIdFromState(String state) {
        if (state == null || !state.contains(":")) {
            throw new BusinessException(BusinessCode.INTERNAL_ERROR, "无效的state参数");
        }
        try {
            return Long.parseLong(state.substring(state.lastIndexOf(":") + 1));
        } catch (NumberFormatException e) {
            throw new BusinessException(BusinessCode.INTERNAL_ERROR, "state中userId解析失败");
        }
    }

    /**
     * 处理 OAuth 回调：换 token → 存账号 → 拉设备列表 → 绑定设备
     */
    @Transactional
    public List<UserDeviceVO> handleCallback(Long userId, EzvizOAuthCallbackDTO dto) {
        // 1. 用 authCode 换 token
        String tokenUrl = ezvizProperties.getBaseUrl() + "/api/lapp/token/v2/get";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grantType", "2");
        params.add("appKey", ezvizProperties.getAppKey());
        params.add("appSecret", ezvizProperties.getAppSecret());
        params.add("authCode", dto.getAuthCode());

        HttpEntity<MultiValueMap<String, String>> httpRequest = new HttpEntity<>(params, headers);

        String accessToken;
        String refreshToken;
        Long expireTime;

        try {
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.postForObject(tokenUrl, httpRequest, String.class);
            JsonNode root = objectMapper.readTree(response);

            String code = root.get("code").asText();
            if (!"200".equals(code)) {
                String msg = root.has("msg") ? root.get("msg").asText() : "unknown";
                log.error("萤石OAuth换token失败, userId={}, code={}, msg={}", userId, code, msg);
                throw new BusinessException(BusinessCode.INTERNAL_ERROR, "授权失败: " + msg);
            }

            JsonNode data = root.get("data");
            accessToken = data.get("accessToken").asText();
            refreshToken = data.get("refreshToken").asText();
            expireTime = data.get("expireTime").asLong();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用萤石token接口异常, userId={}", userId, e);
            throw new BusinessException(BusinessCode.INTERNAL_ERROR, "调用萤石授权接口失败");
        }

        // 2. 存储/更新 user_ezviz_account
        UserEzvizAccount existing = accountMapper.selectOne(
                new LambdaQueryWrapper<UserEzvizAccount>()
                        .eq(UserEzvizAccount::getUserId, userId));
        if (existing != null) {
            existing.setAccessToken(accessToken);
            existing.setRefreshToken(refreshToken);
            existing.setExpireTime(expireTime);
            existing.setDeviceTrustId(dto.getDeviceTrustId());
            existing.setStatus(1);
            accountMapper.updateById(existing);
        } else {
            UserEzvizAccount account = new UserEzvizAccount();
            account.setUserId(userId);
            account.setAccessToken(accessToken);
            account.setRefreshToken(refreshToken);
            account.setExpireTime(expireTime);
            account.setDeviceTrustId(dto.getDeviceTrustId());
            account.setStatus(1);
            accountMapper.insert(account);
        }
        log.info("存储用户萤石OAuth账号成功, userId={}", userId);

        // 3. 获取用户设备列表
        List<JsonNode> ezvizDevices = fetchUserDeviceList(accessToken);

        // 4. 解析 callback 中的 deviceSerials（用户选择授权的设备）
        List<String> authorizedSerials = new ArrayList<>();
        if (dto.getDeviceSerials() != null && !dto.getDeviceSerials().isEmpty()) {
            authorizedSerials = List.of(dto.getDeviceSerials().split(","));
        }

        // 5. 绑定设备
        List<UserDeviceVO> boundDevices = new ArrayList<>();
        for (JsonNode ezvizDevice : ezvizDevices) {
            String deviceSerial = ezvizDevice.get("deviceSerial").asText();

            // 如果用户指定了授权设备列表，只绑定指定的设备
            if (!authorizedSerials.isEmpty() && !authorizedSerials.contains(deviceSerial)) {
                continue;
            }

            String deviceName = ezvizDevice.get("deviceName").asText();
            String deviceType = ezvizDevice.has("deviceType") ? ezvizDevice.get("deviceType").asText() : null;
            int channelNum = ezvizDevice.has("channelNum") ? ezvizDevice.get("channelNum").asInt() : 1;

            // 5a. upsert user_device
            UserDevice existingBinding = userDeviceMapper.selectOne(
                    new LambdaQueryWrapper<UserDevice>()
                            .eq(UserDevice::getUserId, userId)
                            .eq(UserDevice::getDeviceSerial, deviceSerial));
            if (existingBinding != null) {
                existingBinding.setDeviceName(deviceName);
                existingBinding.setDeviceType(deviceType);
                existingBinding.setChannelNo(channelNum);
                existingBinding.setStatus(1);
                existingBinding.setBoundAt(LocalDateTime.now());
                userDeviceMapper.updateById(existingBinding);
            } else {
                UserDevice binding = new UserDevice();
                binding.setUserId(userId);
                binding.setDeviceSerial(deviceSerial);
                binding.setDeviceName(deviceName);
                binding.setDeviceType(deviceType);
                binding.setChannelNo(channelNum);
                binding.setStatus(1);
                userDeviceMapper.insert(binding);
            }

            // 5b. upsert 全局 device 表（如果不存在则插入）
            Device globalDevice = deviceMapper.selectOne(
                    new LambdaQueryWrapper<Device>()
                            .eq(Device::getDeviceSerial, deviceSerial));
            if (globalDevice == null) {
                globalDevice = new Device();
                globalDevice.setDeviceSerial(deviceSerial);
                globalDevice.setChannelNo(channelNum);
                globalDevice.setDeviceName(deviceName);
                globalDevice.setDeviceType(deviceType);
                globalDevice.setSourceType("EZVIZ");
                globalDevice.setStatus("OFFLINE");
                globalDevice.setDeleted(0);
                deviceMapper.insert(globalDevice);
            }

            // 构造返回 VO
            UserDeviceVO vo = new UserDeviceVO();
            vo.setDeviceSerial(deviceSerial);
            vo.setDeviceName(deviceName);
            vo.setDeviceType(deviceType);
            vo.setChannelNo(channelNum);
            vo.setBoundAt(LocalDateTime.now());
            vo.setStatus(1);
            boundDevices.add(vo);
        }

        log.info("用户设备绑定完成, userId={}, boundCount={}", userId, boundDevices.size());
        return boundDevices;
    }

    /**
     * 获取用户的有效 accessToken，过期则自动刷新
     */
    public String getUserAccessToken(Long userId) {
        UserEzvizAccount account = accountMapper.selectOne(
                new LambdaQueryWrapper<UserEzvizAccount>()
                        .eq(UserEzvizAccount::getUserId, userId)
                        .eq(UserEzvizAccount::getStatus, 1));
        if (account == null || account.getAccessToken() == null) {
            return null;
        }
        if (account.getExpireTime() != null
                && System.currentTimeMillis() < account.getExpireTime() - 5 * 60 * 1000) {
            return account.getAccessToken();
        }
        return refreshToken(userId);
    }

    /**
     * 刷新用户的萤石 token
     */
    public String refreshToken(Long userId) {
        UserEzvizAccount account = accountMapper.selectOne(
                new LambdaQueryWrapper<UserEzvizAccount>()
                        .eq(UserEzvizAccount::getUserId, userId)
                        .eq(UserEzvizAccount::getStatus, 1));
        if (account == null || account.getAccessToken() == null) {
            return null;
        }

        String url = ezvizProperties.getBaseUrl() + "/api/lapp/token/v2/refresh";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("appKey", ezvizProperties.getAppKey());
        params.add("appSecret", ezvizProperties.getAppSecret());
        params.add("accessToken", account.getAccessToken());

        HttpEntity<MultiValueMap<String, String>> httpRequest = new HttpEntity<>(params, headers);

        try {
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.postForObject(url, httpRequest, String.class);
            JsonNode root = objectMapper.readTree(response);

            String code = root.get("code").asText();
            if (!"200".equals(code)) {
                log.warn("刷新用户萤石token失败, userId={}, code={}", userId, code);
                account.setStatus(0);
                accountMapper.updateById(account);
                return null;
            }

            JsonNode data = root.get("data");
            account.setAccessToken(data.get("accessToken").asText());
            account.setRefreshToken(data.get("refreshToken").asText());
            account.setExpireTime(data.get("expireTime").asLong());
            accountMapper.updateById(account);

            log.info("刷新用户萤石token成功, userId={}", userId);
            return account.getAccessToken();
        } catch (Exception e) {
            log.error("调用萤石token刷新接口异常, userId={}", userId, e);
            account.setStatus(0);
            accountMapper.updateById(account);
            return null;
        }
    }

    /**
     * 用户已绑定设备列表
     */
    public List<UserDeviceVO> listUserDevices(Long userId) {
        List<UserDevice> devices = userDeviceMapper.selectList(
                new LambdaQueryWrapper<UserDevice>()
                        .eq(UserDevice::getUserId, userId)
                        .eq(UserDevice::getStatus, 1));
        return devices.stream().map(d -> {
            UserDeviceVO vo = new UserDeviceVO();
            vo.setId(d.getId());
            vo.setDeviceSerial(d.getDeviceSerial());
            vo.setDeviceName(d.getDeviceName());
            vo.setDeviceType(d.getDeviceType());
            vo.setChannelNo(d.getChannelNo());
            vo.setBoundAt(d.getBoundAt());
            vo.setStatus(d.getStatus());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 解绑设备
     */
    public void unbindDevice(Long userId, Long userDeviceId) {
        UserDevice binding = userDeviceMapper.selectOne(
                new LambdaQueryWrapper<UserDevice>()
                        .eq(UserDevice::getId, userDeviceId)
                        .eq(UserDevice::getUserId, userId));
        if (binding == null) {
            throw new BusinessException(BusinessCode.INTERNAL_ERROR, "设备绑定不存在");
        }
        binding.setStatus(0);
        userDeviceMapper.updateById(binding);
        log.info("用户解绑设备, userId={}, deviceSerial={}", userId, binding.getDeviceSerial());
    }

    /**
     * 是否已授权
     */
    public boolean hasOAuthAccount(Long userId) {
        return accountMapper.selectCount(
                new LambdaQueryWrapper<UserEzvizAccount>()
                        .eq(UserEzvizAccount::getUserId, userId)
                        .eq(UserEzvizAccount::getStatus, 1)) > 0;
    }

    /**
     * 撤销授权
     */
    @Transactional
    public void revokeOAuth(Long userId) {
        // 撤销账号
        UserEzvizAccount account = accountMapper.selectOne(
                new LambdaQueryWrapper<UserEzvizAccount>()
                        .eq(UserEzvizAccount::getUserId, userId));
        if (account != null) {
            account.setStatus(0);
            accountMapper.updateById(account);
        }
        // 解绑所有设备
        UserDevice update = new UserDevice();
        update.setStatus(0);
        userDeviceMapper.update(
                update,
                new LambdaQueryWrapper<UserDevice>()
                        .eq(UserDevice::getUserId, userId)
                        .eq(UserDevice::getStatus, 1));
        log.info("用户撤销萤石OAuth授权, userId={}", userId);
    }

    /**
     * 用用户 token 拉取萤石设备列表
     */
    private List<JsonNode> fetchUserDeviceList(String accessToken) {
        String url = ezvizProperties.getBaseUrl() + "/api/lapp/device/list";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("accessToken", accessToken);
        params.add("pageSize", "500");
        params.add("pageStart", "0");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.postForObject(url, request, String.class);
            JsonNode root = objectMapper.readTree(response);

            String code = root.get("code").asText();
            if (!"200".equals(code)) {
                log.warn("获取用户萤石设备列表失败, code={}", code);
                return List.of();
            }

            JsonNode listNode = root.get("data").get("list");
            List<JsonNode> devices = new ArrayList<>();
            if (listNode != null && listNode.isArray()) {
                for (JsonNode node : listNode) {
                    devices.add(node);
                }
            }
            return devices;
        } catch (Exception e) {
            log.error("调用萤石设备列表接口异常", e);
            return List.of();
        }
    }
}
