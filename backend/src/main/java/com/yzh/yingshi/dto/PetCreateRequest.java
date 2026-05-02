package com.yzh.yingshi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PetCreateRequest {
    @NotBlank(message = "宠物名不能为空")
    private String petName;

    @NotBlank(message = "宠物类型不能为空")
    private String petType;

    private Integer age;
    private String gender;
    private String remark;
    private String avatarUrl;
}
