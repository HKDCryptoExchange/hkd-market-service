-- ========================================
-- V3: 创建额外的索引和约束
-- ========================================

-- 为K线表创建复合索引 (优化查询性能)
CREATE INDEX IF NOT EXISTS idx_klines_symbol_interval_completed
    ON klines (symbol, interval, completed, open_time DESC);

-- 为成交记录表创建复合索引
CREATE INDEX IF NOT EXISTS idx_market_trades_symbol_side_tradetime
    ON market_trades (symbol, taker_side, trade_time DESC);

-- ========================================
-- 数据完整性约束
-- ========================================

-- K线表检查约束
ALTER TABLE klines
    ADD CONSTRAINT check_klines_interval
    CHECK (interval IN ('1m', '5m', '15m', '30m', '1h', '4h', '1d', '1w', '1M'));

ALTER TABLE klines
    ADD CONSTRAINT check_klines_prices
    CHECK (open > 0 AND high > 0 AND low > 0 AND close > 0 AND high >= low);

ALTER TABLE klines
    ADD CONSTRAINT check_klines_volume
    CHECK (volume >= 0 AND amount >= 0 AND trade_count >= 0);

ALTER TABLE klines
    ADD CONSTRAINT check_klines_time
    CHECK (close_time > open_time);

-- 成交记录表检查约束
ALTER TABLE market_trades
    ADD CONSTRAINT check_market_trades_side
    CHECK (taker_side IN ('BUY', 'SELL'));

ALTER TABLE market_trades
    ADD CONSTRAINT check_market_trades_price
    CHECK (price > 0 AND quantity > 0 AND amount > 0);

COMMENT ON INDEX idx_klines_symbol_interval_completed IS '优化K线查询性能的复合索引';
COMMENT ON INDEX idx_market_trades_symbol_side_tradetime IS '优化成交记录查询性能的复合索引';
