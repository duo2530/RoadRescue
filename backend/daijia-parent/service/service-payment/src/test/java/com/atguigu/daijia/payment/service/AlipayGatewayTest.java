package com.atguigu.daijia.payment.service;

import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.payment.config.AlipayProperties;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AlipayGatewayTest {
    @Test
    void sdkSignsCheckoutAndVerifiesNotificationWithoutNetwork() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048);
        KeyPair keys = generator.generateKeyPair();
        String privateKey = Base64.getEncoder().encodeToString(keys.getPrivate().getEncoded());
        AlipayProperties properties = new AlipayProperties();
        properties.setEnabled(true); properties.setAppId("sandbox-app"); properties.setPrivateKey(privateKey);
        properties.setPublicKey(Base64.getEncoder().encodeToString(keys.getPublic().getEncoded()));
        AlipayGateway gateway = new AlipayGateway(properties);
        String checkout = gateway.create("order16", 16L, new BigDecimal("180.00"));
        assertTrue(checkout.startsWith(properties.getGatewayUrl()));
        assertTrue(checkout.contains("alipay.trade.wap.pay"));
        assertTrue(checkout.contains("sign="));
        Map<String, String> params = new HashMap<>(); params.put("app_id", "sandbox-app"); params.put("out_trade_no", "order16");
        params.put("total_amount", "180.00"); params.put("trade_status", "TRADE_SUCCESS");
        String sign = AlipaySignature.rsaSign(AlipaySignature.getSignContent(params), privateKey, "UTF-8", "RSA2");
        params.put("sign_type", "RSA2"); params.put("sign", sign);
        assertTrue(gateway.verify(new HashMap<>(params)));
        params.put("total_amount", "0.01");
        assertFalse(gateway.verify(new HashMap<>(params)));
        properties.setGatewayUrl("https://openapi.alipay.com/gateway.do");
        assertThrows(GuiguException.class, gateway::requireConfigured);
    }
}
