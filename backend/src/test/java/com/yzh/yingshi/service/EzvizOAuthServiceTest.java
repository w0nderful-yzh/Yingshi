package com.yzh.yingshi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzh.yingshi.common.api.BusinessCode;
import com.yzh.yingshi.common.exception.BusinessException;
import com.yzh.yingshi.config.EzvizProperties;
import com.yzh.yingshi.mapper.DeviceMapper;
import com.yzh.yingshi.mapper.UserDeviceMapper;
import com.yzh.yingshi.mapper.UserEzvizAccountMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class EzvizOAuthServiceTest {

    private EzvizOAuthService service;

    @BeforeEach
    void setUp() {
        EzvizProperties properties = new EzvizProperties();
        properties.setBaseUrl("https://open.ys7.com");
        properties.setAppKey("test-app-key");
        properties.getOauth().setRedirectUri("https://example.com/api/ezviz/oauth/callback");
        service = new EzvizOAuthService(
                properties,
                mock(UserEzvizAccountMapper.class),
                mock(UserDeviceMapper.class),
                mock(DeviceMapper.class),
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(service, "jwtSecret", "test-secret-with-at-least-32-characters");
    }

    @Test
    void generatedStateIsBoundToUser() {
        String state = service.generateAuthUrl(42L).getState();

        assertEquals(42L, service.parseUserIdFromState(state));
        service.verifyCallbackState(42L, state);
    }

    @Test
    void callbackStateRejectsDifferentUser() {
        String state = service.generateAuthUrl(42L).getState();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.verifyCallbackState(7L, state)
        );
        assertEquals(BusinessCode.FORBIDDEN, exception.getBusinessCode());
    }

    @Test
    void tamperedStateIsRejected() {
        String state = service.generateAuthUrl(42L).getState();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.parseUserIdFromState(state.replace("::42::", "::7::"))
        );
        assertEquals(BusinessCode.PARAM_INVALID, exception.getBusinessCode());
    }
}
