package com.atguigu.daijia.dispatch.service.impl;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverOrderQueueTest {
    @Mock RedisTemplate<String, String> redisTemplate;
    @Mock ListOperations<String, String> queue;
    @Mock OrderInfoFeignClient orderInfoFeignClient;
    @InjectMocks NewOrderServiceImpl service;
    private static final String KEY = "driver:order:temp:list:1";
    private static final String MESSAGE = "{\"orderId\":13,\"distance\":13.03}";
    private OrderInfo order;

    @BeforeEach
    void setup() {
        when(redisTemplate.opsForList()).thenReturn(queue);
        when(queue.range(KEY, 0, -1)).thenReturn(List.of(MESSAGE));
        order = new OrderInfo();
        order.setId(13L);
        order.setStatus(1);
        order.setRemark("{\"serviceType\":\"battery\",\"price\":139}");
        order.setStartPointLatitude(BigDecimal.valueOf(31.24));
        when(orderInfoFeignClient.getOrderInfo(13L)).thenReturn(Result.ok(order));
    }

    @Test
    void repeatedRefreshKeepsWaitingOrderAndRescueDetails() {
        for (int i = 0; i < 2; i++) {
            var result = service.findNewOrderQueueData(1L);
            assertEquals(1, result.size());
            assertEquals(13L, result.get(0).getOrderId());
            assertEquals(order.getRemark(), result.get(0).getRemark());
            assertEquals(order.getStartPointLatitude(), result.get(0).getStartPointLatitude());
            assertEquals(new BigDecimal("13.03"), result.get(0).getDistance());
        }
        verify(queue, never()).leftPop(anyString());
        verify(queue, never()).remove(anyString(), anyLong(), any());
    }

    @Test
    void acceptedOrderIsRemovedFromHall() {
        order.setStatus(2);
        assertTrue(service.findNewOrderQueueData(1L).isEmpty());
        verify(queue).remove(KEY, 1, MESSAGE);
    }

    @Test
    void cancelledOrderIsRemovedFromHall() {
        order.setStatus(-1);
        assertTrue(service.findNewOrderQueueData(1L).isEmpty());
        verify(queue).remove(KEY, 1, MESSAGE);
    }
}
