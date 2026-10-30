package com.atguigu.daijia.mgr.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.client.DriverMgrFeignClient;
import com.atguigu.daijia.model.entity.driver.DriverInfo;
import com.atguigu.daijia.model.query.driver.DriverInfoQuery;
import com.atguigu.daijia.model.vo.base.PageVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "司机(救援师傅)管理")
@RestController
@RequestMapping(value="/driver/info")
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverInfoController {

	@Autowired
	private DriverMgrFeignClient driverMgrFeignClient;

	@Operation(summary = "司机分页列表")
	@PostMapping("{page}/{limit}")
	public Result<PageVo<DriverInfo>> findPage(@PathVariable Long page, @PathVariable Long limit,
											   @RequestBody(required = false) DriverInfoQuery query) {
		return driverMgrFeignClient.findPage(page, limit, query == null ? new DriverInfoQuery() : query);
	}

	@Operation(summary = "司机详情")
	@GetMapping("getById/{id}")
	public Result<DriverInfo> getById(@PathVariable Long id) {
		return driverMgrFeignClient.getById(id);
	}

	@Operation(summary = "审核司机认证")
	@GetMapping("updateAuthStatus/{id}/{authStatus}")
	public Result<Boolean> updateAuthStatus(@PathVariable Long id, @PathVariable Integer authStatus) {
		return driverMgrFeignClient.updateAuthStatus(id, authStatus);
	}
}
