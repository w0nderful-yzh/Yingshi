package com.yzh.yingshi.controller;

import com.yzh.yingshi.common.api.ApiResponse;
import com.yzh.yingshi.dto.PetAiReportGenerateRequest;
import com.yzh.yingshi.service.PetAiReportService;
import com.yzh.yingshi.vo.PetAiReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "宠物AI分析报告", description = "基于MiMo多模态模型生成可追溯的宠物事件报告")
@RestController
@RequestMapping("/api/pet-ai/reports")
@RequiredArgsConstructor
public class PetAiReportController {

    private final PetAiReportService reportService;

    @Operation(summary = "生成AI事件分析报告")
    @PostMapping("/generate")
    public ApiResponse<PetAiReportVO> generate(@Valid @RequestBody PetAiReportGenerateRequest request) {
        return ApiResponse.success(reportService.generate(request));
    }

    @Operation(summary = "查询AI分析报告")
    @GetMapping
    public ApiResponse<List<PetAiReportVO>> list(
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String riskLevel) {
        return ApiResponse.success(reportService.list(petId, sourceType, riskLevel));
    }

    @Operation(summary = "查看AI分析报告详情")
    @GetMapping("/{id}")
    public ApiResponse<PetAiReportVO> detail(@PathVariable Long id) {
        return ApiResponse.success(reportService.detail(id));
    }
}
