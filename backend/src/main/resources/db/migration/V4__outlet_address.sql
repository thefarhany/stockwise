-- Alamat usaha: teks lengkap plus bagian-bagiannya (diisi dari Mapbox atau manual). Koordinat tetap di latitude/longitude.
ALTER TABLE outlet
    ADD COLUMN address     VARCHAR(255) NULL,
    ADD COLUMN street      VARCHAR(150) NULL,
    ADD COLUMN village     VARCHAR(100) NULL,
    ADD COLUMN district    VARCHAR(100) NULL,
    ADD COLUMN city        VARCHAR(100) NULL,
    ADD COLUMN province    VARCHAR(100) NULL,
    ADD COLUMN postal_code VARCHAR(10)  NULL;
