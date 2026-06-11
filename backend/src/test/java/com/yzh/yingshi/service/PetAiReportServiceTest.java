package com.yzh.yingshi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzh.yingshi.common.auth.CurrentUserService;
import com.yzh.yingshi.dto.PetAiReportGenerateRequest;
import com.yzh.yingshi.dto.PetAiVisionResult;
import com.yzh.yingshi.entity.Pet;
import com.yzh.yingshi.entity.PetAiReport;
import com.yzh.yingshi.entity.PetDetectionConfig;
import com.yzh.yingshi.entity.PetDetectionRecord;
import com.yzh.yingshi.mapper.AlarmMessageMapper;
import com.yzh.yingshi.mapper.PetAiReportMapper;
import com.yzh.yingshi.mapper.PetDetectionConfigMapper;
import com.yzh.yingshi.mapper.PetDetectionRecordMapper;
import com.yzh.yingshi.mapper.PetMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetAiReportServiceTest {

    @Mock private PetAiReportMapper reportMapper;
    @Mock private PetMapper petMapper;
    @Mock private AlarmMessageMapper alarmMapper;
    @Mock private PetDetectionRecordMapper recordMapper;
    @Mock private PetDetectionConfigMapper configMapper;
    @Mock private CurrentUserService currentUserService;
    @Mock private MimoMultimodalClient multimodalClient;

    private PetAiReportService service;

    @BeforeEach
    void setUp() {
        service = new PetAiReportService(
                reportMapper,
                petMapper,
                alarmMapper,
                recordMapper,
                configMapper,
                currentUserService,
                multimodalClient,
                new ObjectMapper().findAndRegisterModules()
        );
    }

    @Test
    void generatesDetectionReportWithPetProfileAndSystemFacts() {
        LocalDateTime detectTime = LocalDateTime.of(2026, 6, 11, 21, 3);
        PetDetectionRecord record = new PetDetectionRecord();
        record.setId(12L);
        record.setDetectionConfigId(3L);
        record.setPetId(8L);
        record.setDeviceId(5L);
        record.setDeviceSerial("ABC123");
        record.setDetectTime(detectTime);
        record.setPetCoordX(82D);
        record.setPetCoordY(35D);
        record.setPetWidth(12D);
        record.setPetHeight(18D);
        record.setInSafeZone(0);
        record.setAlarmTriggered(1);
        record.setSnapshotUrl("https://example.com/pet.jpg");
        record.setAiResultJson("{\"confidence\":0.94}");

        PetDetectionConfig config = new PetDetectionConfig();
        config.setId(3L);
        config.setUserId(1L);
        config.setPetId(8L);

        Pet pet = new Pet();
        pet.setId(8L);
        pet.setUserId(1L);
        pet.setPetName("团子");
        pet.setPetType("CAT");
        pet.setAge(18);

        PetAiVisionResult result = new PetAiVisionResult();
        result.setRiskLevel("high");
        result.setSummary("团子位于安全区域外。");
        result.setObservedBehavior("画面中宠物靠近房间边缘。");
        result.setEvidenceBasis("系统记录显示越界并已触发告警。");
        result.setRecommendations(List.of("立即确认门窗状态"));
        result.setUncertainties(List.of("单帧无法判断移动方向"));

        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(recordMapper.selectById(12L)).thenReturn(record);
        when(configMapper.selectById(3L)).thenReturn(config);
        when(petMapper.selectById(8L)).thenReturn(pet);
        when(multimodalClient.getModel()).thenReturn("mimo-v2.5");
        when(multimodalClient.analyze(anyList(), anyString(), anyString())).thenReturn(result);
        doAnswer(invocation -> {
            PetAiReport report = invocation.getArgument(0);
            report.setId(99L);
            report.setCreatedAt(LocalDateTime.now());
            return 1;
        }).when(reportMapper).insert(any(PetAiReport.class));

        PetAiReportGenerateRequest request = new PetAiReportGenerateRequest();
        request.setSourceType("DETECTION");
        request.setSourceId(12L);

        var report = service.generate(request);

        assertThat(report.getId()).isEqualTo(99L);
        assertThat(report.getRiskLevel()).isEqualTo("HIGH");
        assertThat(report.getPetName()).isEqualTo("团子");
        assertThat(report.getRecommendations()).containsExactly("立即确认门窗状态");

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(multimodalClient).analyze(anyList(), anyString(), promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("18个月")
                .contains("\"inSafeZone\":false")
                .contains("\"alarmTriggered\":true");
    }
}
