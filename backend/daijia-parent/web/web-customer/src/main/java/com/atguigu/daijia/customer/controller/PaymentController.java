package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.login.GuiguLogin;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.map.client.AlipayFeignClient;
import com.atguigu.daijia.model.form.payment.CreatePaymentForm;
import com.atguigu.daijia.model.vo.payment.PaymentSessionVo;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order/payment")
public class PaymentController {
    private final AlipayFeignClient alipay;
    public PaymentController(AlipayFeignClient alipay) { this.alipay = alipay; }

    @GuiguLogin
    @PostMapping("/create")
    public Result<PaymentSessionVo> create(@RequestBody CreatePaymentForm form) {
        if ("WECHAT_MINI".equals(form.getChannel())) throw new GuiguException(501, "小程序微信支付接口已预留，暂未启用");
        if (!"ALIPAY_WAP_SANDBOX".equals(form.getChannel())) throw new GuiguException(210, "不支持的支付渠道");
        form.setCustomerId(AuthContextHolder.getUserId());
        return alipay.create(form);
    }

    @GuiguLogin
    @GetMapping("/status/{orderNo}")
    public Result<Boolean> status(@PathVariable String orderNo) {
        return alipay.query(orderNo, AuthContextHolder.getUserId());
    }
}
