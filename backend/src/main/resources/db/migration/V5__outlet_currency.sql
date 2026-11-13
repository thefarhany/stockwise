-- Mata uang usaha (kode ISO 4217). Semua kolom uang menyimpan satuan terkecil mata uang ini (rupiah, sen).
ALTER TABLE outlet
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'IDR',
    ADD CONSTRAINT ck_outlet_currency CHECK (currency IN ('IDR', 'MYR', 'SGD', 'USD', 'EUR'));
