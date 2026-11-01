package com.atguigu.daijia.model.form.payment;

import lombok.Data;

@Data
public class CreatePaymentForm {
    private String orderNo;
    /** ALIPAY_WAP_SANDBOX / WECHAT_MINI（预留，当前不启用） */
    private String channel;
    /** 仅接受服务端白名单内的 H5 来源，不接受任意回跳地址。 */
    private String returnOrigin;
    /** 由登录上下文填写，忽略客户端传入的身份。 */
    private Long customerId;
}
