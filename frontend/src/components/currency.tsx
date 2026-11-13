"use client";

import { createContext, useContext, type ReactNode } from "react";
import { formatMoney, type Currency } from "@/lib/currency";

const CurrencyContext = createContext<Currency | null>(null);

/** Mata uang usaha untuk komponen client di bawahnya; dipasang oleh kerangka halaman (AppShell, OnboardingShell). */
export function CurrencyProvider({ currency, children }: { currency: Currency; children: ReactNode }) {
  return <CurrencyContext value={currency}>{children}</CurrencyContext>;
}

export function useCurrency(): Currency {
  const currency = useContext(CurrencyContext);
  if (!currency) throw new Error("useCurrency must be used inside CurrencyProvider");
  return currency;
}

/** Pemformat uang dengan mata uang usaha: money(15000) → "Rp15.000". */
export function useMoney() {
  const currency = useCurrency();
  return (amount: number) => formatMoney(amount, currency);
}

