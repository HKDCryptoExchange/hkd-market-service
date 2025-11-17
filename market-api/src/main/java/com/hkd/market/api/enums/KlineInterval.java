package com.hkd.market.api.enums;

import lombok.Getter;

/**
 * K线周期枚举
 *
 * 支持9种K线周期：1分钟、5分钟、15分钟、30分钟、1小时、4小时、1天、1周、1月
 */
@Getter
public enum KlineInterval {

    /**
     * 1分钟
     */
    MIN_1("1m", 60),

    /**
     * 5分钟
     */
    MIN_5("5m", 300),

    /**
     * 15分钟
     */
    MIN_15("15m", 900),

    /**
     * 30分钟
     */
    MIN_30("30m", 1800),

    /**
     * 1小时
     */
    HOUR_1("1h", 3600),

    /**
     * 4小时
     */
    HOUR_4("4h", 14400),

    /**
     * 1天
     */
    DAY_1("1d", 86400),

    /**
     * 1周
     */
    WEEK_1("1w", 604800),

    /**
     * 1月 (按30天计算)
     */
    MONTH_1("1M", 2592000);

    /**
     * 周期代码 (例如: "1m", "5m", "1h")
     */
    private final String code;

    /**
     * 周期秒数
     */
    private final int seconds;

    KlineInterval(String code, int seconds) {
        this.code = code;
        this.seconds = seconds;
    }

    /**
     * 根据code获取枚举值
     *
     * @param code 周期代码
     * @return KlineInterval枚举
     * @throws IllegalArgumentException 如果code不存在
     */
    public static KlineInterval fromCode(String code) {
        for (KlineInterval interval : values()) {
            if (interval.code.equals(code)) {
                return interval;
            }
        }
        throw new IllegalArgumentException("Invalid kline interval code: " + code);
    }

    /**
     * 验证code是否有效
     *
     * @param code 周期代码
     * @return 是否有效
     */
    public static boolean isValid(String code) {
        try {
            fromCode(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
