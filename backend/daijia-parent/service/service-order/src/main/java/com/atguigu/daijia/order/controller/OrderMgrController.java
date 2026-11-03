package com.atguigu.daijia.order.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.query.order.OrderInfoQuery;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.order.service.OrderInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 道路救援管理端：订单(救援工单)查询接口，供 web-mgr 调用
 */
@Tag(name = "管理端订单接口")
@RestController
@RequestMapping("/order/mgr")
public class OrderMgrController {

    @Autowired
    private OrderInfoService orderInfoService;

    @Operation(summary = "订单分页列表")
    @PostMapping("/findPage/{page}/{limit}")
    public Result<PageVo<OrderInfo>> findPage(@PathVariable Long page, @PathVariable Long limit,
                                              @RequestBody(required = false) OrderInfoQuery query) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            wrapper.like(StringUtils.hasText(query.getOrderNo()), OrderInfo::getOrderNo, query.getOrderNo())
                    .eq(query.getStatus() != null, OrderInfo::getStatus, query.getStatus())
                    .ge(StringUtils.hasText(query.getCreateTimeBegin()), OrderInfo::getCreateTime, query.getCreateTimeBegin())
                    .le(StringUtils.hasText(query.getCreateTimeEnd()), OrderInfo::getCreateTime, query.getCreateTimeEnd());
        }
        wrapper.orderByDesc(OrderInfo::getId);
        Page<OrderInfo> pageInfo = orderInfoService.page(new Page<>(page, limit), wrapper);
        PageVo<OrderInfo> pageVo = new PageVo<>(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
        pageVo.setPage(page);
        pageVo.setLimit(limit);
        return Result.ok(pageVo);
    }

    @Operation(summary = "订单详情")
    @GetMapping("/getById/{id}")
    public Result<OrderInfo> getById(@PathVariable Long id) {
        return Result.ok(orderInfoService.getById(id));
    }

    @Operation(summary = "乘客取消订单：仅等待接单状态可取消")
    @GetMapping("/customerCancel/{customerId}/{orderId}")
    public Result<Boolean> customerCancel(@PathVariable Long customerId, @PathVariable Long orderId) {
        OrderInfo orderInfo = orderInfoService.getById(orderId);
        if (orderInfo == null || !customerId.equals(orderInfo.getCustomerId())
                || orderInfo.getStatus().intValue() != OrderStatus.WAITING_ACCEPT.getStatus().intValue()) {
            return Result.ok(false);
        }
        orderInfoService.orderCancel(orderId);
        return Result.ok(true);
    }

    @Operation(summary = "看板统计：总单量、今日单量、各状态单量、成交金额")
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> map = new HashMap<>();
        map.put("total", orderInfoService.count());
        map.put("today", orderInfoService.count(new LambdaQueryWrapper<OrderInfo>()
                .ge(OrderInfo::getCreateTime, LocalDate.now().toString())));
        List<Map<String, Object>> byStatus = orderInfoService.listMaps(new QueryWrapper<OrderInfo>()
                .select("status", "count(*) as cnt").groupBy("status"));
        map.put("byStatus", byStatus);
        List<Map<String, Object>> amount = orderInfoService.listMaps(new QueryWrapper<OrderInfo>()
                .select("ifnull(sum(real_amount),0) as amount").ge("status", 8));
        Object paid = amount.isEmpty() || amount.get(0) == null ? BigDecimal.ZERO : amount.get(0).get("amount");
        map.put("paidAmount", paid);
        return Result.ok(map);
    }
}
