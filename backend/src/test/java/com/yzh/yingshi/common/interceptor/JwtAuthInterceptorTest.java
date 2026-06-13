package com.yzh.yingshi.common.interceptor;

import com.yzh.yingshi.common.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class JwtAuthInterceptorTest {

    private JwtUtil jwtUtil;
    private JwtAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        interceptor = new JwtAuthInterceptor(jwtUtil);
    }

    @Test
    void publicOAuthGetCallbackBypassesJwt() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/ezviz/oauth/callback");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void publicWebhookPostBypassesJwt() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/ezviz/webhook");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void authenticatedOAuthPostCallbackStillRequiresJwt() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/ezviz/oauth/callback");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
        verifyNoInteractions(jwtUtil);
    }
}
