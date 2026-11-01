package com.atguigu.daijia.payment.service;

import com.alibaba.fastjson.JSON;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.entity.payment.PaymentInfo;
import com.atguigu.daijia.model.form.payment.CreatePaymentForm;
import com.atguigu.daijia.model.vo.order.OrderPayVo;
import com.atguigu.daijia.model.vo.payment.PaymentSessionVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.atguigu.daijia.payment.config.AlipayProperties;
import com.atguigu.daijia.payment.mapper.PaymentInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.redisson.api.RedissonClient;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class AlipayPaymentService {
    private static final int ALIPAY = 2;
    private final PaymentInfoMapper mapper;
    private final OrderInfoFeignClient orders;
    private final AlipayGateway gateway;
    private final AlipayProperties properties;
    private final RedissonClient redisson;

    public AlipayPaymentService(PaymentInfoMapper mapper, OrderInfoFeignClient orders, AlipayGateway gateway,
                                AlipayProperties properties, RedissonClient redisson) {
        this.mapper = mapper; this.orders = orders; this.gateway = gateway; this.properties = properties; this.redisson = redisson;
    }

    private <T> T data(Result<T> result) {
        if (result == null || !Integer.valueOf(200).equals(result.getCode())) {
            throw new GuiguException(result == null ? 502 : result.getCode(), result == null ? "订单服务暂不可用" : result.getMessage());
        }
        return result.getData();
    }

    private <T> T locked(String orderNo, Supplier<T> action) {
        if (!StringUtils.hasText(orderNo) || !orderNo.matches("[A-Za-z0-9_-]{1,64}")) throw new GuiguException(210, "订单号无效");
        RLock lock = redisson.getLock("payment:alipay:" + orderNo);
        try {
            if (!lock.tryLock(3, TimeUnit.SECONDS)) throw new GuiguException(206, "支付处理中，请稍后重试");
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GuiguException(206, "支付操作已中断，请重试");
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    private PaymentInfo payment(String orderNo) {
        return mapper.selectOne(new LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOrderNo, orderNo));
    }

    public PaymentSessionVo create(CreatePaymentForm form) {
        gateway.requireConfigured();
        if (!"ALIPAY_WAP_SANDBOX".equals(form.getChannel())) throw new GuiguException(210, "支付渠道尚未启用");
        return locked(form.getOrderNo(), () -> {
            OrderPayVo order = data(orders.preparePayment(form.getOrderNo(), form.getCustomerId()));
            PaymentInfo record = payment(order.getOrderNo());
            if (record == null) {
                record = new PaymentInfo();
                record.setOrderNo(order.getOrderNo());
                record.setAmount(order.getPayAmount().setScale(2, java.math.RoundingMode.UNNECESSARY));
                record.setContent("中道救援服务费（支付宝沙箱）");
                record.setPayWay(ALIPAY);
                record.setPaymentStatus(0);
                mapper.insert(record);
            }
            if (!Integer.valueOf(ALIPAY).equals(record.getPayWay()) || record.getAmount().compareTo(order.getPayAmount()) != 0) {
                throw new GuiguException(210, "支付记录与账单不一致，请联系客服");
            }
            if (Integer.valueOf(1).equals(record.getPaymentStatus())) throw new GuiguException(210, "订单已支付，请刷新");
            PaymentSessionVo session = new PaymentSessionVo();
            session.setChannel("ALIPAY_WAP_SANDBOX");
            session.setOrderNo(order.getOrderNo());
            session.setRedirectUrl(gateway.create(order.getOrderNo(), order.getOrderId(), record.getAmount(), form.getReturnOrigin()));
            return session;
        });
    }

    public boolean query(String orderNo, Long customerId) {
        // 每次查询先核验订单归属，不能使用别人的订单号探测交易。
        OrderPayVo order = data(orders.getOrderPayVo(orderNo, customerId));
        if (order == null) throw new GuiguException(205, "订单不存在或无权查询");
        return locked(orderNo, () -> {
            PaymentInfo record = payment(orderNo);
            if (record == null || !Integer.valueOf(ALIPAY).equals(record.getPayWay())) return false;
            if (Integer.valueOf(1).equals(record.getPaymentStatus())) return true;
            AlipayTradeQueryResponse response = gateway.query(orderNo);
            if (!response.isSuccess()) {
                if ("ACQ.TRADE_NOT_EXIST".equals(response.getSubCode())) return false;
                throw new GuiguException(502, "支付宝交易查询失败，请稍后重试");
            }
            if (!Set.of("TRADE_SUCCESS", "TRADE_FINISHED").contains(response.getTradeStatus() == null ? "" : response.getTradeStatus())) return false;
            confirm(record, response.getOutTradeNo(), response.getTradeNo(), response.getTotalAmount(), "服务端主动查询确认");
            return true;
        });
    }

    public void notifyPayment(Map<String, String> params) {
        if (!gateway.verify(params) || !properties.getAppId().equals(params.get("app_id"))) {
            throw new GuiguException(205, "支付宝通知验签失败");
        }
        if (StringUtils.hasText(properties.getSellerId()) && !properties.getSellerId().equals(params.get("seller_id"))) {
            throw new GuiguException(205, "支付宝收款方不匹配");
        }
        String status = params.get("trade_status");
        if (!"TRADE_SUCCESS".equals(status) && !"TRADE_FINISHED".equals(status)) return;
        locked(params.get("out_trade_no"), () -> {
            PaymentInfo record = payment(params.get("out_trade_no"));
            if (record == null) throw new GuiguException(205, "支付记录不存在");
            confirm(record, params.get("out_trade_no"), params.get("trade_no"), params.get("total_amount"), JSON.toJSONString(params));
            return null;
        });
    }

    private void confirm(PaymentInfo record, String orderNo, String tradeNo, String amount, String evidence) {
        BigDecimal actual;
        try { actual = new BigDecimal(amount); }
        catch (RuntimeException e) { throw new GuiguException(205, "支付金额无效"); }
        if (!Integer.valueOf(ALIPAY).equals(record.getPayWay()) || !record.getOrderNo().equals(orderNo)
                || !StringUtils.hasText(tradeNo) || record.getAmount().compareTo(actual) != 0) {
            throw new GuiguException(205, "支付结果与账单不一致");
        }
        if (record.getTransactionId() != null && !record.getTransactionId().equals(tradeNo)) {
            throw new GuiguException(205, "支付交易号不一致");
        }
        // 远程状态更新幂等；先更新订单，失败时不记为支付完成，等待通知/查询重试。
        if (!Boolean.TRUE.equals(data(orders.updateOrderPayStatus(orderNo)))) throw new GuiguException(502, "支付状态同步中，请稍后查询");
        if (!Integer.valueOf(1).equals(record.getPaymentStatus())) {
            record.setPaymentStatus(1);
            record.setTransactionId(tradeNo);
            record.setCallbackTime(new Date());
            record.setCallbackContent(evidence);
            if (mapper.updateById(record) != 1) throw new GuiguException(502, "支付记录同步中，请重试");
        }
    }
}
