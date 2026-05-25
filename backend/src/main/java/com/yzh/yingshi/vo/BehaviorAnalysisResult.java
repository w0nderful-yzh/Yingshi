package com.yzh.yingshi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 宠物行为分析结果 —— 结构化输出，前端无需解析自然语言
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "宠物行为分析结果")
public class BehaviorAnalysisResult {

    @Schema(description = "行为状态: NORMAL / ABNORMAL / UNCERTAIN", example = "NORMAL")
    private String status;

    @Schema(description = "风险等级: LOW / MEDIUM / HIGH", example = "LOW")
    private String riskLevel;

    @Schema(description = "分析摘要，1-2 句话", example = "宠物当前正在安全区域内正常休息")
    private String summary;

    @Schema(description = "判断依据，列出支撑结论的关键数据点")
    private List<String> evidence;

    @Schema(description = "可能的异常原因（status=ABNORMAL 时填充）")
    private List<String> possibleCauses;

    @Schema(description = "给主人的建议")
    private List<String> suggestions;

    @Schema(description = "是否需要就医", example = "false")
    private Boolean needVet;

    @Schema(description = "AI 置信度 0-1", example = "0.85")
    private Double confidence;

    @Schema(description = "关联的截图URL（仅用于溯源记录）")
    private String snapshotUrl;
}
