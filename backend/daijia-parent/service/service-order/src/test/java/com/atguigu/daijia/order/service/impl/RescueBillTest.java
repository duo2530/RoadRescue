package com.atguigu.daijia.order.service.impl;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.entity.order.OrderBill;
import com.atguigu.daijia.model.vo.order.OrderPayVo;
import com.atguigu.daijia.order.mapper.OrderInfoMapper;
import com.atguigu.daijia.order.mapper.OrderBillMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RescueBillTest {
    @Mock OrderInfoMapper orders;
    @Mock OrderBillMapper bills;
    @InjectMocks OrderInfoServiceImpl service;
    OrderInfo order;
    @BeforeEach
    void setup() {
        order = new OrderInfo(); order.setId(16L); order.setCustomerId(1L); order.setOrderNo("order16");
        order.setStatus(6); order.setExpectDistance(new BigDecimal("15"));
        order.setRemark("{\"serviceType\":\"tow\",\"price\":0.01}");
    }
    @Test
    void legacyBillUsesServerRescuePricingInsteadOfClientPriceOrDaijiaAmount() {
        when(orders.selectOne(any())).thenReturn(order);
        OrderBill bill = new OrderBill(); bill.setTotalAmount(new BigDecimal("19.99"));
        bill.setTollFee(new BigDecimal("10.00")); bill.setCouponAmount(new BigDecimal("5.00"));
        when(bills.selectOne(any())).thenReturn(bill);
        OrderPayVo pay = new OrderPayVo(); pay.setPayAmount(new BigDecimal("410.00"));
        when(orders.selectOrderPayVo("order16", 1L)).thenReturn(pay);
        assertEquals(new BigDecimal("410.00"), service.preparePayment("order16", 1L).getPayAmount());
        assertEquals(new BigDecimal("415.00"), bill.getTotalAmount());
        assertEquals(7, order.getStatus());
        verify(bills).updateById(bill);
    }
    @Test
    void publishedBillIsFrozenAcrossCheckoutRetries() {
        order.setStatus(7);
        when(orders.selectOne(any())).thenReturn(order);
        OrderPayVo pay = new OrderPayVo(); pay.setPayAmount(new BigDecimal("180.00"));
        when(orders.selectOrderPayVo("order16", 1L)).thenReturn(pay);
        assertEquals(new BigDecimal("180.00"), service.preparePayment("order16", 1L).getPayAmount());
        verifyNoInteractions(bills);
        verify(orders, never()).updateById(any());
    }
    @Test
    void missingOrOtherCustomersOrderCannotBePrepared() {
        assertThrows(GuiguException.class, () -> service.preparePayment("order16", 2L));
        verifyNoInteractions(bills);
    }
    @Test
    void cancelledOrderCannotBecomePayable() {
        order.setStatus(-1); when(orders.selectOne(any())).thenReturn(order);
        assertThrows(GuiguException.class, () -> service.preparePayment("order16", 1L));
        verifyNoInteractions(bills);
    }
}
