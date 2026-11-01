package com.atguigu.daijia.order.service.impl;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.order.mapper.OrderInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrabOrderTest {
    @Mock OrderInfoMapper mapper;
    @Mock RedissonClient redissonClient;
    @Mock RedisTemplate redisTemplate;
    @Mock RLock lock;
    @InjectMocks OrderInfoServiceImpl service;

    @BeforeAll
    static void initMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), OrderInfo.class);
    }

    @BeforeEach
    void setup() {
        when(redissonClient.getLock("rob:new:order:lock13")).thenReturn(lock);
    }

    @Test
    void acceptsWaitingOrderWithoutDependingOnSharedRedisMarker() throws Exception {
        when(lock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(mapper.update(any(OrderInfo.class), any(LambdaQueryWrapper.class))).thenReturn(1);
        assertTrue(service.robNewOrder(1L, 13L));
        ArgumentCaptor<OrderInfo> update = ArgumentCaptor.forClass(OrderInfo.class);
        ArgumentCaptor<LambdaQueryWrapper> condition = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).update(update.capture(), condition.capture());
        assertEquals(2, update.getValue().getStatus());
        assertEquals(1L, update.getValue().getDriverId());
        assertNotNull(update.getValue().getAcceptTime());
        assertTrue(condition.getValue().getSqlSegment().contains("status"));
        assertTrue(condition.getValue().getSqlSegment().contains("driver_id IS NULL"));
        verify(redisTemplate).delete("order:accept:mark:13");
        verify(redisTemplate, never()).hasKey(any());
        verify(lock).unlock();
    }

    @Test
    void lockTimeoutIsFailureAndDoesNotUnlockAnotherThread() throws Exception {
        when(lock.tryLock(1, TimeUnit.SECONDS)).thenReturn(false);
        assertThrows(GuiguException.class, () -> service.robNewOrder(1L, 13L));
        verifyNoInteractions(mapper);
        verify(lock, never()).unlock();
    }

    @Test
    void alreadyTakenOrderCannotBeOverwritten() throws Exception {
        when(lock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
        OrderInfo existing = new OrderInfo();
        existing.setDriverId(2L);
        existing.setStatus(2);
        when(mapper.selectById(13L)).thenReturn(existing);
        assertThrows(GuiguException.class, () -> service.robNewOrder(1L, 13L));
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void repeatRequestBySameDriverIsIdempotent() throws Exception {
        when(lock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
        OrderInfo existing = new OrderInfo();
        existing.setDriverId(1L);
        existing.setStatus(2);
        when(mapper.selectById(13L)).thenReturn(existing);
        assertTrue(service.robNewOrder(1L, 13L));
    }

    @Test
    void cancelledOrderIsRejected() throws Exception {
        when(lock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
        OrderInfo existing = new OrderInfo();
        existing.setStatus(-1);
        when(mapper.selectById(13L)).thenReturn(existing);
        assertThrows(GuiguException.class, () -> service.robNewOrder(1L, 13L));
    }

    @Test
    void timeoutCancellationOnlyUpdatesWaitingOrderAndOwnMarker() {
        // 此测试不调用抢单锁。
        reset(redissonClient);
        when(mapper.update(any(OrderInfo.class), any(LambdaQueryWrapper.class))).thenReturn(1);
        service.orderCancel(12L);
        verify(redisTemplate).delete("order:accept:mark:12");
        verify(redisTemplate, never()).delete("order:accept:mark:13");
        verify(redisTemplate, never()).delete("order:accept:mark:");
    }
}
