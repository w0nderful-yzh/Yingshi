package com.yzh.yingshi.dto;

import lombok.Data;

@Data
public class EzvizOAuthCallbackDTO {

    private String authCode;

    private String state;

    private String deviceSerials;

    private String deviceTrustId;
}
