-- ========================================
-- V1: 创建K线数据表 (TimescaleDB Hypertable)
-- ========================================

-- 创建K线表
CREATE TABLE IF NOT EXISTS klines (
    id BIGINT PRIMARY KEY,                     -- SnowflakeId主键
    symbol VARCHAR(20) NOT NULL,               -- 交易对 (BTC-USDT)
    interval VARCHAR(10) NOT NULL,             -- K线周期 (1m/5m/15m/30m/1h/4h/1d/1w/1M)
    open_time BIGINT NOT NULL,                 -- K线开始时间 (时间戳秒)
    close_time BIGINT NOT NULL,                -- K线结束时间 (时间戳秒)
    open NUMERIC(20, 8) NOT NULL,              -- 开盘价
    high NUMERIC(20, 8) NOT NULL,              -- 最高价
    low NUMERIC(20, 8) NOT NULL,               -- 最低价
    close NUMERIC(20, 8) NOT NULL,             -- 收盘价
    volume NUMERIC(30, 8) NOT NULL DEFAULT 0,  -- 成交量
    amount NUMERIC(30, 8) NOT NULL DEFAULT 0,  -- 成交额
    trade_count INTEGER NOT NULL DEFAULT 0,    -- 成交笔数
    completed BOOLEAN NOT NULL DEFAULT FALSE,  -- 是否已完成
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,   -- 创建时间
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP    -- 更新时间
);

-- 创建索引
CREATE INDEX idx_klines_symbol_interval_opentime
    ON klines (symbol, interval, open_time DESC);

CREATE INDEX idx_klines_opentime
    ON klines (open_time DESC);

-- 创建唯一约束 (防止重复数据)
CREATE UNIQUE INDEX uk_klines_symbol_interval_opentime
    ON klines (symbol, interval, open_time);

-- ========================================
-- TimescaleDB Hypertable 配置
-- ========================================

-- 启用TimescaleDB扩展 (如果还未启用)
-- CREATE EXTENSION IF NOT EXISTS timescaledb;

-- 将klines表转换为Hypertable (按open_time分区)
-- 注意: 需要先启用TimescaleDB扩展
-- SELECT create_hypertable('klines', 'open_time', chunk_time_interval => 86400);

-- 设置数据保留策略 (保留5年数据)
-- SELECT add_retention_policy('klines', INTERVAL '5 years');

-- ========================================
-- 说明:
-- 1. 如果使用TimescaleDB，需要手动执行上面注释的SQL语句
-- 2. chunk_time_interval => 86400 表示每天一个chunk
-- 3. 数据保留策略会自动删除5年前的数据
-- ========================================

COMMENT ON TABLE klines IS 'K线数据表 (TimescaleDB Hypertable)';
COMMENT ON COLUMN klines.id IS '主键ID (SnowflakeId)';
COMMENT ON COLUMN klines.symbol IS '交易对';
COMMENT ON COLUMN klines.interval IS 'K线周期 (1m/5m/15m/30m/1h/4h/1d/1w/1M)';
COMMENT ON COLUMN klines.open_time IS 'K线开始时间 (时间戳秒)';
COMMENT ON COLUMN klines.close_time IS 'K线结束时间 (时间戳秒)';
COMMENT ON COLUMN klines.completed IS '是否已完成 (false=进行中, true=已完成)';
