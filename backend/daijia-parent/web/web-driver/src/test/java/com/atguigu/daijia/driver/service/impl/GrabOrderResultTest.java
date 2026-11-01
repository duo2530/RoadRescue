package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrabOrderResultTest {
    @Mock OrderInfoFeignClient client;
    @InjectMocks OrderServiceImpl service;

    @Test
    void downstreamFailureIsNotWrappedAsSuccess() {
        when(client.robNewOrder(1L, 13L)).thenReturn(Result.build(null, 217, "订单已失效"));
        var error = assertThrows(GuiguException.class, () -> service.robNewOrder(1L, 13L));
        assertEquals(217, error.getCode());
        assertEquals("订单已失效", error.getMessage());
    }

    @Test
    void falseAndNullResultsAreNotSuccess() {
        when(client.robNewOrder(1L, 13L)).thenReturn(Result.ok(false), Result.ok(null));
        assertThrows(GuiguException.class, () -> service.robNewOrder(1L, 13L));
        assertThrows(GuiguException.class, () -> service.robNewOrder(1L, 13L));
    }

    @Test
    void trueResultIsSuccess() {
        when(client.robNewOrder(1L, 13L)).thenReturn(Result.ok(true));
        assertTrue(service.robNewOrder(1L, 13L));
    }
}
