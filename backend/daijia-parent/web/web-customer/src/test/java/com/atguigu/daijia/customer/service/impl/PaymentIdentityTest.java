package com.atguigu.daijia.customer.service.impl;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.login.GuiguLoginAspect;
import com.atguigu.daijia.common.util.AuthContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentIdentityTest {
    @Mock RedisTemplate<String, String> redis;
    @Mock ValueOperations<String, String> values;
    @Mock ProceedingJoinPoint action;
    @InjectMocks GuiguLoginAspect aspect;

    @BeforeEach
    void setup() {
        MockHttpServletRequest request = new MockHttpServletRequest(); request.addHeader("token", "test-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(redis.opsForValue()).thenReturn(values);
    }
    @AfterEach
    void cleanup() { AuthContextHolder.removeUserId(); RequestContextHolder.resetRequestAttributes(); }

    @Test
    void expiredTokenCannotReusePreviousRequestsIdentity() {
        AuthContextHolder.setUserId(99L);
        assertThrows(GuiguException.class, () -> aspect.login(action, null));
        assertNull(AuthContextHolder.getUserId()); verifyNoInteractions(action);
    }
    @Test
    void successfulRequestClearsIdentity() throws Throwable {
        when(values.get("user:login:test-token")).thenReturn("1");
        when(action.proceed()).thenAnswer(call -> { assertEquals(1L, AuthContextHolder.getUserId()); return "ok"; });
        assertEquals("ok", aspect.login(action, null)); assertNull(AuthContextHolder.getUserId());
    }
    @Test
    void failedRequestAlsoClearsIdentity() throws Throwable {
        when(values.get("user:login:test-token")).thenReturn("1");
        when(action.proceed()).thenThrow(new RuntimeException("test"));
        assertThrows(RuntimeException.class, () -> aspect.login(action, null)); assertNull(AuthContextHolder.getUserId());
    }
}
