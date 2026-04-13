package com.yzh.yingshi.vo;

import lombok.Data;

@Data
public class DeviceVO {
    private Long id;
    private String deviceName;
    private String sourceType;
    private String streamUrl;
    private String status;
    private String remark;
}
