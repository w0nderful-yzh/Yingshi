package com.yzh.yingshi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "宠物AI报告生成请求")
public class PetAiReportGenerateRequest {

    @NotBlank(message = "来源类型不能为空")
    @Schema(description = "来源类型: ALARM/DETECTION/IMAGE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sourceType;

    @Schema(description = "告警或检测记录ID")
    private Long sourceId;

    @Schema(description = "宠物ID，告警和手动图片分析时必填")
    private Long petId;

    @Schema(description = "手动图片地址，IMAGE来源时必填")
    private String imageUrl;

    @Schema(description = "用户希望AI重点回答的问题")
    private String question;

    @Schema(description = "是否忽略已有报告并重新生成")
    private Boolean regenerate = false;
}
