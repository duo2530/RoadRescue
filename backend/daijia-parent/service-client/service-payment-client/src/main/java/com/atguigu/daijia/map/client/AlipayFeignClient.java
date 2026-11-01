package com.atguigu.daijia.map.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.form.payment.CreatePaymentForm;
import com.atguigu.daijia.model.vo.payment.PaymentSessionVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(value = "service-payment", contextId = "alipayPayment")
public interface AlipayFeignClient {
    @PostMapping("/internal/alipay/create")
    Result<PaymentSessionVo> create(@RequestBody CreatePaymentForm form);

    @GetMapping("/internal/alipay/query/{orderNo}/{customerId}")
    Result<Boolean> query(@PathVariable("orderNo") String orderNo, @PathVariable("customerId") Long customerId);
}
