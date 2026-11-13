import { fileURLToPath } from "node:url";
import { defineConfig } from "vitest/config";

// Samakan alias "@/" dengan tsconfig supaya test bisa mengimpor modul yang memakainya.
export default defineConfig({
  resolve: { alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) } },
});
