"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useTranslations } from "next-intl";
import type { FormEvent } from "react";
import { useForm } from "react-hook-form";
import { FormError } from "@/features/auth/components/form-error";
import { createStockItem } from "@/features/catalog/api/stock-items";
import { StockItemFields, stockItemDefaults as defaults } from "@/features/catalog/components/stock-item-fields";
import { stockItemSchema, type StockItemInput } from "@/features/catalog/schemas";
import { ApiError } from "@/lib/api-client";
import { ONBOARDING_FORM_ID } from "../constants";
import { ADD_MORE, submitIntent, useStepSubmit } from "./use-step-submit";

export function IngredientForm({ hasItems }: { hasItems: boolean }) {
  const t = useTranslations("onboarding");
  const tc = useTranslations("common");
  const tv = useTranslations("validation");
  const form = useForm<StockItemInput>({ resolver: zodResolver(stockItemSchema(tv)), defaultValues: defaults });
  const { reset, getValues, handleSubmit, setError, setFocus, formState: { errors, isSubmitting } } = form;
  const step = useStepSubmit({ next: "/onboarding/products", hasItems, isEmpty: () => !getValues("name").trim() });

  const save = handleSubmit(async (input, event) => {
    try {
      await createStockItem(input);
    } catch (e) {
      if (e instanceof ApiError && e.errorCode === "DUPLICATE_NAME") {
        setError("name", { message: t("duplicateIngredient") }, { shouldFocus: true });
      } else {
        const message = step.failed(e);
        if (message) setError("root", { message });
      }
      return;
    }
    if (step.saved(submitIntent(event))) {
      reset(defaults);
      setFocus("name");
    }
  });

  const onSubmit = (e: FormEvent<HTMLFormElement>) => (step.skip(e) ? undefined : save(e));

  return (
    <form id={ONBOARDING_FORM_ID} onSubmit={onSubmit} noValidate className="space-y-6">
      <FormError message={errors.root?.message} />

      <StockItemFields form={form} />

      <div className="flex flex-wrap items-center justify-between gap-3">
        <Link href="/onboarding" className="btn border-base-300 bg-white">{tc("back")}</Link>
        <div className="flex flex-wrap gap-3">
          <button type="submit" value={ADD_MORE} disabled={isSubmitting} className="btn border-base-300 bg-white">
            {t("saveAddAnother")}
          </button>
          <button type="submit" disabled={isSubmitting} className="btn btn-primary">
            {isSubmitting ? tc("saving") : t("continueToProducts")}
          </button>
        </div>
      </div>
    </form>
  );
}
