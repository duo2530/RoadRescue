package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.DriverPhoneLoginForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;

public interface DriverService {

    String login(String code);

    //发送手机验证码，返回提示文案
    String sendSmsCode(String phone);

    //手机号验证码登录，返回 token
    String loginByPhone(DriverPhoneLoginForm form);

    //司机认证信息
    DriverAuthInfoVo getDriverAuthInfo(Long driverId);

    //更新司机认证信息
    Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm);

    //创建司机人脸模型
    Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm);

    //判断司机人脸识别
    Boolean isFaceRecognition(Long driverId);

    //人脸识别
    Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm);

    //开始接单服务
    Boolean startService(Long driverId);

    //停止接单服务
    Boolean stopService(Long driverId);
}
