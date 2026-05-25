package com.yzh.yingshi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 宠物行为分析请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "宠物行为分析请求")
public class PetAnalyzeRequest {

    @Schema(description = "宠物ID，用于自动查询宠物信息作为上下文")
    private Long petId;

    @Schema(description = "宠物名称，不传则从petId查询")
    @Size(max = 50, message = "宠物名称长度不能超过50")
    private String petName;

    @Schema(description = "宠物类型 (cat/dog/...)")
    @Size(max = 30, message = "宠物类型长度不能超过30")
    private String petType;

    @NotBlank(message = "截图URL不能为空")
    @Size(max = 1000, message = "截图URL长度不能超过1000")
    @Schema(description = "截图URL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String imageUrl;

    @Schema(description = "AI检测结果JSON（坐标、置信度等）")
    @Size(max = 20000, message = "AI检测结果JSON长度不能超过20000")
    private String detectionJson;

    @Schema(description = "用户附带的额外问题")
    @Size(max = 1000, message = "用户附带问题长度不能超过1000")
    private String userQuestion;
}
