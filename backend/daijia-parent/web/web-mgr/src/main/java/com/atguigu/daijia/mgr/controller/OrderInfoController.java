package com.atguigu.daijia.mgr.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.customer.client.CustomerMgrFeignClient;
import com.atguigu.daijia.driver.client.DriverMgrFeignClient;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.query.order.OrderInfoQuery;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.order.client.OrderMgrFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@Tag(name = "订单(救援工单)管理")
@RestController
@RequestMapping(value="/order/info")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoController {

	@Autowired
	private OrderMgrFeignClient orderMgrFeignClient;

	@Autowired
	private DriverMgrFeignClient driverMgrFeignClient;

	@Autowired
	private CustomerMgrFeignClient customerMgrFeignClient;

	@Operation(summary = "订单分页列表")
	@PostMapping("{page}/{limit}")
	public Result<PageVo<OrderInfo>> findPage(@PathVariable Long page, @PathVariable Long limit,
											  @RequestBody(required = false) OrderInfoQuery query) {
		return orderMgrFeignClient.findPage(page, limit, query == null ? new OrderInfoQuery() : query);
	}

	@Operation(summary = "订单详情")
	@GetMapping("getById/{id}")
	public Result<OrderInfo> getById(@PathVariable Long id) {
		return orderMgrFeignClient.getById(id);
	}

	@Operation(summary = "看板统计")
	@GetMapping("dashboard")
	public Result<Map<String, Object>> dashboard() {
		Map<String, Object> map = orderMgrFeignClient.stats().getData();
		map.put("driverCount", driverMgrFeignClient.count().getData());
		map.put("customerCount", customerMgrFeignClient.count().getData());
		return Result.ok(map);
	}
}
