export function FormError({ message }: { message?: string }) {
  // Wadah tetap ada supaya pembaca layar mengumumkan pesan; empty:mb-0 supaya tidak menambah jarak space-y saat kosong.
  return (
    <div role="alert" aria-live="assertive" className="empty:mb-0">
      {message && <p className="rounded-field bg-error/10 px-4 py-3 text-sm text-error">{message}</p>}
    </div>
  );
}
