package com.thefarhany.stockwise.dto.auth;

import com.thefarhany.stockwise.model.auth.Language;
import jakarta.validation.constraints.NotNull;

/** PUT /me/language: bahasa tampilan akun yang sedang login. */
public record LanguageRequest(@NotNull Language language) {
}
