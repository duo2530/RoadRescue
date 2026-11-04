package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.login.GuiguLogin;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "位置API接口管理")
@RestController
@RequestMapping("/location")
public class LocationController {

    private static final int MAX_NEARBY_DRIVERS = 20;

    @Autowired
    private LocationFeignClient locationFeignClient;

    @Operation(summary = "乘客首页：附近在线救援车辆的位置")
    @GuiguLogin
    @PostMapping("/searchNearByDriver")
    public Result<List<NearByDriverVo>> searchNearByDriver(@RequestBody SearchNearByDriverForm form) {
        if (form.getLongitude() == null || form.getLatitude() == null) {
            return Result.ok(List.of());
        }
        // 下单前还不知道里程，按 0 公里搜索，只受司机自己的接单距离约束
        form.setMileageDistance(BigDecimal.ZERO);
        List<NearByDriverVo> drivers = locationFeignClient.searchNearByDriver(form).getData();
        // 乘客只需要车的位置，不下发司机 id
        return Result.ok(drivers.stream().limit(MAX_NEARBY_DRIVERS).peek(d -> d.setDriverId(null)).toList());
    }
}
