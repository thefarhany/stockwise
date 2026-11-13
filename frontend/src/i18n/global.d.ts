import type en from "../../messages/en.json";
import type { Locale } from "./locale";

// Kunci terjemahan diperiksa TypeScript: messages/id.json harus punya kunci yang sama (lihat messages.test.ts).
declare module "next-intl" {
  interface AppConfig {
    Locale: Locale;
    Messages: typeof en;
  }
}
