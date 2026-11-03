package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 道路救援 H5 演示登录：浏览器里拿不到 wx.login 的 code，直接为配置的演示乘客签发 token。
 * 仅当 roadrescue.demo-login.enabled=true 时注册，生产环境不要打开。
 */
@Tag(name = "演示登录(仅本地)")
@RestController
@RequestMapping("/customer")
@ConditionalOnProperty(prefix = "roadrescue.demo-login", name = "enabled", havingValue = "true")
@SuppressWarnings({"unchecked", "rawtypes"})
public class DemoLoginController {

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${roadrescue.demo-login.customer-id:1}")
    private Long demoCustomerId;

    @Operation(summary = "H5 演示登录")
    @GetMapping("/demoLogin")
    public Result<String> demoLogin() {
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        redisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX + token,
                demoCustomerId.toString(),
                RedisConstant.USER_LOGIN_KEY_TIMEOUT,
                TimeUnit.SECONDS);
        return Result.ok(token);
    }
}
