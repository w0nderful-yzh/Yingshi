package com.yzh.yingshi.common.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yzh.yingshi.common.api.BusinessCode;
import com.yzh.yingshi.common.exception.BusinessException;
import com.yzh.yingshi.entity.Device;
import com.yzh.yingshi.entity.Pet;
import com.yzh.yingshi.entity.UserDevice;
import com.yzh.yingshi.mapper.DeviceMapper;
import com.yzh.yingshi.mapper.UserDeviceMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CurrentUserService {

    private final HttpServletRequest request;
    private final UserDeviceMapper userDeviceMapper;
    private final DeviceMapper deviceMapper;

    @Value("${app.auth.allow-unbound-device-access:true}")
    private boolean allowUnboundDeviceAccess;

    public Long requireCurrentUserId() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(BusinessCode.UNAUTHORIZED, "用户未登录");
        }
        return userId;
    }

    public Long getCurrentUserId() {
        Object attr = request.getAttribute("userId");
        if (attr instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public boolean hasAuthenticatedUser() {
        return getCurrentUserId() != null;
    }

    public String requireCurrentRole() {
        Object attr = request.getAttribute("role");
        String role = attr instanceof String ? (String) attr : null;
        if (role == null || !UserRole.isKnown(role)) {
            throw new BusinessException(BusinessCode.FORBIDDEN, "用户角色无效");
        }
        return role;
    }

    public boolean hasWriteAccess() {
        try {
            return UserRole.canWrite(requireCurrentRole());
        } catch (BusinessException ex) {
            return false;
        }
    }

    public void requireWriteAccess() {
        if (!UserRole.canWrite(requireCurrentRole())) {
            throw new BusinessException(BusinessCode.FORBIDDEN, "当前角色没有写入权限");
        }
    }

    public Set<String> getAuthorizedDeviceSerials() {
        Long userId = requireCurrentUserId();
        List<UserDevice> bindings = userDeviceMapper.selectList(
                new LambdaQueryWrapper<UserDevice>()
                        .eq(UserDevice::getUserId, userId)
                        .eq(UserDevice::getStatus, 1)
        );
        if (bindings.isEmpty()) {
            if (!allowUnboundDeviceAccess) {
                return Collections.emptySet();
            }
            return listAllAccessibleDevices().stream()
                    .map(Device::getDeviceSerial)
                    .filter(serial -> serial != null && !serial.isBlank())
                    .collect(Collectors.toSet());
        }
        return bindings.stream()
                .map(UserDevice::getDeviceSerial)
                .filter(serial -> serial != null && !serial.isBlank())
                .collect(Collectors.toSet());
    }

    public Set<Long> getAuthorizedDeviceIds() {
        if (allowUnboundDeviceAccess && userHasNoBoundDevices()) {
            return listAllAccessibleDevices().stream()
                    .map(Device::getId)
                    .collect(Collectors.toSet());
        }

        Set<String> serials = getAuthorizedDeviceSerials();
        if (serials.isEmpty()) {
            return Collections.emptySet();
        }

        List<Device> devices = deviceMapper.selectList(
                new LambdaQueryWrapper<Device>()
                        .in(Device::getDeviceSerial, serials)
        );
        return devices.stream()
                .map(Device::getId)
                .collect(Collectors.toSet());
    }

    private boolean userHasNoBoundDevices() {
        Long userId = requireCurrentUserId();
        Long count = userDeviceMapper.selectCount(
                new LambdaQueryWrapper<UserDevice>()
                        .eq(UserDevice::getUserId, userId)
                        .eq(UserDevice::getStatus, 1)
        );
        return count == null || count == 0;
    }

    private List<Device> listAllAccessibleDevices() {
        return deviceMapper.selectList(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getDeleted, 0)
        );
    }

    public void assertDeviceAccessible(Long deviceId) {
        if (deviceId == null) {
            throw new BusinessException(BusinessCode.PARAM_INVALID, "设备ID不能为空");
        }
        if (!getAuthorizedDeviceIds().contains(deviceId)) {
            throw new BusinessException(BusinessCode.FORBIDDEN, "无权访问该设备");
        }
    }

    public void assertDeviceAccessible(Device device) {
        if (device == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "设备不存在");
        }
        String serial = device.getDeviceSerial();
        if (serial == null || !getAuthorizedDeviceSerials().contains(serial)) {
            throw new BusinessException(BusinessCode.FORBIDDEN, "无权访问该设备");
        }
    }

    public void assertPetOwned(Pet pet) {
        if (pet == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "宠物不存在");
        }
        Long userId = requireCurrentUserId();
        if (!userId.equals(pet.getUserId())) {
            throw new BusinessException(BusinessCode.FORBIDDEN, "无权访问该宠物");
        }
    }
}
