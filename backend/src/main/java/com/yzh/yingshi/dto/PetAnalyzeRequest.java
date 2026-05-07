package com.yzh.yingshi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
    private String petName;

    @Schema(description = "宠物类型 (cat/dog/...)")
    private String petType;

    @NotBlank(message = "截图URL不能为空")
    @Schema(description = "截图URL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String imageUrl;

    @Schema(description = "AI检测结果JSON（坐标、置信度等）")
    private String detectionJson;

    @Schema(description = "用户附带的额外问题")
    private String userQuestion;
}
