package com.atguigu.daijia.model.vo.payment;

import lombok.Data;

@Data
public class PaymentSessionVo {
    private String channel;
    private String redirectUrl;
    private String orderNo;
}
