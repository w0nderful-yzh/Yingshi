package com.yzh.yingshi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeviceSyncResultDTO {

    private Integer total;

    private Integer inserted;

    private Integer updated;

    private String message;
}
