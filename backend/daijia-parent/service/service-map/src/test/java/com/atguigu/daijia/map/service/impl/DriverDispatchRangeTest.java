package com.atguigu.daijia.map.service.impl;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverDispatchRangeTest {
    @Mock RedisTemplate<String, String> redisTemplate;
    @Mock GeoOperations<String, String> geo;
    @Mock DriverInfoFeignClient driverInfoFeignClient;
    @InjectMocks LocationServiceImpl service;
    private DriverSet settings;
    private SearchNearByDriverForm query;

    @BeforeEach
    void setup() {
        when(redisTemplate.opsForGeo()).thenReturn(geo);
        GeoResult<RedisGeoCommands.GeoLocation<String>> driver = new GeoResult<>(
                new RedisGeoCommands.GeoLocation<>("1", null),
                new Distance(13.03, RedisGeoCommands.DistanceUnit.KILOMETERS));
        when(geo.radius(anyString(), any(Circle.class), any(RedisGeoCommands.GeoRadiusCommandArgs.class)))
                .thenReturn(new GeoResults<>(List.of(driver)));
        settings = new DriverSet();
        settings.setOrderDistance(BigDecimal.ZERO);
        settings.setAcceptDistance(BigDecimal.valueOf(50));
        when(driverInfoFeignClient.getDriverSet(1L)).thenReturn(Result.ok(settings));
        query = new SearchNearByDriverForm();
        query.setLatitude(BigDecimal.valueOf(31.244147));
        query.setLongitude(BigDecimal.valueOf(121.6784));
        query.setMileageDistance(BigDecimal.valueOf(15));
    }

    @Test
    void driverThirteenKilometresAwayIsIncludedWhenAcceptDistanceIsFifty() {
        assertEquals(1, service.searchNearByDriver(query).size());
        ArgumentCaptor<Circle> circle = ArgumentCaptor.forClass(Circle.class);
        verify(geo).radius(anyString(), circle.capture(), any(RedisGeoCommands.GeoRadiusCommandArgs.class));
        assertEquals(100, circle.getValue().getRadius().getValue());
    }

    @Test
    void driverPersonalDistanceLimitStillApplies() {
        settings.setAcceptDistance(BigDecimal.TEN);
        assertTrue(service.searchNearByDriver(query).isEmpty());
    }

    @Test
    void driverOrderMileageLimitStillApplies() {
        settings.setOrderDistance(BigDecimal.TEN);
        assertTrue(service.searchNearByDriver(query).isEmpty());
    }
}
