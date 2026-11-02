# Stockwise

Aplikasi kasir, stok, dan keuangan untuk usaha makanan dan minuman skala kecil yang dagangannya cepat basi. Stockwise tidak berhenti di mencatat: penjualan hari ini diolah menjadi jawaban untuk satu pertanyaan harian, **besok harus siapkan berapa?**

## Latar Belakang

Pedagang kecil biasanya belanja berdasarkan perasaan, dan tebakan yang meleset merugikan ke dua arah:

- **Kurang stok.** Dagangan habis sebelum tutup, pembeli pulang dengan tangan kosong. Kerugian ini tidak pernah tercatat, jadi tidak disadari.
- **Kelebihan stok.** Sisa disimpan sampai turun kualitasnya, dijual diskon, atau dibuang. Modal hilang.
- **Tidak ada pola.** Tanpa catatan, pedagang tidak tahu bahwa Sabtu lebih ramai, tanggal tua lebih sepi, atau hujan mengubah jumlah pembeli.
- **Untung bersih tidak diketahui.** Uang usaha dan uang pribadi tercampur.
- **Mencatat itu merepotkan.** Saat ramai tidak ada waktu mengetik, dan aplikasi yang ribet ditinggalkan dalam seminggu.

Persona utamanya Pak Budi, pemilik lapak ayam geprek: belanja ayam di pasar tiap pagi, jualan sambil melayani dan menggoreng, dan tahu untung hanya dari uang di laci. *"Kemarin habis jam 2, hari ini malah sisa banyak."*

## Visi

Pedagang makanan dan minuman kecil berhenti menebak. Setiap malam mereka tahu untung hari itu dan berapa yang perlu disiapkan besok, sehingga dagangan tidak habis sebelum waktunya dan tidak terbuang.

## Cara Kerja

Satu hari dagang dicatat sebagai satu **sesi harian**:

1. **Buka lapak.** Sisa kemarin dibawa otomatis, lalu pedagang mencatat belanja atau produksi hari ini beserta total bayarnya.
2. **Jualan.** Kasir bermode cepat: satu tap pada tombol produk menambah satu item. Setiap penjualan mengurangi stok sesuai resep, dan jam stok habis tercatat otomatis.
3. **Tutup lapak.** Pedagang mengonfirmasi sisa, lalu memilih simpan ke besok atau buang. Sesi yang lupa ditutup ditutup otomatis 30 menit setelah jam tutup.
4. **Ringkasan malam.** Omzet, laba, sisa, jam habis, dan saran belanja besok langsung tersedia.

Jam habis dan sisa stok adalah data yang membuat saran besok masuk akal. Hari ketika stok habis lebih awal tidak dianggap sebagai hari sepi, karena permintaan yang hilang ikut diperkirakan.

## Fitur

**Kasir**
- Mode cepat (grid tombol produk) dan mode rekap (isi total terjual per produk di akhir hari)
- Metode bayar tunai, QRIS statis, atau lainnya (pencatatan saja, tanpa integrasi pembayaran)
- Penjualan ditolak bila stok tidak cukup; void dengan alasan
- Riwayat transaksi dan nomor struk

**Bahan, produk, dan resep**
- Pemilik mengisi bahan, produk, dan resep sendiri, jadi dagangan apa pun bisa dipakai, bukan hanya ayam geprek
- Belanja boleh dicatat dalam satuan pasar (kg, liter) dan dikonversi otomatis ke gram atau ml
- Panduan bertahap untuk usaha baru, dengan contoh dan teks bantuan di setiap isian

**Keuangan**
- Omzet, biaya terjual (HPP), biaya terbuang, dan laba harian dihitung otomatis
- Harga bahan dihitung dari belanja (rata-rata tertimbang dengan sisa kemarin)
- HPP dan margin per porsi, serta saran harga jual dari target margin usaha
- Laporan rentang tanggal dengan tren harian atau mingguan, dan ekspor penjualan ke Excel

**Saran belanja dan insight**
- Prediksi permintaan per produk untuk besok, diterjemahkan lewat resep menjadi saran stok per bahan (jumlah, rentang, estimasi biaya)
- Perkiraan penjualan yang terlewat bila stok habis sebelum tutup
- Insight berbahasa sehari-hari atas permintaan pemilik. Angka selalu berasal dari data pedagang, bukan dikarang model; bila model tidak tersedia, insight memakai teks template

**Akun dan data**
- Siapa pun bisa daftar dan mendapat usahanya sendiri; data setiap usaha terpisah sepenuhnya
- Dua peran: **Owner** (menu, harga, staf, laporan) dan **Cashier** (penjualan dan sesi harian). Owner juga bisa melakukan semua pekerjaan Cashier
- Owner bisa menonaktifkan akun Cashier dan menghapus akun beserta usahanya secara permanen

## Prinsip

1. Mencatat harus lebih cepat daripada tidak mencatat.
2. Angka selalu berasal dari data pedagang; penjelasan boleh berbahasa manusia, angka tidak boleh dikarang.
3. Mulai dari yang sederhana. Saran awal memakai pola rata-rata yang bisa dijelaskan, dan baru menjadi lebih canggih setelah datanya cukup.
4. Data setiap usaha terpisah.

## Status

| Bagian | Status |
|---|---|
| Backend (akun, katalog, sesi, kasir, laporan, saran belanja, insight) | Rilis 1 selesai dan teruji |
| Frontend web | Sedang dikerjakan |
| Pengeluaran operasional, laporan akurasi | Rilis 2 |
| Input suara/teks bebas, scan nota, cuaca, asisten tanya-jawab, aplikasi Android | Rilis 3 |
| Reset kata sandi | Ditunda |

Yang belum dikerjakan: pemesanan online, integrasi pembayaran (QRIS/VA), manajemen karyawan dan gaji, banyak cabang dalam satu akun, mode offline, dan aplikasi iOS.

## Tujuan Proyek

Stockwise dibangun sebagai proyek portofolio, dan terbuka bagi pedagang sungguhan yang ingin mencoba. Yang dituju adalah produk yang benar-benar jalan dari awal sampai akhir: daftar, mencatat sesi harian, sampai saran belanja. Keberhasilannya diukur dari tebakan belanja yang makin akurat dari minggu ke minggu: hari kehabisan stok dan sisa terbuang berkurang, dan pencatatan harian makin konsisten.

## Teknologi

Backend Spring Boot (Java 21) dengan MySQL; frontend Next.js. Panduan menjalankannya ada di `backend/` dan `frontend/`.
