package com.yzh.yingshi.common.interceptor;

import com.yzh.yingshi.common.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class JwtAuthInterceptorTest {

    private final JwtAuthInterceptor interceptor = new JwtAuthInterceptor(mock(JwtUtil.class));

    @Test
    void allowsPublicEzvizGetCallback() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ezviz/oauth/callback");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void protectsFrontendEzvizPostCallback() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/ezviz/oauth/callback");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
    }
}
