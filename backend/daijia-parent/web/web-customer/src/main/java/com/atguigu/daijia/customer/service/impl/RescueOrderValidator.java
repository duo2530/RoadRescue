package com.atguigu.daijia.customer.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Set;

final class RescueOrderValidator {
    private static final Set<String> SERVICES = Set.of("tow", "battery", "tire", "charge");

    private RescueOrderValidator() {
    }

    static void validate(SubmitOrderForm form) {
        require(form != null, "请填写救援信息");
        require(StringUtils.hasText(form.getStartLocation())
                && validPoint(form.getStartPointLatitude(), form.getStartPointLongitude()), "请选择救援地点");

        JSONObject extra;
        try {
            extra = JSON.parseObject(form.getRemark());
        } catch (RuntimeException e) {
            throw new GuiguException(500, "救援信息格式错误");
        }
        require(extra != null, "请填写救援信息");
        String serviceType = extra.getString("serviceType");
        require(serviceType != null && SERVICES.contains(serviceType), "服务类型错误");
        require(StringUtils.hasText(form.getEndLocation())
                && validPoint(form.getEndPointLatitude(), form.getEndPointLongitude()),
                "tow".equals(serviceType) ? "请选择拖车终点" : "请选择救援地点");
        require(StringUtils.hasText(extra.getString("carType")), "请选择车型");
        require(StringUtils.hasText(extra.getString("position")), "请选择车辆当前位置");
        String plate = extra.getString("plate");
        require(StringUtils.hasText(plate), "请选择车牌号");
        require(plate.matches("^[一-龥][A-Z][A-Z0-9]{5,6}$"), "请输入正确的车牌号");
        String phone = extra.getString("phone");
        require(StringUtils.hasText(phone), "请输入联系电话");
        require(phone.matches("^1[0-9]{10}$"), "请输入正确的联系电话");
    }

    private static boolean validPoint(BigDecimal latitude, BigDecimal longitude) {
        return latitude != null && longitude != null
                && latitude.abs().compareTo(BigDecimal.valueOf(90)) <= 0
                && longitude.abs().compareTo(BigDecimal.valueOf(180)) <= 0;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new GuiguException(500, message);
    }
}
