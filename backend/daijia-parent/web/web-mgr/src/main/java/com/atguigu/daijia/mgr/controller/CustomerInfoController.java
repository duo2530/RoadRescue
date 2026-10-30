package com.atguigu.daijia.mgr.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.customer.client.CustomerMgrFeignClient;
import com.atguigu.daijia.model.entity.customer.CustomerInfo;
import com.atguigu.daijia.model.query.customer.CustomerInfoQuery;
import com.atguigu.daijia.model.vo.base.PageVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "乘客(求助用户)管理")
@RestController
@RequestMapping("/customer/info")
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerInfoController {

	@Autowired
	private CustomerMgrFeignClient customerMgrFeignClient;

	@Operation(summary = "乘客分页列表")
	@PostMapping("{page}/{limit}")
	public Result<PageVo<CustomerInfo>> findPage(@PathVariable Long page, @PathVariable Long limit,
												 @RequestBody(required = false) CustomerInfoQuery query) {
		return customerMgrFeignClient.findPage(page, limit, query == null ? new CustomerInfoQuery() : query);
	}

	@Operation(summary = "启用/禁用乘客")
	@GetMapping("updateStatus/{id}/{status}")
	public Result<Boolean> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
		return customerMgrFeignClient.updateStatus(id, status);
	}
}
