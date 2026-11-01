package com.atguigu.daijia.payment.service;

import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.entity.payment.PaymentInfo;
import com.atguigu.daijia.model.form.payment.CreatePaymentForm;
import com.atguigu.daijia.model.vo.order.OrderPayVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.atguigu.daijia.payment.config.AlipayProperties;
import com.atguigu.daijia.payment.mapper.PaymentInfoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class AlipayPaymentServiceTest {
    @Mock PaymentInfoMapper mapper;
    @Mock OrderInfoFeignClient orders;
    @Mock AlipayGateway gateway;
    @Mock RedissonClient redisson;
    @Mock RLock lock;
    AlipayPaymentService service;
    AlipayProperties properties;
    OrderPayVo order;
    PaymentInfo record;

    @BeforeEach
    void setup() throws Exception {
        properties = new AlipayProperties();
        properties.setAppId("sandbox-app");
        service = new AlipayPaymentService(mapper, orders, gateway, properties, redisson);
        lenient().when(redisson.getLock(anyString())).thenReturn(lock);
        lenient().when(lock.tryLock(3, TimeUnit.SECONDS)).thenReturn(true);
        order = new OrderPayVo();
        order.setOrderId(16L); order.setOrderNo("order16"); order.setCustomerId(1L);
        order.setStatus(7); order.setPayAmount(new BigDecimal("180.00"));
        record = new PaymentInfo(); record.setId(1L); record.setOrderNo("order16");
        record.setPayWay(2); record.setPaymentStatus(0); record.setAmount(new BigDecimal("180.00"));
    }

    @Test
    void createsCheckoutUsingServerBillAmount() {
        when(orders.preparePayment("order16", 1L)).thenReturn(Result.ok(order));
        when(gateway.create("order16", 16L, new BigDecimal("180.00"), null)).thenReturn("https://openapi-sandbox.dl.alipaydev.com/gateway.do?signed=1");
        CreatePaymentForm form = new CreatePaymentForm();
        form.setOrderNo("order16"); form.setCustomerId(1L); form.setChannel("ALIPAY_WAP_SANDBOX");
        assertEquals("ALIPAY_WAP_SANDBOX", service.create(form).getChannel());
        ArgumentCaptor<PaymentInfo> inserted = ArgumentCaptor.forClass(PaymentInfo.class);
        verify(mapper).insert(inserted.capture());
        assertEquals(new BigDecimal("180.00"), inserted.getValue().getAmount());
        assertEquals(2, inserted.getValue().getPayWay());
        verify(orders, never()).updateOrderPayStatus(anyString());
    }

    @Test
    void cannotPayAnotherCustomersOrder() {
        when(orders.preparePayment("order16", 2L)).thenReturn(Result.build(null, 205, "无权支付"));
        CreatePaymentForm form = new CreatePaymentForm();
        form.setOrderNo("order16"); form.setCustomerId(2L); form.setChannel("ALIPAY_WAP_SANDBOX");
        assertThrows(GuiguException.class, () -> service.create(form));
        verifyNoInteractions(mapper);
    }

    private Map<String, String> notification() {
        Map<String, String> params = new HashMap<>();
        params.put("app_id", "sandbox-app"); params.put("out_trade_no", "order16");
        params.put("trade_no", "alipay-trade-1"); params.put("total_amount", "180.00");
        params.put("trade_status", "TRADE_SUCCESS");
        return params;
    }

    @Test
    void invalidSignatureCannotChangeOrder() {
        assertThrows(GuiguException.class, () -> service.notifyPayment(notification()));
        verifyNoInteractions(mapper, orders);
    }

    @Test
    void wrongApplicationCannotChangeOrder() {
        Map<String, String> params = notification(); params.put("app_id", "other-app");
        when(gateway.verify(params)).thenReturn(true);
        assertThrows(GuiguException.class, () -> service.notifyPayment(params));
        verifyNoInteractions(mapper, orders);
    }

    @Test
    void wrongAmountCannotChangeOrder() {
        Map<String, String> params = notification(); params.put("total_amount", "0.01");
        when(gateway.verify(params)).thenReturn(true);
        when(mapper.selectOne(any())).thenReturn(record);
        assertThrows(GuiguException.class, () -> service.notifyPayment(params));
        verify(orders, never()).updateOrderPayStatus(anyString());
    }

    @Test
    void validAndDuplicateNotificationsAreIdempotent() {
        Map<String, String> params = notification();
        when(gateway.verify(params)).thenReturn(true);
        when(mapper.selectOne(any())).thenReturn(record);
        when(orders.updateOrderPayStatus("order16")).thenReturn(Result.ok(true));
        when(mapper.updateById(record)).thenReturn(1);
        service.notifyPayment(params); service.notifyPayment(params);
        assertEquals(1, record.getPaymentStatus());
        assertEquals("alipay-trade-1", record.getTransactionId());
        verify(mapper, times(1)).updateById(record);
    }

    @Test
    void downstreamFailureDoesNotMarkPaymentComplete() {
        Map<String, String> params = notification();
        when(gateway.verify(params)).thenReturn(true);
        when(mapper.selectOne(any())).thenReturn(record);
        when(orders.updateOrderPayStatus("order16")).thenReturn(Result.build(null, 204, "更新失败"));
        assertThrows(GuiguException.class, () -> service.notifyPayment(params));
        assertEquals(0, record.getPaymentStatus());
        verify(mapper, never()).updateById(any());
    }

    @Test
    void activeQueryConfirmsSuccessfulTradeWithMatchingAmount() {
        when(orders.getOrderPayVo("order16", 1L)).thenReturn(Result.ok(order));
        when(mapper.selectOne(any())).thenReturn(record);
        AlipayTradeQueryResponse response = new AlipayTradeQueryResponse();
        response.setCode("10000"); response.setTradeStatus("TRADE_SUCCESS");
        response.setOutTradeNo("order16"); response.setTradeNo("alipay-trade-1"); response.setTotalAmount("180.00");
        when(gateway.query("order16")).thenReturn(response);
        when(orders.updateOrderPayStatus("order16")).thenReturn(Result.ok(true));
        when(mapper.updateById(record)).thenReturn(1);
        assertTrue(service.query("order16", 1L));
    }

    @Test
    void nonexistentSandboxTradeRemainsUnpaid() {
        when(orders.getOrderPayVo("order16", 1L)).thenReturn(Result.ok(order));
        when(mapper.selectOne(any())).thenReturn(record);
        AlipayTradeQueryResponse response = new AlipayTradeQueryResponse();
        response.setCode("40004"); response.setSubCode("ACQ.TRADE_NOT_EXIST");
        when(gateway.query("order16")).thenReturn(response);
        assertFalse(service.query("order16", 1L));
        verify(orders, never()).updateOrderPayStatus(anyString());
    }
}
