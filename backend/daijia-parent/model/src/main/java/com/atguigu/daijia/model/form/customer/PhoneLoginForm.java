package com.atguigu.daijia.model.form.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PhoneLoginForm {

    @Schema(description = "手机号码")
    private String phone;

    @Schema(description = "6位数字验证码")
    private String code;
}
