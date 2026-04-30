package com.yzh.yingshi.vo;

import lombok.Data;

@Data
public class CloudRecordFileVO {

    private Long deviceId;

    private String deviceSerial;

    private Integer channelNo;

    private String startTime;

    private String endTime;

    private String recordType;

    private String fileType;

    private String source;
}
