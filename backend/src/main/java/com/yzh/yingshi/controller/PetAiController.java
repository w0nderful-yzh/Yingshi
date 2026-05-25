package com.yzh.yingshi.controller;

import com.yzh.yingshi.common.api.ApiResponse;
import com.yzh.yingshi.dto.PetAnalyzeRequest;
import com.yzh.yingshi.service.PetAiService;
import com.yzh.yingshi.vo.BehaviorAnalysisResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 宠物AI助手接口
 */
@Tag(name = "宠物AI助手", description = "基于大模型的宠物行为分析与问答")
@RestController
@RequestMapping("/api/pet-ai")
@RequiredArgsConstructor
public class PetAiController {

    private final PetAiService petAiService;

    @Operation(summary = "分析宠物行为", description = "基于检测数据（detectionJson）分析宠物行为状态，返回结构化结果")
    @PostMapping("/analyze")
    public ApiResponse<BehaviorAnalysisResult> analyzeBehavior(@Valid @RequestBody PetAnalyzeRequest request) {
        BehaviorAnalysisResult result = petAiService.analyzePetBehavior(request);
        return ApiResponse.success(result);
    }

    @Operation(summary = "宠物健康建议", description = "根据宠物历史记录给出健康建议")
    @GetMapping("/health-advice")
    public ApiResponse<String> healthAdvice(
            @RequestParam @NotBlank @Size(max = 50) String petName,
            @RequestParam(required = false) @Size(max = 2000) String recentRecords) {
        String advice = petAiService.getHealthAdvice(petName, recentRecords);
        return ApiResponse.success(advice);
    }

    @Operation(summary = "宠物AI聊天（同步）", description = "用户自由提问，等待完整回复")
    @PostMapping("/chat")
    public ApiResponse<String> chat(@RequestParam @NotBlank @Size(max = 2000) String message) {
        String reply = petAiService.chat(message);
        return ApiResponse.success(reply);
    }

    @Operation(summary = "宠物AI聊天（流式SSE）", description = "用户自由提问，逐 token 推送，打字机效果")
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> chatStream(@RequestParam @NotBlank @Size(max = 2000) String message) {
        return ResponseEntity.ok()
                .header("X-Accel-Buffering", "no")
                .body(petAiService.chatStream(message));
    }
}
