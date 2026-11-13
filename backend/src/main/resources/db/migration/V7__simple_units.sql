-- Satuan disederhanakan jadi 10: gram, kg, ml, liter, pcs, tray, botol, pack, dus, renceng.
-- Satuan lama dipetakan ke satuan baru. Bila isinya berbeda (ons, sendok, cup, lusin, kodi), semua jumlah yang
-- tersimpan dalam satuan bahan dikalikan faktor dan harga per satuan dibagi faktor, supaya nilai uangnya tetap.
CREATE TEMPORARY TABLE unit_map (old_unit VARCHAR(20) PRIMARY KEY, new_unit VARCHAR(20) NOT NULL, f INT NOT NULL);
INSERT INTO unit_map VALUES
    ('ONS', 'GRAM', 100), ('SDT', 'ML', 5), ('SDM', 'ML', 15), ('CUP', 'ML', 240),
    ('LUSIN', 'PCS', 12), ('KODI', 'PCS', 20),
    ('BUTIR', 'PCS', 1), ('BUAH', 'PCS', 1), ('SIUNG', 'PCS', 1), ('IKAT', 'PCS', 1), ('BATANG', 'PCS', 1),
    ('LEMBAR', 'PCS', 1), ('EKOR', 'PCS', 1), ('POTONG', 'PCS', 1), ('SISIR', 'PCS', 1), ('PORSI', 'PCS', 1),
    ('KALENG', 'PCS', 1), ('GALON', 'PCS', 1), ('JERIGEN', 'PCS', 1), ('BUNGKUS', 'PACK', 1);

ALTER TABLE stock_item DROP CHECK ck_stock_item_unit;

UPDATE recipe_item r JOIN stock_item s ON s.id = r.stock_item_id JOIN unit_map m ON m.old_unit = s.unit
SET r.quantity = r.quantity * m.f
WHERE m.f <> 1;

UPDATE session_stock x JOIN stock_item s ON s.id = x.stock_item_id JOIN unit_map m ON m.old_unit = s.unit
SET x.opening_qty = x.opening_qty * m.f,
    x.added_qty = x.added_qty * m.f,
    x.sold_qty = x.sold_qty * m.f,
    x.adjusted_qty = x.adjusted_qty * m.f,
    x.closing_qty = x.closing_qty * m.f,
    x.carried_over_qty = x.carried_over_qty * m.f,
    x.discarded_qty = x.discarded_qty * m.f,
    x.recommended_qty = x.recommended_qty * m.f
WHERE m.f <> 1;

UPDATE stock_movement x JOIN stock_item s ON s.id = x.stock_item_id JOIN unit_map m ON m.old_unit = s.unit
SET x.quantity = x.quantity * m.f,
    x.unit_cost = x.unit_cost / m.f
WHERE m.f <> 1;

UPDATE recommendation x JOIN stock_item s ON s.id = x.stock_item_id JOIN unit_map m ON m.old_unit = s.unit
SET x.required_qty = x.required_qty * m.f,
    x.carry_over_qty = x.carry_over_qty * m.f,
    x.recommended_qty = x.recommended_qty * m.f,
    x.lower_qty = x.lower_qty * m.f,
    x.upper_qty = x.upper_qty * m.f
WHERE m.f <> 1;

-- Harga perkiraan per satuan bahan (BIGINT): dibulatkan setelah dibagi, mis. Rp500/sdm jadi Rp33/ml.
UPDATE stock_item s JOIN unit_map m ON m.old_unit = s.unit
SET s.unit_cost = ROUND(s.unit_cost / m.f),
    s.unit = m.new_unit;

DROP TEMPORARY TABLE unit_map;

ALTER TABLE stock_item ADD CONSTRAINT ck_stock_item_unit CHECK (unit IN (
    'GRAM', 'KG', 'ML', 'LITER', 'PCS', 'TRAY', 'BOTOL', 'PACK', 'DUS', 'RENCENG'
));
