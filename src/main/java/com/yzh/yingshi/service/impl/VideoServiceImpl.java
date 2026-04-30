package com.yzh.yingshi.service.impl;

import com.yzh.yingshi.common.api.BusinessCode;
import com.yzh.yingshi.common.exception.BusinessException;
import com.yzh.yingshi.constant.VideoConstant;
import com.yzh.yingshi.dto.CloudPlaybackUrlRequestDTO;
import com.yzh.yingshi.dto.CloudRecordQueryDTO;
import com.yzh.yingshi.dto.LiveUrlRequestDTO;
import com.yzh.yingshi.entity.Device;
import com.yzh.yingshi.mapper.DeviceMapper;
import com.yzh.yingshi.service.EzvizVideoService;
import com.yzh.yingshi.service.VideoService;
import com.yzh.yingshi.vo.CloudPlaybackUrlVO;
import com.yzh.yingshi.vo.CloudRecordFileVO;
import com.yzh.yingshi.vo.LiveUrlVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final DeviceMapper deviceMapper;
    private final EzvizVideoService ezvizVideoService;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern(VideoConstant.DATE_TIME_PATTERN);

    @Override
    public LiveUrlVO getLiveUrl(LiveUrlRequestDTO dto) {
        Device device = checkAndGetEzvizDevice(dto.getDeviceId(), true);

        Integer channelNo = device.getChannelNo() == null ? VideoConstant.DEFAULT_CHANNEL_NO : device.getChannelNo();
        Integer protocol = dto.getProtocol() == null ? VideoConstant.DEFAULT_LIVE_PROTOCOL : dto.getProtocol();
        Integer quality = dto.getQuality() == null ? VideoConstant.DEFAULT_QUALITY : dto.getQuality();
        Integer expireTime = dto.getExpireTime() == null ? VideoConstant.DEFAULT_EXPIRE_TIME : dto.getExpireTime();

        return ezvizVideoService.getLiveAddress(
                device.getId(), device.getDeviceSerial(), channelNo,
                protocol, quality, expireTime
        );
    }

    @Override
    public List<CloudRecordFileVO> listCloudRecords(CloudRecordQueryDTO dto) {
        Device device = checkAndGetEzvizDevice(dto.getDeviceId(), false);
        validateTimeRange(dto.getStartTime(), dto.getEndTime(), true);

        Integer channelNo = device.getChannelNo() == null ? VideoConstant.DEFAULT_CHANNEL_NO : device.getChannelNo();

        return ezvizVideoService.listCloudRecordFiles(
                device.getId(), device.getDeviceSerial(), channelNo,
                dto.getStartTime(), dto.getEndTime()
        );
    }

    @Override
    public CloudPlaybackUrlVO getCloudPlaybackUrl(CloudPlaybackUrlRequestDTO dto) {
        Device device = checkAndGetEzvizDevice(dto.getDeviceId(), false);
        validateTimeRange(dto.getStartTime(), dto.getEndTime(), true);

        Integer channelNo = device.getChannelNo() == null ? VideoConstant.DEFAULT_CHANNEL_NO : device.getChannelNo();
        Integer protocol = dto.getProtocol() == null ? VideoConstant.DEFAULT_CLOUD_PLAYBACK_PROTOCOL : dto.getProtocol();
        Integer quality = dto.getQuality() == null ? VideoConstant.DEFAULT_QUALITY : dto.getQuality();
        Integer expireTime = dto.getExpireTime() == null ? VideoConstant.DEFAULT_EXPIRE_TIME : dto.getExpireTime();

        return ezvizVideoService.getCloudPlaybackAddress(
                device.getId(), device.getDeviceSerial(), channelNo,
                dto.getStartTime(), dto.getEndTime(),
                protocol, quality, expireTime
        );
    }

    private Device checkAndGetEzvizDevice(Long deviceId, boolean requireOnline) {
        if (deviceId == null) {
            throw new BusinessException(BusinessCode.PARAM_INVALID, "设备ID不能为空");
        }

        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "设备不存在");
        }

        if (!VideoConstant.SOURCE_TYPE_EZVIZ.equals(device.getSourceType())) {
            throw new BusinessException(BusinessCode.PARAM_INVALID, "当前设备不是萤石设备");
        }

        if (VideoConstant.STATUS_DISABLED.equals(device.getStatus())) {
            throw new BusinessException(BusinessCode.STATUS_CONFLICT, "设备已禁用");
        }

        if (device.getDeviceSerial() == null || device.getDeviceSerial().isBlank()) {
            throw new BusinessException(BusinessCode.PARAM_INVALID, "设备序列号为空");
        }

        if (requireOnline && VideoConstant.STATUS_OFFLINE.equals(device.getStatus())) {
            throw new BusinessException(BusinessCode.STATUS_CONFLICT, "设备离线，无法预览");
        }

        return device;
    }

    private void validateTimeRange(String startTime, String endTime, boolean requireSameDay) {
        if (startTime == null || startTime.isBlank()) {
            throw new BusinessException(BusinessCode.PARAM_INVALID, "startTime 不能为空");
        }
        if (endTime == null || endTime.isBlank()) {
            throw new BusinessException(BusinessCode.PARAM_INVALID, "endTime 不能为空");
        }

        LocalDateTime start;
        LocalDateTime end;
        try {
            start = LocalDateTime.parse(startTime, DT_FMT);
            end = LocalDateTime.parse(endTime, DT_FMT);
        } catch (DateTimeParseException e) {
            throw new BusinessException(BusinessCode.PARAM_INVALID, "时间格式错误，请使用 yyyy-MM-dd HH:mm:ss");
        }

        if (!start.isBefore(end)) {
            throw new BusinessException(BusinessCode.PARAM_INVALID, "开始时间必须早于结束时间");
        }

        if (requireSameDay) {
            LocalDate startDate = start.toLocalDate();
            LocalDate endDate = end.toLocalDate();
            if (!startDate.equals(endDate)) {
                throw new BusinessException(BusinessCode.PARAM_INVALID, "云存储回放开始时间和结束时间必须在同一天");
            }
        }
    }
}
