-- Baseline Stockwise R1 (lihat docs/04-database-design.md).
-- PK UUID disimpan BINARY(16). FK ditulis sebagai FOREIGN KEY terpisah (REFERENCES inline diabaikan MySQL).
-- Default aksi hapus RESTRICT; CASCADE hanya refresh_token, recipe_item, sale_item.

CREATE TABLE outlet (
    id                        BINARY(16)    NOT NULL DEFAULT (UUID_TO_BIN(UUID())),
    name                      VARCHAR(100)  NOT NULL,
    timezone                  VARCHAR(40)   NOT NULL DEFAULT 'Asia/Jakarta',
    open_time                 TIME          NULL,
    close_time                TIME          NULL,
    latitude                  DECIMAL(9,6)  NULL,
    longitude                 DECIMAL(9,6)  NULL,
    payday_days               JSON          NOT NULL DEFAULT (JSON_ARRAY(25, 1)),
    recommendation_buffer_pct DECIMAL(5,2)  NOT NULL DEFAULT 5.00,
    created_at                DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT ck_outlet_hours CHECK (close_time > open_time)
);

CREATE TABLE app_user (
    id            BINARY(16)   NOT NULL DEFAULT (UUID_TO_BIN(UUID())),
    outlet_id     BINARY(16)   NOT NULL,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_user_email (email),
    CONSTRAINT fk_app_user_outlet FOREIGN KEY (outlet_id) REFERENCES outlet (id),
    CONSTRAINT ck_app_user_role CHECK (role IN ('OWNER', 'CASHIER'))
);

CREATE TABLE refresh_token (
    id         BINARY(16)   NOT NULL DEFAULT (UUID_TO_BIN(UUID())),
    user_id    BINARY(16)   NOT NULL,
    token_hash VARCHAR(100) NOT NULL,
    expires_at DATETIME(6)  NOT NULL,
    revoked_at DATETIME(6)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_token_hash (token_hash),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
);

CREATE TABLE stock_item (
    id                 BINARY(16)   NOT NULL DEFAULT (UUID_TO_BIN(UUID())),
    outlet_id          BINARY(16)   NOT NULL,
    name               VARCHAR(100) NOT NULL,
    kind               VARCHAR(10)  NOT NULL,
    unit               VARCHAR(20)  NOT NULL,
    unit_cost          BIGINT       NOT NULL DEFAULT 0,
    shelf_life_days    SMALLINT     NOT NULL DEFAULT 1,
    carry_over_allowed BOOLEAN      NOT NULL DEFAULT TRUE,
    is_primary         BOOLEAN      NOT NULL DEFAULT FALSE,
    active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_item_name (outlet_id, name),
    CONSTRAINT fk_stock_item_outlet FOREIGN KEY (outlet_id) REFERENCES outlet (id),
    CONSTRAINT ck_stock_item_kind CHECK (kind IN ('RAW', 'FINISHED')),
    CONSTRAINT ck_stock_item_unit CHECK (unit IN ('PCS', 'GRAM', 'KG', 'ML', 'LITER', 'CUP'))
);

CREATE TABLE product (
    id         BINARY(16)   NOT NULL DEFAULT (UUID_TO_BIN(UUID())),
    outlet_id  BINARY(16)   NOT NULL,
    name       VARCHAR(100) NOT NULL,
    category   VARCHAR(50)  NULL,
    price      BIGINT       NOT NULL,
    stock_mode VARCHAR(20)  NOT NULL,
    sort_order SMALLINT     NOT NULL DEFAULT 0,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_name (outlet_id, name),
    CONSTRAINT fk_product_outlet FOREIGN KEY (outlet_id) REFERENCES outlet (id),
    CONSTRAINT ck_product_price CHECK (price >= 0),
    CONSTRAINT ck_product_mode CHECK (stock_mode IN ('MADE_TO_ORDER', 'BATCH'))
);

CREATE TABLE recipe_item (
    product_id    BINARY(16)    NOT NULL,
    stock_item_id BINARY(16)    NOT NULL,
    quantity      DECIMAL(12,3) NOT NULL,
    PRIMARY KEY (product_id, stock_item_id),
    CONSTRAINT fk_recipe_item_product FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE CASCADE,
    CONSTRAINT fk_recipe_item_stock_item FOREIGN KEY (stock_item_id) REFERENCES stock_item (id),
    CONSTRAINT ck_recipe_item_qty CHECK (quantity > 0)
);

