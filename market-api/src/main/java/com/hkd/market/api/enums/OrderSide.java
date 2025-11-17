package com.hkd.market.api.enums;

import lombok.Getter;

/**
 * 订单方向枚举
 */
@Getter
public enum OrderSide {

    /**
     * 买单
     */
    BUY("buy"),

    /**
     * 卖单
     */
    SELL("sell");

    /**
     * 方向代码
     */
    private final String code;

    OrderSide(String code) {
        this.code = code;
    }

    /**
     * 根据code获取枚举值
     *
     * @param code 方向代码
     * @return OrderSide枚举
     * @throws IllegalArgumentException 如果code不存在
     */
    public static OrderSide fromCode(String code) {
        for (OrderSide side : values()) {
            if (side.code.equalsIgnoreCase(code)) {
                return side;
            }
        }
        throw new IllegalArgumentException("Invalid order side: " + code);
    }
}
