package com.atguigu.daijia.driver.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.entity.driver.DriverInfo;
import com.atguigu.daijia.model.query.driver.DriverInfoQuery;
import com.atguigu.daijia.model.vo.base.PageVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(value = "service-driver", contextId = "driverMgrFeignClient")
public interface DriverMgrFeignClient {

    @PostMapping("/driver/mgr/findPage/{page}/{limit}")
    Result<PageVo<DriverInfo>> findPage(@PathVariable("page") Long page, @PathVariable("limit") Long limit,
                                        @RequestBody DriverInfoQuery query);

    @GetMapping("/driver/mgr/getById/{id}")
    Result<DriverInfo> getById(@PathVariable("id") Long id);

    @GetMapping("/driver/mgr/updateAuthStatus/{id}/{authStatus}")
    Result<Boolean> updateAuthStatus(@PathVariable("id") Long id, @PathVariable("authStatus") Integer authStatus);

    @GetMapping("/driver/mgr/count")
    Result<Long> count();
}
