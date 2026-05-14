package com.yzh.yingshi.controller;

import com.yzh.yingshi.common.api.ApiResponse;
import com.yzh.yingshi.dto.PetAnalyzeRequest;
import com.yzh.yingshi.service.PetAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 宠物AI助手接口
 */
@Tag(name = "宠物AI助手", description = "基于大模型的宠物行为分析与问答")
@RestController
@RequestMapping("/api/pet-ai")
@RequiredArgsConstructor
public class PetAiController {

    private final PetAiService petAiService;

    @Operation(summary = "分析宠物行为", description = "根据截图和检测结果分析宠物是否存在异常行为")
    @PostMapping("/analyze")
    public ApiResponse<String> analyzeBehavior(@Valid @RequestBody PetAnalyzeRequest request) {
        String result = petAiService.analyzePetBehavior(request);
        return ApiResponse.success(result);
    }

    @Operation(summary = "宠物健康建议", description = "根据宠物历史记录给出健康建议")
    @GetMapping("/health-advice")
    public ApiResponse<String> healthAdvice(
            @RequestParam String petName,
            @RequestParam(required = false) String recentRecords) {
        String advice = petAiService.getHealthAdvice(petName, recentRecords);
        return ApiResponse.success(advice);
    }

    @Operation(summary = "宠物AI聊天", description = "用户自由提问关于宠物的问题")
    @PostMapping("/chat")
    public ApiResponse<String> chat(@RequestParam String message) {
        String reply = petAiService.chat(message);
        return ApiResponse.success(reply);
    }
}
