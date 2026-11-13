-- Satuan bahan makanan lengkap: ons, takaran sendok, hitungan (butir, lusin, tray, ...) dan kemasan (bungkus, botol, ...).
ALTER TABLE stock_item DROP CHECK ck_stock_item_unit;
ALTER TABLE stock_item ADD CONSTRAINT ck_stock_item_unit CHECK (unit IN (
    'GRAM', 'ONS', 'KG',
    'ML', 'SDT', 'SDM', 'CUP', 'LITER',
    'PCS', 'BUTIR', 'BUAH', 'LUSIN', 'KODI', 'TRAY',
    'SIUNG', 'IKAT', 'BATANG', 'LEMBAR', 'EKOR', 'POTONG', 'SISIR', 'PORSI',
    'BUNGKUS', 'PACK', 'BOTOL', 'KALENG', 'GALON', 'JERIGEN'
));
