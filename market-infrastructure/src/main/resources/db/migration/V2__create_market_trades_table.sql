-- ========================================
-- V2: 创建市场成交记录表 (按月分表)
-- ========================================

-- 创建成交记录主表
CREATE TABLE IF NOT EXISTS market_trades (
    id BIGINT PRIMARY KEY,                     -- SnowflakeId主键
    trade_id BIGINT NOT NULL,                  -- 交易ID (从matching-engine获取)
    symbol VARCHAR(20) NOT NULL,               -- 交易对 (BTC-USDT)
    price NUMERIC(20, 8) NOT NULL,             -- 成交价格
    quantity NUMERIC(30, 8) NOT NULL,          -- 成交数量
    amount NUMERIC(30, 8) NOT NULL,            -- 成交额
    taker_side VARCHAR(10) NOT NULL,           -- Taker方向 (BUY/SELL)
    buy_order_id BIGINT NOT NULL,              -- 买方订单ID
    sell_order_id BIGINT NOT NULL,             -- 卖方订单ID
    trade_time BIGINT NOT NULL,                -- 成交时间 (时间戳秒)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP   -- 创建时间
);

-- 创建索引
CREATE INDEX idx_market_trades_symbol_tradetime
    ON market_trades (symbol, trade_time DESC);

CREATE INDEX idx_market_trades_tradetime
    ON market_trades (trade_time DESC);

CREATE INDEX idx_market_trades_trade_id
    ON market_trades (trade_id);

-- 创建唯一约束 (防止重复数据)
CREATE UNIQUE INDEX uk_market_trades_trade_id
    ON market_trades (trade_id);

-- ========================================
-- 按月分表配置
-- ========================================

-- 说明: 按月分表需要使用PostgreSQL分区表功能
-- 以下是分区表配置示例 (实际生产环境中需要根据需求调整)

-- 1. 将market_trades转换为分区表
--    ALTER TABLE market_trades RENAME TO market_trades_old;
--
--    CREATE TABLE market_trades (
--        id BIGINT,
--        trade_id BIGINT NOT NULL,
--        symbol VARCHAR(20) NOT NULL,
--        price NUMERIC(20, 8) NOT NULL,
--        quantity NUMERIC(30, 8) NOT NULL,
--        amount NUMERIC(30, 8) NOT NULL,
--        taker_side VARCHAR(10) NOT NULL,
--        buy_order_id BIGINT NOT NULL,
--        sell_order_id BIGINT NOT NULL,
--        trade_time BIGINT NOT NULL,
--        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--        PRIMARY KEY (id, created_at)
--    ) PARTITION BY RANGE (created_at);

-- 2. 创建分区表 (按月)
--    CREATE TABLE market_trades_202501 PARTITION OF market_trades
--        FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
--
--    CREATE TABLE market_trades_202502 PARTITION OF market_trades
--        FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

-- 3. 设置数据保留策略 (保留1年数据)
--    可以使用定时任务定期删除旧分区

COMMENT ON TABLE market_trades IS '市场成交记录表 (按月分表)';
COMMENT ON COLUMN market_trades.id IS '主键ID (SnowflakeId)';
COMMENT ON COLUMN market_trades.trade_id IS '交易ID (从matching-engine获取)';
COMMENT ON COLUMN market_trades.symbol IS '交易对';
COMMENT ON COLUMN market_trades.taker_side IS 'Taker方向 (BUY/SELL)';
