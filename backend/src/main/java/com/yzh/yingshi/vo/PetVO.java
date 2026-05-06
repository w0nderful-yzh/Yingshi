package com.yzh.yingshi.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PetVO {
    private Long id;
    private String petName;
    private String petType;
    private Integer age;
    private String gender;
    private String remark;
    private String avatarUrl;
    private LocalDateTime createdAt;
}
