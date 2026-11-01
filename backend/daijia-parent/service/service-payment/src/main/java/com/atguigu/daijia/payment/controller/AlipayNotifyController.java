package com.atguigu.daijia.payment.controller;

import com.atguigu.daijia.payment.service.AlipayPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
public class AlipayNotifyController {
    private final AlipayPaymentService service;
    public AlipayNotifyController(AlipayPaymentService service) { this.service = service; }

    @PostMapping(value = "/payment/alipay/notify", produces = "text/plain;charset=UTF-8")
    public String notifyPayment(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            if (entry.getValue().length != 1) return "failure";
            params.put(entry.getKey(), entry.getValue()[0]);
        }
        try { service.notifyPayment(params); return "success"; }
        catch (RuntimeException e) { return "failure"; }
    }
}