CREATE TABLE daily_session (
    id                  BINARY(16)  NOT NULL DEFAULT (UUID_TO_BIN(UUID())),
    outlet_id           BINARY(16)  NOT NULL,
    business_date       DATE        NOT NULL,
    status              VARCHAR(10) NOT NULL,
    -- MySQL tidak punya partial unique index: kolom ini membuat hanya satu sesi OPEN per usaha
    open_outlet_id      BINARY(16)  GENERATED ALWAYS AS (CASE WHEN status = 'OPEN' THEN outlet_id ELSE NULL END) STORED,
    opened_at           DATETIME(6) NOT NULL,
    opened_by           BINARY(16)  NOT NULL,
    closed_at           DATETIME(6) NULL,
    closed_by           BINARY(16)  NULL,
    close_type          VARCHAR(10) NULL,
    planned_close_time  TIME        NOT NULL,
    weather_code        SMALLINT    NULL,
    precipitation_mm    DECIMAL(6,2) NULL,
    temperature_max_c   DECIMAL(4,1) NULL,
    notes               TEXT        NULL,
    created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_daily_session_date (outlet_id, business_date),
    UNIQUE KEY uk_daily_session_one_open (open_outlet_id),
    CONSTRAINT fk_daily_session_outlet FOREIGN KEY (outlet_id) REFERENCES outlet (id),
    CONSTRAINT fk_daily_session_opened_by FOREIGN KEY (opened_by) REFERENCES app_user (id),
    CONSTRAINT fk_daily_session_closed_by FOREIGN KEY (closed_by) REFERENCES app_user (id),
    CONSTRAINT ck_daily_session_status CHECK (status IN ('OPEN', 'CLOSED')),
    CONSTRAINT ck_daily_session_close_type CHECK (close_type IS NULL OR close_type IN ('MANUAL', 'AUTO'))
);

