package com.atguigu.daijia.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

/** 救援计价统一在服务端计算，前端金额仅用于预估展示。 */
public final class RescuePricing {
    private RescuePricing() { }

    public static boolean supports(String type) {
        return type != null && Set.of("tow", "battery", "tire", "charge").contains(type);
    }

    public static BigDecimal price(String type, BigDecimal distanceKm) {
        BigDecimal amount;
        switch (type) {
            case "tow":
                if (distanceKm == null || distanceKm.signum() < 0) throw new IllegalArgumentException("拖车里程无效");
                amount = BigDecimal.valueOf(180).add(distanceKm.subtract(BigDecimal.TEN).max(BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(45))).setScale(0, RoundingMode.HALF_UP);
                break;
            case "battery": amount = BigDecimal.valueOf(139); break;
            case "tire": amount = BigDecimal.valueOf(199); break;
            case "charge": amount = BigDecimal.valueOf(350); break;
            default: throw new IllegalArgumentException("服务类型错误");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }
}
