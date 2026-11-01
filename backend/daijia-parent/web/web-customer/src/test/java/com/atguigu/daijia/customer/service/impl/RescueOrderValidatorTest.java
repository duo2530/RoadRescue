package com.atguigu.daijia.customer.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RescueOrderValidatorTest {
    private SubmitOrderForm validForm() {
        SubmitOrderForm form = new SubmitOrderForm();
        form.setStartLocation("同景苑");
        form.setStartPointLatitude(new BigDecimal("31.23"));
        form.setStartPointLongitude(new BigDecimal("121.65"));
        form.setEndLocation("如家酒店(金高路)");
        form.setEndPointLatitude(new BigDecimal("31.28"));
        form.setEndPointLongitude(new BigDecimal("121.60"));
        form.setRemark("{\"serviceType\":\"tow\",\"carType\":\"9座以下（含9座）客车\","
                + "\"position\":\"地面\",\"plate\":\"沪B8K6T9\",\"phone\":\"13000000000\"}");
        return form;
    }

    @ParameterizedTest
    @CsvSource({"plate,请选择车牌号", "phone,请输入联系电话", "carType,请选择车型", "position,请选择车辆当前位置"})
    void missingFieldsAreRejectedBeforeRemoteCalls(String field, String message) {
        SubmitOrderForm form = validForm();
        JSONObject extra = JSON.parseObject(form.getRemark());
        extra.put(field, " ");
        form.setRemark(extra.toJSONString());
        // 无远程依赖的 service：若校验前调用算路，此测试会因空指针失败。
        GuiguException error = assertThrows(GuiguException.class, () -> new OrderServiceImpl().submitOrder(form));
        assertEquals(message, error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "{broken", "[]"})
    void missingOrMalformedRescueInfoIsRejected(String remark) {
        SubmitOrderForm form = validForm();
        form.setRemark(remark);
        assertThrows(GuiguException.class, () -> RescueOrderValidator.validate(form));
    }

    @ParameterizedTest
    @CsvSource({"phone,123,请输入正确的联系电话", "plate,ABC,请输入正确的车牌号", "serviceType,unknown,服务类型错误"})
    void malformedFieldsAreRejected(String field, String value, String message) {
        SubmitOrderForm form = validForm();
        JSONObject extra = JSON.parseObject(form.getRemark());
        extra.put(field, value);
        form.setRemark(extra.toJSONString());
        assertEquals(message, assertThrows(GuiguException.class,
                () -> RescueOrderValidator.validate(form)).getMessage());
    }

    @Test
    void missingAndInvalidCoordinatesAreRejected() {
        SubmitOrderForm form = validForm();
        form.setStartPointLatitude(null);
        assertThrows(GuiguException.class, () -> RescueOrderValidator.validate(form));
        SubmitOrderForm invalidEnd = validForm();
        invalidEnd.setEndPointLongitude(BigDecimal.valueOf(181));
        assertThrows(GuiguException.class, () -> RescueOrderValidator.validate(invalidEnd));
    }

    @ParameterizedTest
    @ValueSource(strings = {"tow", "battery", "tire", "charge"})
    void completeOrdersPass(String serviceType) {
        SubmitOrderForm form = validForm();
        JSONObject extra = JSON.parseObject(form.getRemark());
        extra.put("serviceType", serviceType);
        extra.put("plate", "沪AD12345");
        form.setRemark(extra.toJSONString());
        assertDoesNotThrow(() -> RescueOrderValidator.validate(form));
    }
}
