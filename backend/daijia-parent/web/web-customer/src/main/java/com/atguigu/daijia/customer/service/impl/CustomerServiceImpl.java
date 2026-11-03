package com.atguigu.daijia.customer.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.customer.client.CustomerInfoFeignClient;
import com.atguigu.daijia.customer.service.CustomerService;
import com.atguigu.daijia.model.form.customer.CustomerPhoneForm;
import com.atguigu.daijia.model.form.customer.PhoneLoginForm;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
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
public class CustomerServiceImpl implements CustomerService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");
    private static final Pattern SMS_CODE_PATTERN = Pattern.compile("^\\d{6}$");

    //注入远程调用接口
    @Autowired
    private CustomerInfoFeignClient client;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private CustomerInfoFeignClient customerInfoFeignClient;

    //演示验证码，没有短信服务商时使用；生产环境必须清空，否则任何人知道手机号即可登录
    @Value("${roadrescue.sms.demo-code:}")
    private String demoSmsCode;

    @Override
    public String login(String code) {
        //1 拿着code进行远程调用，返回用户id
        Result<Long> loginResult = client.login(code);

        //2 判断如果返回失败了，返回错误提示
        Integer codeResult = loginResult.getCode();
        if(codeResult != 200) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        //3 获取远程调用返回用户id
        Long customerId = loginResult.getData();

        //4 判断返回用户id是否为空，如果为空，返回错误提示
        if(customerId == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        //5 生成token并写入Redis
        return issueToken(customerId);
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
                RedisConstant.SMS_CUSTOMER_SEND_LIMIT_KEY_PREFIX + phone, "1",
                RedisConstant.SMS_CUSTOMER_SEND_LIMIT_TIMEOUT, TimeUnit.SECONDS);
        if(!Boolean.TRUE.equals(firstInWindow)) {
            throw fail(ResultCodeEnum.FAIL, "发送过于频繁，请60秒后再试");
        }

        redisTemplate.opsForValue().set(codeKey(phone), demoSmsCode,
                RedisConstant.SMS_CUSTOMER_CODE_TIMEOUT, TimeUnit.SECONDS);
        //新验证码从零开始计错误次数
        redisTemplate.delete(failKey(phone));

        return "演示验证码：" + demoSmsCode;
    }

    @Override
    public String loginByPhone(PhoneLoginForm form) {
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

        CustomerPhoneForm customerPhoneForm = new CustomerPhoneForm();
        customerPhoneForm.setPhone(phone);
        Result<Long> loginResult = customerInfoFeignClient.loginByPhone(customerPhoneForm);
        if(!Integer.valueOf(200).equals(loginResult.getCode())) {
            throw new GuiguException(loginResult.getCode(), loginResult.getMessage());
        }

        Long customerId = loginResult.getData();
        if(customerId == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        return issueToken(customerId);
    }

    @Override
    public CustomerLoginVo getCustomerLoginInfo(String token) {
        //2 根据token查询redis
        //3 查询token在redis里面对应用户id
        String customerId =
                (String)redisTemplate.opsForValue()
                        .get(RedisConstant.USER_LOGIN_KEY_PREFIX + token);

        if(StringUtils.isEmpty(customerId)) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
//        if(!StringUtils.hasText(customerId)) {
//            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
//        }

        //4 根据用户id进行远程调用 得到用户信息
        Result<CustomerLoginVo> customerLoginVoResult =
                customerInfoFeignClient.getCustomerLoginInfo(Long.parseLong(customerId));

        Integer code = customerLoginVoResult.getCode();
        if(code != 200) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        CustomerLoginVo customerLoginVo = customerLoginVoResult.getData();
        if(customerLoginVo == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        //5 返回用户信息
        return customerLoginVo;
    }

    //获取用户信息
    @Override
    public CustomerLoginVo getCustomerInfo(Long customerId) {

        //根据用户id进行远程调用 得到用户信息
        Result<CustomerLoginVo> customerLoginVoResult =
                customerInfoFeignClient.getCustomerLoginInfo(customerId);

        Integer code = customerLoginVoResult.getCode();
        if(code != 200) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        CustomerLoginVo customerLoginVo = customerLoginVoResult.getData();
        if(customerLoginVo == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        //5 返回用户信息
        return customerLoginVo;
    }

    //更新用户微信手机号
    @Override
    public Boolean updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm) {
        Result<Boolean> booleanResult = customerInfoFeignClient.updateWxPhoneNumber(updateWxPhoneForm);
        return true;
    }

    //签发登录 token，微信登录和手机号登录共用
    private String issueToken(Long customerId) {
        String token = UUID.randomUUID().toString().replaceAll("-","");
        redisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX+token,
                                     customerId.toString(),
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
        if(fails != null && fails >= RedisConstant.SMS_CUSTOMER_MAX_FAIL_TIMES) {
            redisTemplate.delete(codeKey(phone));
            redisTemplate.delete(failKey(phone));
        }
    }

    private static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    private static String codeKey(String phone) {
        return RedisConstant.SMS_CUSTOMER_CODE_KEY_PREFIX + phone;
    }

    private static String failKey(String phone) {
        return RedisConstant.SMS_CUSTOMER_FAIL_KEY_PREFIX + phone;
    }

    private static GuiguException fail(ResultCodeEnum resultCodeEnum, String message) {
        return new GuiguException(resultCodeEnum.getCode(), message);
    }

}
