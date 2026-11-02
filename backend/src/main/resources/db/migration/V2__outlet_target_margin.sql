-- FR-53: target margin per usaha untuk saran harga jual, dalam persen dari harga jual (40 = laba kotor 40% dari harga).
ALTER TABLE outlet
    ADD COLUMN target_margin_pct DECIMAL(5,2) NOT NULL DEFAULT 40.00,
    ADD CONSTRAINT ck_outlet_target_margin CHECK (target_margin_pct >= 0 AND target_margin_pct <= 90);
