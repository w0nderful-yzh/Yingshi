package com.yzh.yingshi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SaveDeviceZonesRequest {

    @Valid
    @NotEmpty(message = "zones 不能为空")
    private List<ZonePayload> zones;

    @Data
    public static class ZonePayload {
        @NotBlank(message = "zoneName 不能为空")
        private String zoneName;
        @NotBlank(message = "zoneType 不能为空")
        private String zoneType;
        @NotEmpty(message = "coordinates 不能为空")
        private List<List<Integer>> coordinates;
    }
}
