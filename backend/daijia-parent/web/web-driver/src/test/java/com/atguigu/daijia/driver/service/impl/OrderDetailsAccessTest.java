package com.atguigu.daijia.driver.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.dispatch.client.NewOrderFeignClient;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderDetailsAccessTest {
    @Mock OrderInfoFeignClient orderInfoFeignClient;
    @Mock NewOrderFeignClient newOrderFeignClient;
    @InjectMocks OrderServiceImpl service;
    private OrderInfo order;

    @BeforeEach
    void setup() {
        order = new OrderInfo();
        order.setId(13L);
        order.setStatus(1);
        order.setRemark("{\"serviceType\":\"tow\",\"price\":180,\"phone\":\"13000000000\"}");
    }

    @Test
    void dispatchedDriverCanPreviewWaitingOrderWithoutContact() {
        when(orderInfoFeignClient.getOrderInfo(13L)).thenReturn(Result.ok(order));
        NewOrderDataVo item = new NewOrderDataVo();
        item.setOrderId(13L);
        when(newOrderFeignClient.findNewOrderQueueData(1L)).thenReturn(Result.ok(List.of(item)));
        var detail = service.getOrderInfo(13L, 1L);
        assertFalse(detail.getMine());
        assertEquals("tow", JSON.parseObject(detail.getRemark()).getString("serviceType"));
        assertFalse(JSON.parseObject(detail.getRemark()).containsKey("phone"));
        assertTrue(JSON.parseObject(order.getRemark()).containsKey("phone"));
        assertNull(detail.getOrderBillVo());
    }

    @Test
    void ownerWithIdOutsideLongCacheCanViewFullDetails() {
        order.setDriverId(Long.valueOf("1000"));
        order.setStatus(2);
        when(orderInfoFeignClient.getOrderInfo(13L)).thenReturn(Result.ok(order));
        var detail = service.getOrderInfo(13L, Long.valueOf("1000"));
        assertTrue(detail.getMine());
        assertEquals("13000000000", JSON.parseObject(detail.getRemark()).getString("phone"));
        verifyNoInteractions(newOrderFeignClient);
    }

    @Test
    void undispatchedDriverCannotPreviewOrder() {
        when(orderInfoFeignClient.getOrderInfo(13L)).thenReturn(Result.ok(order));
        when(newOrderFeignClient.findNewOrderQueueData(2L)).thenReturn(Result.ok(List.of()));
        assertEquals(205, assertThrows(GuiguException.class, () -> service.getOrderInfo(13L, 2L)).getCode());
    }

    @Test
    void anotherDriversOrderRemainsForbidden() {
        order.setDriverId(2L);
        order.setStatus(2);
        when(orderInfoFeignClient.getOrderInfo(13L)).thenReturn(Result.ok(order));
        assertThrows(GuiguException.class, () -> service.getOrderInfo(13L, 1L));
        verifyNoInteractions(newOrderFeignClient);
    }

    @Test
    void cancelledOrderCannotBePreviewed() {
        order.setStatus(-1);
        when(orderInfoFeignClient.getOrderInfo(13L)).thenReturn(Result.ok(order));
        assertThrows(GuiguException.class, () -> service.getOrderInfo(13L, 1L));
        verifyNoInteractions(newOrderFeignClient);
    }

    @Test
    void missingOrderIsHandled() {
        when(orderInfoFeignClient.getOrderInfo(13L)).thenReturn(Result.ok(null));
        assertThrows(GuiguException.class, () -> service.getOrderInfo(13L, 1L));
    }

    @Test
    void hallListAlsoHidesContact() {
        NewOrderDataVo item = new NewOrderDataVo();
        item.setRemark(order.getRemark());
        when(newOrderFeignClient.findNewOrderQueueData(1L)).thenReturn(Result.ok(List.of(item)));
        var list = service.findNewOrderQueueData(1L);
        assertFalse(JSON.parseObject(list.get(0).getRemark()).containsKey("phone"));
    }
}
