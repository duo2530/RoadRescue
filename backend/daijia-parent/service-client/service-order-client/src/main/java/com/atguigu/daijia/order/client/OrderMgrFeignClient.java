package com.atguigu.daijia.order.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.query.order.OrderInfoQuery;
import com.atguigu.daijia.model.vo.base.PageVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(value = "service-order", contextId = "orderMgrFeignClient")
public interface OrderMgrFeignClient {

    @PostMapping("/order/mgr/findPage/{page}/{limit}")
    Result<PageVo<OrderInfo>> findPage(@PathVariable("page") Long page, @PathVariable("limit") Long limit,
                                       @RequestBody OrderInfoQuery query);

    @GetMapping("/order/mgr/getById/{id}")
    Result<OrderInfo> getById(@PathVariable("id") Long id);

    @GetMapping("/order/mgr/customerCancel/{customerId}/{orderId}")
    Result<Boolean> customerCancel(@PathVariable("customerId") Long customerId, @PathVariable("orderId") Long orderId);

    @GetMapping("/order/mgr/stats")
    Result<Map<String, Object>> stats();
}
