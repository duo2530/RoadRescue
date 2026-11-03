package com.atguigu.daijia.common.constant;

public class RedisConstant {

    //用户登录
    public static final String USER_LOGIN_KEY_PREFIX = "user:login:";
    public static final String USER_LOGIN_REFRESH_KEY_PREFIX = "user:login:refresh:";
    public static final int USER_LOGIN_KEY_TIMEOUT = 60 * 60 * 24 * 100;
    public static final int USER_LOGIN_REFRESH_KEY_TIMEOUT = 60 * 60 * 24 * 365;

    //手机验证码登录（乘客端）
    public static final String SMS_CUSTOMER_CODE_KEY_PREFIX = "sms:customer:code:";
    public static final int SMS_CUSTOMER_CODE_TIMEOUT = 60 * 5;
    public static final String SMS_CUSTOMER_FAIL_KEY_PREFIX = "sms:customer:fail:";
    public static final int SMS_CUSTOMER_MAX_FAIL_TIMES = 5;
    public static final String SMS_CUSTOMER_SEND_LIMIT_KEY_PREFIX = "sms:customer:limit:";
    public static final int SMS_CUSTOMER_SEND_LIMIT_TIMEOUT = 60;

    //手机验证码登录（司机端）
    public static final String SMS_DRIVER_CODE_KEY_PREFIX = "sms:driver:code:";
    public static final int SMS_DRIVER_CODE_TIMEOUT = 60 * 5;
    public static final String SMS_DRIVER_FAIL_KEY_PREFIX = "sms:driver:fail:";
    public static final int SMS_DRIVER_MAX_FAIL_TIMES = 5;
    public static final String SMS_DRIVER_SEND_LIMIT_KEY_PREFIX = "sms:driver:limit:";
    public static final int SMS_DRIVER_SEND_LIMIT_TIMEOUT = 60;

    //司机GEO地址
    public static final String DRIVER_GEO_LOCATION = "driver:geo:location";
    //司机接单临时容器
    public static final String DRIVER_ORDER_TEMP_LIST = "driver:order:temp:list:";
    public static final long DRIVER_ORDER_TEMP_LIST_EXPIRES_TIME = 1;
    //司机订单去重容器
    public static final String DRIVER_ORDER_REPEAT_LIST = "driver:order:repeat:list:";
    public static final long DRIVER_ORDER_REPEAT_LIST_EXPIRES_TIME = 16;

//    //订单与任务关联
//    public static final String ORDER_JOB = "order:job:";
//    public static final long ORDER_JOB_EXPIRES_TIME = 15;

    //更新订单位置
    public static final String UPDATE_ORDER_LOCATION = "update:order:location:";
    public static final long UPDATE_ORDER_LOCATION_EXPIRES_TIME = 15;

    //订单接单标识
    public static final String ORDER_ACCEPT_MARK = "order:accept:mark:";
    public static final long ORDER_ACCEPT_MARK_EXPIRES_TIME = 15;

    //抢新订单锁
    public static final String ROB_NEW_ORDER_LOCK = "rob:new:order:lock";
    //等待获取锁的时间
    public static final long ROB_NEW_ORDER_LOCK_WAIT_TIME = 1;
    //加锁的时间
    public static final long ROB_NEW_ORDER_LOCK_LEASE_TIME = 1;

    //优惠券信息
    public static final String COUPON_INFO = "coupon:info:";

    //优惠券分布式锁
    public static final String COUPON_LOCK = "coupon:lock:";
    //等待获取锁的时间
    public static final long COUPON_LOCK_WAIT_TIME = 1;
    //加锁的时间
    public static final long COUPON_LOCK_LEASE_TIME = 1;
}
