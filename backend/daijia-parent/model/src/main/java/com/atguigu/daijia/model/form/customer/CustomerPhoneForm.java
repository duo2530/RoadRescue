package com.atguigu.daijia.model.form.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CustomerPhoneForm {

    @Schema(description = "手机号码")
    private String phone;
}
