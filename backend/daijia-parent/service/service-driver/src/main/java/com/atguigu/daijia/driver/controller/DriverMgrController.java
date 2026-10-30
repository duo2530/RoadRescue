package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.service.DriverInfoService;
import com.atguigu.daijia.model.entity.driver.DriverInfo;
import com.atguigu.daijia.model.query.driver.DriverInfoQuery;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 中道救援管理端：司机(救援师傅)查询与审核接口，供 web-mgr 调用
 */
@Tag(name = "管理端司机接口")
@RestController
@RequestMapping("/driver/mgr")
public class DriverMgrController {

    @Autowired
    private DriverInfoService driverInfoService;

    @Operation(summary = "司机分页列表")
    @PostMapping("/findPage/{page}/{limit}")
    public Result<PageVo<DriverInfo>> findPage(@PathVariable Long page, @PathVariable Long limit,
                                               @RequestBody(required = false) DriverInfoQuery query) {
        LambdaQueryWrapper<DriverInfo> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            String keyword = query.getKeyword();
            wrapper.and(StringUtils.hasText(keyword), w -> w.like(DriverInfo::getName, keyword)
                            .or().like(DriverInfo::getPhone, keyword)
                            .or().like(DriverInfo::getNickname, keyword))
                    .eq(query.getStatus() != null, DriverInfo::getAuthStatus, query.getStatus());
        }
        wrapper.orderByDesc(DriverInfo::getId);
        Page<DriverInfo> pageInfo = driverInfoService.page(new Page<>(page, limit), wrapper);
        PageVo<DriverInfo> pageVo = new PageVo<>(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
        pageVo.setPage(page);
        pageVo.setLimit(limit);
        return Result.ok(pageVo);
    }

    @Operation(summary = "司机详情")
    @GetMapping("/getById/{id}")
    public Result<DriverInfo> getById(@PathVariable Long id) {
        return Result.ok(driverInfoService.getById(id));
    }

    @Operation(summary = "审核司机认证：authStatus 2=通过 -1=驳回")
    @GetMapping("/updateAuthStatus/{id}/{authStatus}")
    public Result<Boolean> updateAuthStatus(@PathVariable Long id, @PathVariable Integer authStatus) {
        DriverInfo driverInfo = new DriverInfo();
        driverInfo.setId(id);
        driverInfo.setAuthStatus(authStatus);
        return Result.ok(driverInfoService.updateById(driverInfo));
    }

    @Operation(summary = "司机总数")
    @GetMapping("/count")
    public Result<Long> count() {
        return Result.ok(driverInfoService.count());
    }
}
