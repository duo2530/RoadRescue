package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.dispatch.client.NewOrderFeignClient;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.DriverService;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.DriverPhoneForm;
import com.atguigu.daijia.model.form.driver.DriverPhoneLoginForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverServiceImpl implements DriverService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");
    private static final Pattern SMS_CODE_PATTERN = Pattern.compile("^\\d{6}$");

    //演示验证码，没有短信服务商时使用；生产环境必须清空，否则任何人知道手机号即可登录
    @Value("${roadrescue.sms.demo-code:}")
    private String demoSmsCode;

    @Autowired
    private DriverInfoFeignClient driverInfoFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private LocationFeignClient locationFeignClient;

    @Autowired
    private NewOrderFeignClient newOrderFeignClient;

    //登录
    @Override
    public String login(String code) {
        //远程调用，得到司机id
        Result<Long> longResult = driverInfoFeignClient.login(code);
        //TODO 判断
        Long driverId = longResult.getData();
        return issueToken(driverId);
    }

    @Override
    public String sendSmsCode(String phone) {
        if(!isValidPhone(phone)) {
            throw fail(ResultCodeEnum.ARGUMENT_VALID_ERROR, "手机号格式不正确");
        }
        if(!StringUtils.hasText(demoSmsCode)) {
            throw fail(ResultCodeEnum.SERVICE_ERROR, "短信服务未开通");
        }

        //同一手机号 60 秒内只能发一次。setIfAbsent 是原子操作，并发请求只有一个能拿到限制标记
        Boolean firstInWindow = redisTemplate.opsForValue().setIfAbsent(
                RedisConstant.SMS_DRIVER_SEND_LIMIT_KEY_PREFIX + phone, "1",
                RedisConstant.SMS_DRIVER_SEND_LIMIT_TIMEOUT, TimeUnit.SECONDS);
        if(!Boolean.TRUE.equals(firstInWindow)) {
            throw fail(ResultCodeEnum.FAIL, "发送过于频繁，请60秒后再试");
        }

        redisTemplate.opsForValue().set(codeKey(phone), demoSmsCode,
                RedisConstant.SMS_DRIVER_CODE_TIMEOUT, TimeUnit.SECONDS);
        //新验证码从零开始计错误次数
        redisTemplate.delete(failKey(phone));

        return "演示验证码：" + demoSmsCode;
    }

    @Override
    public String loginByPhone(DriverPhoneLoginForm form) {
        String phone = form.getPhone();
        String code = form.getCode();
        if(!isValidPhone(phone)) {
            throw fail(ResultCodeEnum.ARGUMENT_VALID_ERROR, "手机号格式不正确");
        }
        if(code == null || !SMS_CODE_PATTERN.matcher(code).matches()) {
            throw fail(ResultCodeEnum.ARGUMENT_VALID_ERROR, "验证码应为6位数字");
        }

        String stored = (String) redisTemplate.opsForValue().get(codeKey(phone));
        if(stored == null) {
            throw fail(ResultCodeEnum.PHONE_CODE_ERROR, "验证码已过期或未发送，请重新获取");
        }

        if(!code.equals(stored)) {
            recordFailure(phone);
            throw fail(ResultCodeEnum.PHONE_CODE_ERROR, "验证码错误");
        }

        //删除成功才算消费验证码，两次并发提交同一正确验证码时只有一次能通过
        if(!Boolean.TRUE.equals(redisTemplate.delete(codeKey(phone)))) {
            throw fail(ResultCodeEnum.PHONE_CODE_ERROR, "验证码已过期或未发送，请重新获取");
        }
        redisTemplate.delete(failKey(phone));

        DriverPhoneForm driverPhoneForm = new DriverPhoneForm();
        driverPhoneForm.setPhone(phone);
        Result<Long> loginResult = driverInfoFeignClient.loginByPhone(driverPhoneForm);
        if(!ResultCodeEnum.SUCCESS.getCode().equals(loginResult.getCode())) {
            throw new GuiguException(loginResult.getCode(), loginResult.getMessage());
        }

        Long driverId = loginResult.getData();
        if(driverId == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        return issueToken(driverId);
    }

    //签发登录 token，微信登录和手机号登录共用
    private String issueToken(Long driverId) {
        String token = UUID.randomUUID().toString().replaceAll("-","");
        redisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX + token,
                                             driverId.toString(),
                                             RedisConstant.USER_LOGIN_KEY_TIMEOUT,
                                             TimeUnit.SECONDS);
        return token;
    }

    //错误计数 key 的 TTL 跟随验证码剩余时间，验证码过期或被删除后计数自动消失
    private void recordFailure(String phone) {
        Long fails = redisTemplate.opsForValue().increment(failKey(phone));
        Long remaining = redisTemplate.getExpire(codeKey(phone), TimeUnit.SECONDS);
        if(remaining != null && remaining > 0) {
            redisTemplate.expire(failKey(phone), remaining, TimeUnit.SECONDS);
        }
        if(fails != null && fails >= RedisConstant.SMS_DRIVER_MAX_FAIL_TIMES) {
            redisTemplate.delete(codeKey(phone));
            redisTemplate.delete(failKey(phone));
        }
    }

    private static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    private static String codeKey(String phone) {
        return RedisConstant.SMS_DRIVER_CODE_KEY_PREFIX + phone;
    }

    private static String failKey(String phone) {
        return RedisConstant.SMS_DRIVER_FAIL_KEY_PREFIX + phone;
    }

    private static GuiguException fail(ResultCodeEnum resultCodeEnum, String message) {
        return new GuiguException(resultCodeEnum.getCode(), message);
    }

    //司机认证信息
    @Override
    public DriverAuthInfoVo getDriverAuthInfo(Long driverId) {
        Result<DriverAuthInfoVo> authInfoVoResult = driverInfoFeignClient.getDriverAuthInfo(driverId);
        DriverAuthInfoVo driverAuthInfoVo = authInfoVoResult.getData();
        return driverAuthInfoVo;
    }

    //更新司机认证信息
    @Override
    public Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        Result<Boolean> booleanResult = driverInfoFeignClient.UpdateDriverAuthInfo(updateDriverAuthInfoForm);
        //service-driver 校验失败时返回业务码与中文提示，这里要透传，否则调用方只会看到 200 和 null
        if (!ResultCodeEnum.SUCCESS.getCode().equals(booleanResult.getCode())) {
            throw new GuiguException(booleanResult.getCode(), booleanResult.getMessage());
        }
        Boolean data = booleanResult.getData();
        return data;
    }

    //创建司机人脸模型
    @Override
    public Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm) {
        Result<Boolean> booleanResult = driverInfoFeignClient.creatDriverFaceModel(driverFaceModelForm);
        return booleanResult.getData();
    }

    //判断是否进行人脸识别
    @Override
    public Boolean isFaceRecognition(Long driverId) {
        return driverInfoFeignClient.isFaceRecognition(driverId).getData();
    }

    //人脸识别
    @Override
    public Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm) {
        return driverInfoFeignClient.verifyDriverFace(driverFaceModelForm).getData();
    }

    //开始接单服务
    @Override
    public Boolean startService(Long driverId) {
        //1 判断完成认证
        DriverLoginVo driverLoginVo = driverInfoFeignClient.getDriverLoginInfo(driverId).getData();
        if(driverLoginVo.getAuthStatus()!=2) {
            throw new GuiguException(ResultCodeEnum.AUTH_ERROR);
        }

        //2 判断当日是否人脸识别
        Boolean isFace = driverInfoFeignClient.isFaceRecognition(driverId).getData();
        if(!isFace) {
            throw new GuiguException(ResultCodeEnum.FACE_ERROR);
        }

        //3 更新订单状态 1 开始接单
        driverInfoFeignClient.updateServiceStatus(driverId,1);

        //4 删除redis司机位置信息
        locationFeignClient.removeDriverLocation(driverId);

        //5 清空司机临时队列数据
        newOrderFeignClient.clearNewOrderQueueData(driverId);
        return true;
    }

    //停止接单服务
    @Override
    public Boolean stopService(Long driverId) {
        //更新司机的接单状态 0
        driverInfoFeignClient.updateServiceStatus(driverId,0);

        //删除司机位置信息
        locationFeignClient.removeDriverLocation(driverId);

        //清空司机临时队列
        newOrderFeignClient.clearNewOrderQueueData(driverId);
        return true;
    }
}
