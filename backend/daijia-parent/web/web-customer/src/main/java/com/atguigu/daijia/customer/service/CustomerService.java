package com.atguigu.daijia.customer.service;

import com.atguigu.daijia.model.form.customer.PhoneLoginForm;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;

public interface CustomerService {

    //微信登录
    String login(String code);

    //获取用户信息
    CustomerLoginVo getCustomerLoginInfo(String token);

    //获取用户信息
    CustomerLoginVo getCustomerInfo(Long customerId);

    //更新用户微信手机号
    Boolean updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm);

    //发送手机验证码，返回提示文案
    String sendSmsCode(String phone);

    //手机号验证码登录，返回 token
    String loginByPhone(PhoneLoginForm form);
}
