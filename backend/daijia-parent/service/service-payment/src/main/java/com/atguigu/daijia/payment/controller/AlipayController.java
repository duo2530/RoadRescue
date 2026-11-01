package com.atguigu.daijia.payment.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.form.payment.CreatePaymentForm;
import com.atguigu.daijia.model.vo.payment.PaymentSessionVo;
import com.atguigu.daijia.payment.service.AlipayPaymentService;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/internal/alipay")
public class AlipayController {
    private final AlipayPaymentService service;
    public AlipayController(AlipayPaymentService service) { this.service = service; }

    @PostMapping("/create")
    public Result<PaymentSessionVo> create(@RequestBody CreatePaymentForm form) { return Result.ok(service.create(form)); }

    @GetMapping("/query/{orderNo}/{customerId}")
    public Result<Boolean> query(@PathVariable String orderNo, @PathVariable Long customerId) { return Result.ok(service.query(orderNo, customerId)); }

    /* 异步通知由独立的公开端点接收，内部创建/查询不暴露到网关。 */
}
