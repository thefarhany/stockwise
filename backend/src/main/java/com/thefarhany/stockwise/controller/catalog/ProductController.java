package com.thefarhany.stockwise.controller.catalog;

import com.thefarhany.stockwise.common.ApiResponse;
import com.thefarhany.stockwise.dto.catalog.ProductRequest;
import com.thefarhany.stockwise.dto.catalog.ProductResponse;
import com.thefarhany.stockwise.dto.catalog.RecipeRequest;
import com.thefarhany.stockwise.dto.catalog.RecipeResponse;
import com.thefarhany.stockwise.security.AuthPrincipal;
import com.thefarhany.stockwise.service.catalog.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
class ProductController {

	private final ProductService service;

	ProductController(ProductService service) {
		this.service = service;
	}

	@GetMapping
	ApiResponse<List<ProductResponse>> list(@AuthenticationPrincipal AuthPrincipal me,
			@RequestParam(defaultValue = "false") boolean includeInactive) {
		return ApiResponse.ok(service.list(me, includeInactive));
	}

	@PostMapping
	@PreAuthorize("hasRole('OWNER')")
	@ResponseStatus(HttpStatus.CREATED)
	ApiResponse<ProductResponse> create(@AuthenticationPrincipal AuthPrincipal me,
			@Valid @RequestBody ProductRequest request) {
		return ApiResponse.ok(service.create(me.outletId(), request));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('OWNER')")
	ApiResponse<ProductResponse> update(@AuthenticationPrincipal AuthPrincipal me, @PathVariable UUID id,
			@Valid @RequestBody ProductRequest request) {
		return ApiResponse.ok(service.update(me.outletId(), id, request));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('OWNER')")
	ApiResponse<Void> deactivate(@AuthenticationPrincipal AuthPrincipal me, @PathVariable UUID id) {
		service.deactivate(me.outletId(), id);
		return ApiResponse.ok(null);
	}

	@PutMapping("/{id}/recipe")
	@PreAuthorize("hasRole('OWNER')")
	ApiResponse<RecipeResponse> replaceRecipe(@AuthenticationPrincipal AuthPrincipal me, @PathVariable UUID id,
			@Valid @RequestBody RecipeRequest request) {
		return ApiResponse.ok(service.replaceRecipe(me.outletId(), id, request));
	}

}
