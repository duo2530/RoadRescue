package com.atguigu.daijia.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "zhongdao.payment.alipay")
public class AlipayProperties {
    private boolean enabled;
    private String gatewayUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    private String appId;
    private String privateKey;
    private String publicKey;
    private String sellerId;
    private String notifyUrl;
    private String h5ReturnUrl = "http://localhost:9090/#/pages/order/detail";
    private java.util.List<String> allowedReturnOrigins = java.util.List.of("http://localhost:9090", "http://127.0.0.1:9090");
}
