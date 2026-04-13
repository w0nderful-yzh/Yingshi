package com.yzh.yingshi.vo;

import lombok.Data;

@Data
public class ConnectivityTestVO {
    private Boolean reachable;
    private Integer latencyMs;
}
