package com.atguigu.daijia.model.form.driver;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DriverPhoneForm {

    @Schema(description = "手机号码")
    private String phone;
}
