package com.atguigu.daijia.payment.service;

import com.alipay.api.*;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.payment.config.AlipayProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@Component
public class AlipayGateway {
    private final AlipayProperties properties;
    public AlipayGateway(AlipayProperties properties) { this.properties = properties; }

    public void requireConfigured() {
        if (!properties.isEnabled() || !StringUtils.hasText(properties.getAppId())
                || !StringUtils.hasText(properties.getPrivateKey()) || !StringUtils.hasText(properties.getPublicKey())) {
            throw new GuiguException(503, "支付宝沙箱尚未配置");
        }
        // 本次只允许沙箱，防止复用配置时意外切到真实收款。
        if (!Set.of("https://openapi-sandbox.dl.alipaydev.com/gateway.do", "https://openapi.alipaydev.com/gateway.do")
                .contains(properties.getGatewayUrl())) throw new GuiguException(503, "当前仅启用支付宝沙箱支付");
    }

    protected AlipayClient client() {
        requireConfigured();
        return new DefaultAlipayClient(properties.getGatewayUrl(), properties.getAppId(), properties.getPrivateKey(),
                "json", "UTF-8", properties.getPublicKey(), "RSA2");
    }

    public String create(String orderNo, Long orderId, BigDecimal amount) {
        return create(orderNo, orderId, amount, null);
    }

    public String create(String orderNo, Long orderId, BigDecimal amount, String returnOrigin) {
        String returnUrl = properties.getH5ReturnUrl();
        if (StringUtils.hasText(returnOrigin)) {
            if (!properties.getAllowedReturnOrigins().contains(returnOrigin)) throw new GuiguException(210, "H5 支付回跳来源未配置");
            returnUrl = returnOrigin + "/#/pages/order/detail";
        }
        try {
            AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
            AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
            model.setOutTradeNo(orderNo);
            model.setTotalAmount(amount.toPlainString());
            model.setSubject("中道救援服务费（沙箱）");
            model.setProductCode("QUICK_WAP_WAY");
            request.setBizModel(model);
            if (StringUtils.hasText(properties.getNotifyUrl())) request.setNotifyUrl(properties.getNotifyUrl());
            request.setReturnUrl(returnUrl + "?id=" + orderId + "&paymentReturn=1");
            return client().pageExecute(request, "GET").getBody();
        } catch (AlipayApiException e) {
            throw new GuiguException(502, "支付宝收银台创建失败，请稍后重试");
        }
    }

    public AlipayTradeQueryResponse query(String orderNo) {
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            AlipayTradeQueryModel model = new AlipayTradeQueryModel();
            model.setOutTradeNo(orderNo);
            request.setBizModel(model);
            return client().execute(request);
        } catch (AlipayApiException e) {
            throw new GuiguException(502, "支付宝交易状态暂时无法查询，请稍后重试");
        }
    }

    public boolean verify(Map<String, String> params) {
        requireConfigured();
        try { return AlipaySignature.rsaCheckV1(new java.util.HashMap<>(params), properties.getPublicKey(), "UTF-8", "RSA2"); }
        catch (AlipayApiException e) { return false; }
    }
}