CREATE TABLE session_stock (
    id               BINARY(16)    NOT NULL DEFAULT (UUID_TO_BIN(UUID())),
    session_id       BINARY(16)    NOT NULL,
    stock_item_id    BINARY(16)    NOT NULL,
    opening_qty      DECIMAL(12,3) NOT NULL DEFAULT 0,
    opening_age_days SMALLINT      NOT NULL DEFAULT 0,
    opening_value    BIGINT        NOT NULL DEFAULT 0,
    added_qty        DECIMAL(12,3) NOT NULL DEFAULT 0,
    added_cost       BIGINT        NOT NULL DEFAULT 0,
    sold_qty         DECIMAL(12,3) NOT NULL DEFAULT 0,
    adjusted_qty     DECIMAL(12,3) NOT NULL DEFAULT 0,
    closing_qty      DECIMAL(12,3) NULL,
    carried_over_qty DECIMAL(12,3) NULL,
    discarded_qty    DECIMAL(12,3) NULL,
    sold_cost        BIGINT        NULL,
    discarded_cost   BIGINT        NULL,
    adjustment_cost  BIGINT        NULL,
    carried_value    BIGINT        NULL,
    sold_out_at      DATETIME(6)   NULL,
    recommended_qty  DECIMAL(12,3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_session_stock (session_id, stock_item_id),
    CONSTRAINT fk_session_stock_session FOREIGN KEY (session_id) REFERENCES daily_session (id),
    CONSTRAINT fk_session_stock_item FOREIGN KEY (stock_item_id) REFERENCES stock_item (id)
);

CREATE TABLE stock_movement (
    id            BINARY(16)    NOT NULL DEFAULT (UUID_TO_BIN(UUID())),
    session_id    BINARY(16)    NOT NULL,
    stock_item_id BINARY(16)    NOT NULL,
    type          VARCHAR(20)   NOT NULL,
    quantity      DECIMAL(12,3) NOT NULL,
    unit_cost     DECIMAL(14,4) NULL,
    reference_id  BINARY(16)    NULL,
    reason        VARCHAR(200)  NULL,
    created_by    BINARY(16)    NULL,
    created_at    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY ix_stock_movement_session_item (session_id, stock_item_id),
    KEY ix_stock_movement_reference (reference_id),
    CONSTRAINT fk_stock_movement_session FOREIGN KEY (session_id) REFERENCES daily_session (id),
    CONSTRAINT fk_stock_movement_item FOREIGN KEY (stock_item_id) REFERENCES stock_item (id),
    CONSTRAINT fk_stock_movement_user FOREIGN KEY (created_by) REFERENCES app_user (id),
    CONSTRAINT ck_stock_movement_type CHECK (type IN
        ('OPENING', 'PURCHASE', 'PRODUCTION', 'SALE', 'VOID', 'ADJUSTMENT', 'CARRY_OVER', 'DISCARD'))
);

CREATE TABLE sale (
    id             BINARY(16)   NOT NULL DEFAULT (UUID_TO_BIN(UUID())),
    outlet_id      BINARY(16)   NOT NULL,
    session_id     BINARY(16)   NOT NULL,
    receipt_no     INT          NOT NULL,
    sold_at        DATETIME(6)  NOT NULL,
    total_amount   BIGINT       NOT NULL,
    payment_method VARCHAR(20)  NOT NULL,
    cash_received  BIGINT       NULL,
    input_method   VARCHAR(20)  NOT NULL,
    status         VARCHAR(10)  NOT NULL DEFAULT 'COMPLETED',
    cashier_id     BINARY(16)   NOT NULL,
    void_reason    VARCHAR(200) NULL,
    voided_at      DATETIME(6)  NULL,
    voided_by      BINARY(16)   NULL,
    created_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sale_receipt (session_id, receipt_no),
    KEY ix_sale_session_time (session_id, sold_at),
    KEY ix_sale_outlet_time (outlet_id, sold_at),
    CONSTRAINT fk_sale_outlet FOREIGN KEY (outlet_id) REFERENCES outlet (id),
    CONSTRAINT fk_sale_session FOREIGN KEY (session_id) REFERENCES daily_session (id),
    CONSTRAINT fk_sale_cashier FOREIGN KEY (cashier_id) REFERENCES app_user (id),
    CONSTRAINT fk_sale_voided_by FOREIGN KEY (voided_by) REFERENCES app_user (id),
    CONSTRAINT ck_sale_payment CHECK (payment_method IN ('CASH', 'QRIS', 'OTHER')),
    CONSTRAINT ck_sale_input CHECK (input_method IN ('TAP', 'RECAP', 'VOICE')),
    CONSTRAINT ck_sale_status CHECK (status IN ('COMPLETED', 'VOID')),
    CONSTRAINT ck_sale_void_reason CHECK (status = 'COMPLETED' OR void_reason IS NOT NULL)
);

CREATE TABLE sale_item (
    id         BINARY(16)    NOT NULL DEFAULT (UUID_TO_BIN(UUID())),
    sale_id    BINARY(16)    NOT NULL,
    product_id BINARY(16)    NOT NULL,
    quantity   DECIMAL(12,3) NOT NULL,
    unit_price BIGINT        NOT NULL,
    subtotal   BIGINT        NOT NULL,
    PRIMARY KEY (id),
    KEY ix_sale_item_product (product_id),
    CONSTRAINT fk_sale_item_sale FOREIGN KEY (sale_id) REFERENCES sale (id) ON DELETE CASCADE,
    CONSTRAINT fk_sale_item_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT ck_sale_item_qty CHECK (quantity > 0)
);

CREATE TABLE forecast (
    id            BINARY(16)    NOT NULL DEFAULT (UUID_TO_BIN(UUID())),
    outlet_id     BINARY(16)    NOT NULL,
    target_date   DATE          NOT NULL,
    product_id    BINARY(16)    NOT NULL,
    predicted_qty DECIMAL(12,3) NOT NULL,
    lower_qty     DECIMAL(12,3) NOT NULL,
    upper_qty     DECIMAL(12,3) NOT NULL,
    method        VARCHAR(20)   NOT NULL,
    model_version VARCHAR(40)   NOT NULL,
    features      JSON          NULL,
    created_at    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_forecast (outlet_id, target_date, product_id, model_version),
    CONSTRAINT fk_forecast_outlet FOREIGN KEY (outlet_id) REFERENCES outlet (id),
    CONSTRAINT fk_forecast_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT ck_forecast_method CHECK (method IN ('COLD_START', 'BASELINE', 'ML'))
);

CREATE TABLE recommendation (
    id              BINARY(16)    NOT NULL DEFAULT (UUID_TO_BIN(UUID())),
    outlet_id       BINARY(16)    NOT NULL,
    target_date     DATE          NOT NULL,
    stock_item_id   BINARY(16)    NOT NULL,
    required_qty    DECIMAL(12,3) NOT NULL,
    carry_over_qty  DECIMAL(12,3) NOT NULL DEFAULT 0,
    recommended_qty DECIMAL(12,3) NOT NULL,
    lower_qty       DECIMAL(12,3) NULL,
    upper_qty       DECIMAL(12,3) NULL,
    estimated_cost  BIGINT        NOT NULL,
    reasons         JSON          NULL,
    status          VARCHAR(10)   NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY ix_recommendation_target (outlet_id, target_date, status),
    CONSTRAINT fk_recommendation_outlet FOREIGN KEY (outlet_id) REFERENCES outlet (id),
    CONSTRAINT fk_recommendation_item FOREIGN KEY (stock_item_id) REFERENCES stock_item (id),
    CONSTRAINT ck_recommendation_status CHECK (status IN ('ACTIVE', 'SUPERSEDED'))
);

CREATE TABLE daily_summary (
    id                   BINARY(16)    NOT NULL DEFAULT (UUID_TO_BIN(UUID())),
    session_id           BINARY(16)    NOT NULL,
    revenue              BIGINT        NOT NULL,
    cogs                 BIGINT        NOT NULL,
    waste_cost           BIGINT        NOT NULL,
    adjustment_cost      BIGINT        NOT NULL,
    carried_value        BIGINT        NOT NULL,
    expense_total        BIGINT        NOT NULL DEFAULT 0,
    net_profit           BIGINT        NOT NULL,
    total_items_sold     DECIMAL(12,3) NOT NULL,
    void_count           INT           NOT NULL DEFAULT 0,
    lost_sales_estimate  JSON          NULL,
    insight_status       VARCHAR(10)   NOT NULL DEFAULT 'NONE',
    insight_text         TEXT          NULL,
    insight_model        VARCHAR(60)   NULL,
    insight_attempts     SMALLINT      NOT NULL DEFAULT 0,
    insight_generated_at DATETIME(6)   NULL,
    created_at           DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_daily_summary_session (session_id),
    CONSTRAINT fk_daily_summary_session FOREIGN KEY (session_id) REFERENCES daily_session (id),
    CONSTRAINT ck_daily_summary_insight CHECK (insight_status IN ('NONE', 'READY'))
);
