"use client";

import type { ChangeEvent } from "react";
import { useController, type Control, type FieldPath, type FieldValues } from "react-hook-form";
import { useCurrency } from "@/components/currency";
import { currencies, editMoney, formatAmount, parseMoney, type Currency } from "@/lib/currency";
import { Field, type FieldProps } from "./field";

type MoneyInputProps = Omit<FieldProps, "value" | "onChange" | "type" | "prefix" | "placeholder"> & {
  /** Satuan terkecil mata uang (rupiah, sen). */
  value: number;
  onValueChange: (amount: number) => void;
  /** Nilai yang ditampilkan sebagai isian kosong (mis. 0 untuk harga perkiraan yang belum diisi). */
  emptyValue?: number;
};

/**
 * Ketikan di isian uang diformat langsung (lib/currency editMoney); kursor dikembalikan ke digit yang sama setelah
 * React menulis teks baru. Mengembalikan teks yang sudah diformat.
 */
export function typeMoney(e: ChangeEvent<HTMLInputElement>, currency: Currency): string {
  const input = e.target;
  const { text, caret } = editMoney(input.value, input.selectionStart ?? input.value.length, currency);
  requestAnimationFrame(() => {
    if (document.activeElement === input) input.setSelectionRange(caret, caret);
  });
  return text;
}

/** inputMode dan placeholder ("0" atau "0.00") untuk isian uang. */
export const moneyInputProps = (currency: Currency) => ({
  inputMode: currencies[currency].decimals > 0 ? ("decimal" as const) : ("numeric" as const),
  placeholder: formatAmount(0, currency),
  autoComplete: "off",
});

/** Isian uang dalam mata uang usaha: tampil "15.000" atau "15.00", tersimpan dalam satuan terkecil (15000, 1500). */
export function MoneyInput({ value, onValueChange, emptyValue = Number.NaN, ...props }: MoneyInputProps) {
  const currency = useCurrency();
  const empty = !Number.isFinite(value) || value === emptyValue;
  return (
    <Field
      {...moneyInputProps(currency)}
      {...props}
      type="text"
      prefix={currencies[currency].symbol}
      value={empty ? "" : formatAmount(value, currency)}
      onChange={(e) => {
        const amount = parseMoney(typeMoney(e, currency), currency);
        onValueChange(Number.isNaN(amount) ? emptyValue : amount);
      }}
    />
  );
}

type MoneyFieldProps<T extends FieldValues> = Omit<MoneyInputProps, "value" | "onValueChange" | "onBlur" | "name"> & {
  control: Control<T>;
  name: FieldPath<T>;
};

/** MoneyInput yang tersambung ke react-hook-form. */
export function MoneyField<T extends FieldValues>({ control, name, ...props }: MoneyFieldProps<T>) {
  const { field: { ref, name: fieldName, value, onChange, onBlur } } = useController({ control, name });
  return <MoneyInput {...props} ref={ref} name={fieldName} value={value as number} onValueChange={onChange} onBlur={onBlur} />;
}
