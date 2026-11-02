package com.thefarhany.stockwise.repository.catalog;

import com.thefarhany.stockwise.model.catalog.Product;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {

	List<Product> findByOutletIdOrderBySortOrderAscNameAsc(UUID outletId);

	List<Product> findByOutletIdAndActiveTrueOrderBySortOrderAscNameAsc(UUID outletId);

	Optional<Product> findByIdAndOutletId(UUID id, UUID outletId);

	List<Product> findByIdInAndOutletIdAndActiveTrue(Collection<UUID> ids, UUID outletId);

	boolean existsByOutletIdAndNameIgnoreCase(UUID outletId, String name);

	boolean existsByOutletIdAndNameIgnoreCaseAndIdNot(UUID outletId, String name, UUID id);

}
