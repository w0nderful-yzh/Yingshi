package com.yzh.yingshi.common.auth;

import com.yzh.yingshi.common.api.BusinessCode;
import com.yzh.yingshi.common.exception.BusinessException;
import com.yzh.yingshi.mapper.DeviceMapper;
import com.yzh.yingshi.mapper.UserDeviceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class CurrentUserServiceTest {

    @Test
    void requireCurrentUserIdReadsRequestAttribute() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 42L);
        CurrentUserService currentUserService = new CurrentUserService(
                request,
                mock(UserDeviceMapper.class),
                mock(DeviceMapper.class)
        );

        assertEquals(42L, currentUserService.requireCurrentUserId());
    }

    @Test
    void requireWriteAccessAllowsOperator() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("role", UserRole.OPERATOR);
        CurrentUserService currentUserService = new CurrentUserService(
                request,
                mock(UserDeviceMapper.class),
                mock(DeviceMapper.class)
        );

        assertDoesNotThrow(currentUserService::requireWriteAccess);
    }

    @Test
    void requireWriteAccessRejectsViewer() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("role", UserRole.VIEWER);
        CurrentUserService currentUserService = new CurrentUserService(
                request,
                mock(UserDeviceMapper.class),
                mock(DeviceMapper.class)
        );

        BusinessException exception = assertThrows(BusinessException.class, currentUserService::requireWriteAccess);
        assertEquals(BusinessCode.FORBIDDEN, exception.getBusinessCode());
    }
}
