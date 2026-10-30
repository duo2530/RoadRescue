package com.atguigu.daijia.payment.config;

import com.wechat.pay.java.core.RSAPublicKeyConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="wx.v3pay") //读取节点
@Data
public class WxPayV3Properties {

    private String appid;
    /** 商户号 */
    public String merchantId;
    /** 商户API私钥路径 */
    public String privateKeyPath;
    /** 商户证书序列号 */
    public String merchantSerialNumber;
    /** 商户APIV3密钥 */
    public String apiV3key;
    /** 回调地址 */
    private String notifyUrl;
    /** 微信支付公钥文件路径 */
    private String publicKeyPath;
    /** 微信支付公钥ID(PUB_KEY_ID_ 开头) */
    private String publicKeyId;

    /** 商户已启用微信支付公钥模式,平台证书接口不可用,改用公钥验签 */
    @Bean
    public RSAPublicKeyConfig getConfig(){
        return new RSAPublicKeyConfig.Builder()
                .merchantId(this.getMerchantId())
                .privateKeyFromPath(this.getPrivateKeyPath())
                .merchantSerialNumber(this.getMerchantSerialNumber())
                .publicKeyFromPath(this.getPublicKeyPath())
                .publicKeyId(this.getPublicKeyId())
                .apiV3Key(this.getApiV3key())
                .build();

    }
}
