/** Porsi per jam dari jam buka sampai jam tutup, jam tanpa penjualan diisi 0. Jam di luar jam buka tetap ikut. */
export function hourlySeries(byHour: { hour: number; qty: number }[], openHour: number, closeHour: number) {
  const qty = new Map(byHour.map((h) => [h.hour, h.qty]));
  const hours = [...byHour.map((h) => h.hour), openHour, closeHour];
  const from = Math.min(...hours);
  const to = Math.max(...hours);
  return Array.from({ length: to - from + 1 }, (_, i) => ({ hour: from + i, qty: qty.get(from + i) ?? 0 }));
}

/** Dua jam teramai (porsi terbanyak dulu, seri diambil yang lebih awal), tanpa jam kosong. */
export function busiestHours(series: { hour: number; qty: number }[]) {
  return series
    .filter((h) => h.qty > 0)
    .toSorted((a, b) => b.qty - a.qty || a.hour - b.hour)
    .slice(0, 2);
}
