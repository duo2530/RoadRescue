package com.atguigu.daijia.customer.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.entity.customer.CustomerInfo;
import com.atguigu.daijia.model.query.customer.CustomerInfoQuery;
import com.atguigu.daijia.model.vo.base.PageVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(value = "service-customer", contextId = "customerMgrFeignClient")
public interface CustomerMgrFeignClient {

    @PostMapping("/customer/mgr/findPage/{page}/{limit}")
    Result<PageVo<CustomerInfo>> findPage(@PathVariable("page") Long page, @PathVariable("limit") Long limit,
                                          @RequestBody CustomerInfoQuery query);

    @GetMapping("/customer/mgr/updateStatus/{id}/{status}")
    Result<Boolean> updateStatus(@PathVariable("id") Long id, @PathVariable("status") Integer status);

    @GetMapping("/customer/mgr/count")
    Result<Long> count();
}
