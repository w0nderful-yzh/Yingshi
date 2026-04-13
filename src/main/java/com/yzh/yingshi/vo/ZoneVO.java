package com.yzh.yingshi.vo;

import lombok.Data;

import java.util.List;

@Data
public class ZoneVO {
    private Long zoneId;
    private String zoneName;
    private String zoneType;
    private List<List<Integer>> coordinates;
}
