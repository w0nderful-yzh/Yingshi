package com.yzh.yingshi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_ezviz_account")
public class UserEzvizAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String accessToken;

    private String refreshToken;

    private Long expireTime;

    private String deviceTrustId;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
