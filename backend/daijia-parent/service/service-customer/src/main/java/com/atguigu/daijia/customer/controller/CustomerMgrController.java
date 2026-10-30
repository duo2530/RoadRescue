package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.customer.service.CustomerInfoService;
import com.atguigu.daijia.model.entity.customer.CustomerInfo;
import com.atguigu.daijia.model.query.customer.CustomerInfoQuery;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 中道救援管理端：乘客(求助用户)查询接口，供 web-mgr 调用
 */
@Tag(name = "管理端乘客接口")
@RestController
@RequestMapping("/customer/mgr")
public class CustomerMgrController {

    @Autowired
    private CustomerInfoService customerInfoService;

    @Operation(summary = "乘客分页列表")
    @PostMapping("/findPage/{page}/{limit}")
    public Result<PageVo<CustomerInfo>> findPage(@PathVariable Long page, @PathVariable Long limit,
                                                 @RequestBody(required = false) CustomerInfoQuery query) {
        LambdaQueryWrapper<CustomerInfo> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            String keyword = query.getKeyword();
            wrapper.and(StringUtils.hasText(keyword), w -> w.like(CustomerInfo::getNickname, keyword)
                            .or().like(CustomerInfo::getPhone, keyword))
                    .eq(query.getStatus() != null, CustomerInfo::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(CustomerInfo::getId);
        Page<CustomerInfo> pageInfo = customerInfoService.page(new Page<>(page, limit), wrapper);
        PageVo<CustomerInfo> pageVo = new PageVo<>(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
        pageVo.setPage(page);
        pageVo.setLimit(limit);
        return Result.ok(pageVo);
    }

    @Operation(summary = "启用/禁用乘客")
    @GetMapping("/updateStatus/{id}/{status}")
    public Result<Boolean> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        CustomerInfo customerInfo = new CustomerInfo();
        customerInfo.setId(id);
        customerInfo.setStatus(status);
        return Result.ok(customerInfoService.updateById(customerInfo));
    }

    @Operation(summary = "乘客总数")
    @GetMapping("/count")
    public Result<Long> count() {
        return Result.ok(customerInfoService.count());
    }
}
